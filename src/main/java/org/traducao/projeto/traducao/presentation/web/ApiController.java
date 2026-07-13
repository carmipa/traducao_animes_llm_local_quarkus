package org.traducao.projeto.traducao.presentation.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.traducao.projeto.analisadorMidia.application.AnalisarMidiaUseCase;
import org.traducao.projeto.analisadorMidia.domain.ResultadoAnaliseLote;
import org.traducao.projeto.legendasExtracao.application.ExtrairLegendaUseCase;
import org.traducao.projeto.legendasExtracao.domain.FormatoLegenda;
import org.traducao.projeto.legendasExtracao.domain.RelatorioExtracao;
import org.traducao.projeto.legendasExtracao.domain.exceptions.FormatoLegendaInvalidoException;
import org.traducao.projeto.mapaProjeto.application.GeradorMapaProjetoUseCase;
import org.traducao.projeto.raspagemCorrecao.application.CorrigirComGoogleUseCase;
import org.traducao.projeto.raspagemRevisao.application.ResultadoRevisaoLegendas;
import org.traducao.projeto.raspagemRevisao.application.RevisarCacheUseCase;
import org.traducao.projeto.raspagemRevisao.application.RevisarLegendasUseCase;
import org.traducao.projeto.remuxer.application.RemuxarLoteUseCase;
import org.traducao.projeto.remuxer.domain.RelatorioRemux;
import org.traducao.projeto.telemetria.TelemetriaResumo;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.traducao.application.ProcessarArquivoUseCase;
import org.traducao.projeto.traducao.domain.StatusLlm;
import org.traducao.projeto.traducao.domain.exceptions.TradutorException;
import org.traducao.projeto.traducao.domain.ports.MistralPort;
import org.traducao.projeto.traducao.infrastructure.config.LlmProperties;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;
import org.traducao.projeto.core.util.DuracaoUtil;
import org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto;
import org.traducao.projeto.traducao.presentation.ui.AnsiCores;
import org.traducao.projeto.traducao.presentation.ui.PastasExecucao;
import org.traducao.projeto.traducaoCorrige.application.LimparCacheUseCase;

import org.traducao.projeto.core.execucao.FilaExecucaoPipeline;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * REST Controller que expõe a API REST para a interface web.
 * Permite acionar todos os módulos do pipeline em segundo plano.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger log = LoggerFactory.getLogger(ApiController.class);
    private static final Set<String> EXTENSOES_SUPORTADAS = Set.of(".ass", ".ssa");

    // Fila única compartilhada por todos os módulos (ver FilaExecucaoPipeline):
    // garante execução sequencial em segundo plano e impede que outro endpoint
    // troque o contexto/modelo LLM no meio de um job em andamento.
    private final FilaExecucaoPipeline filaExecucao;

    private final AnalisarMidiaUseCase analisarMidiaUseCase;
    private final ExtrairLegendaUseCase extrairLegendaUseCase;
    private final ProcessarArquivoUseCase processarArquivoUseCase;
    private final LimparCacheUseCase limparCacheUseCase;
    private final CorrigirComGoogleUseCase corrigirComGoogleUseCase;
    private final RevisarCacheUseCase revisarCacheUseCase;
    private final RevisarLegendasUseCase revisarLegendasUseCase;
    private final RemuxarLoteUseCase remuxarLoteUseCase;
    private final GeradorMapaProjetoUseCase geradorMapaProjetoUseCase;
    private final TelemetriaService telemetriaService;
    private final org.traducao.projeto.telemetria.TelemetriaDatasetService telemetriaDatasetService;
    private final LogStreamService logStreamService;
    private final TradutorProperties propriedades;
    private final PastasExecucao pastasExecucao;
    private final MistralPort mistralPort;
    private final GerenciadorContexto gerenciadorContexto;
    private final LlmProperties llmProperties;

    public ApiController(
            FilaExecucaoPipeline filaExecucao,
            AnalisarMidiaUseCase analisarMidiaUseCase,
            ExtrairLegendaUseCase extrairLegendaUseCase,
            ProcessarArquivoUseCase processarArquivoUseCase,
            LimparCacheUseCase limparCacheUseCase,
            CorrigirComGoogleUseCase corrigirComGoogleUseCase,
            RevisarCacheUseCase revisarCacheUseCase,
            RevisarLegendasUseCase revisarLegendasUseCase,
            RemuxarLoteUseCase remuxarLoteUseCase,
            GeradorMapaProjetoUseCase geradorMapaProjetoUseCase,
            TelemetriaService telemetriaService,
            org.traducao.projeto.telemetria.TelemetriaDatasetService telemetriaDatasetService,
            LogStreamService logStreamService,
            TradutorProperties propriedades,
            PastasExecucao pastasExecucao,
            MistralPort mistralPort,
            GerenciadorContexto gerenciadorContexto,
            LlmProperties llmProperties) {
        this.filaExecucao = filaExecucao;
        this.analisarMidiaUseCase = analisarMidiaUseCase;
        this.extrairLegendaUseCase = extrairLegendaUseCase;
        this.processarArquivoUseCase = processarArquivoUseCase;
        this.limparCacheUseCase = limparCacheUseCase;
        this.corrigirComGoogleUseCase = corrigirComGoogleUseCase;
        this.revisarCacheUseCase = revisarCacheUseCase;
        this.revisarLegendasUseCase = revisarLegendasUseCase;
        this.remuxarLoteUseCase = remuxarLoteUseCase;
        this.geradorMapaProjetoUseCase = geradorMapaProjetoUseCase;
        this.telemetriaService = telemetriaService;
        this.telemetriaDatasetService = telemetriaDatasetService;
        this.logStreamService = logStreamService;
        this.propriedades = propriedades;
        this.pastasExecucao = pastasExecucao;
        this.mistralPort = mistralPort;
        this.gerenciadorContexto = gerenciadorContexto;
        this.llmProperties = llmProperties;
    }

    // DTOs
    public record OperacaoRequest(String entrada, String saida, String contextoId, Long syncOffsetMs) {}
    public record ExtracaoRequest(String entrada, String saida, String formato) {}
    public record RespostaPadrao(String mensagem) {}
    public record MapaResponse(String conteudo, String arvoreGithub, String nomeProjeto) {}
    public record LlmStatusResponse(boolean online, boolean modeloCarregado, String modelo, String mensagem) {}
    public record ContextoResponse(String id, String nome, boolean padrao) {}

    /**
     * Endpoint para consulta de status geral (heartbeat)
     */
    @GetMapping("/status")
    public ResponseEntity<RespostaPadrao> status() {
        return ResponseEntity.ok(new RespostaPadrao("online"));
    }

    /**
     * Para o trabalho em execução na fila do pipeline e descarta os
     * enfileirados. A parada é cooperativa: o job interrompido encerra no
     * próximo ponto seguro (entre falas/arquivos), preservando o progresso já
     * salvo — cache de tradução e arquivos concluídos não se perdem.
     */
    @PostMapping("/pipeline/parar")
    public ResponseEntity<RespostaPadrao> pararPipeline() {
        if (!filaExecucao.ocupada()) {
            return ResponseEntity.ok(new RespostaPadrao("Nenhum trabalho em execução no pipeline."));
        }
        int canceladas = filaExecucao.parar();
        log.info("Pipeline interrompido pelo usuário ({} tarefa(s) cancelada(s)).", canceladas);
        System.out.println(AnsiCores.YELLOW
            + "[STOP] Interrupção solicitada pelo usuário — o trabalho atual encerra no próximo ponto seguro."
            + AnsiCores.RESET);
        return ResponseEntity.ok(new RespostaPadrao(
            "Parada solicitada (" + canceladas + " tarefa(s) cancelada(s)). "
                + "O trabalho atual encerra no próximo ponto seguro, preservando o progresso já salvo."));
    }

    /**
     * Estado da fila do pipeline — usado pela UI no modal do "Sair" para
     * avisar quando ainda há trabalho em execução.
     */
    @GetMapping("/pipeline/status")
    public ResponseEntity<RespostaPadrao> statusPipeline() {
        return ResponseEntity.ok(new RespostaPadrao(filaExecucao.ocupada() ? "ocupada" : "livre"));
    }

    /**
     * Status ao vivo do servidor LLM local (LM Studio) para o card do painel
     * inicial: informa se está online, se há modelo carregado em memória e qual
     * é ({@link MistralPort#verificarDisponibilidade()} já adota o modelo ativo
     * em {@link LlmProperties#model()} quando o detecta).
     */
    @GetMapping("/llm/status")
    public ResponseEntity<LlmStatusResponse> statusLlm() {
        try {
            StatusLlm status = mistralPort.verificarDisponibilidade();
            String modelo = status.modeloCarregado() ? llmProperties.model() : null;
            return ResponseEntity.ok(new LlmStatusResponse(
                status.servidorOnline(), status.modeloCarregado(), modelo, status.mensagem()));
        } catch (Exception e) {
            log.warn("Falha ao consultar o status do LLM: {}", e.getMessage());
            return ResponseEntity.ok(new LlmStatusResponse(false, false, null,
                "Falha ao consultar o servidor LLM: " + e.getMessage()));
        }
    }

    /**
     * Lista os contextos de tradução disponíveis (animes).
     */
    @GetMapping("/contextos")
    public ResponseEntity<List<ContextoResponse>> listarContextos() {
        String idPadrao = gerenciadorContexto.getIdContextoPadrao();
        List<ContextoResponse> lista = gerenciadorContexto.getProvedores().stream()
                .map(p -> new ContextoResponse(p.getId(), p.getNomeExibicao(), p.getId().equals(idPadrao)))
                .toList();
        return ResponseEntity.ok(lista);
    }

    /**
     * Retorna estatísticas acumuladas do TelemetriaService.
     * O TelemetriaService em si não tem getters (não é um DTO), por isso
     * o resumo é montado explicitamente em {@link TelemetriaResumo}.
     */
    @GetMapping("/telemetria")
    public ResponseEntity<TelemetriaResumo> obterTelemetria() {
        Path diretorioCache = Path.of(propriedades.diretorioCache() != null && !propriedades.diretorioCache().isBlank()
                ? propriedades.diretorioCache() : "cache");
        return ResponseEntity.ok(telemetriaService.gerarResumo(diretorioCache));
    }

    /**
     * Exportação segura do arquivo de telemetria para download (Higienizado)
     */
    @GetMapping("/telemetria/exportar")
    public ResponseEntity<byte[]> exportarTelemetria() {
        try {
            Path arquivoTelemetria = TelemetriaService.resolverArquivoTelemetriaCanonico();
            if (!Files.exists(arquivoTelemetria)) {
                return ResponseEntity.notFound().build();
            }

            byte[] fileContent = Files.readAllBytes(arquivoTelemetria);

            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"kronos_telemetria_segura.json\"")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .contentLength(fileContent.length)
                    .body(fileContent);
        } catch (IOException e) {
            log.error("Erro ao exportar telemetria para download", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 1. ANÁLISE DE MÍDIA
     */
    @PostMapping("/analisar")
    public ResponseEntity<RespostaPadrao> analisar(@RequestBody OperacaoRequest req) {
        if (req.entrada() == null || req.entrada().isBlank()) {
            return ResponseEntity.badRequest().body(new RespostaPadrao("Caminho de entrada obrigatório."));
        }

        submeterJobComRelatorio("analise", "Análise de Mídia", () -> {
            try {
                Path pathEntrada = normalizarCaminho(req.entrada());
                if (pathEntrada == null) {
                    System.out.println("\u001B[31m[FAIL] Caminho de entrada inválido: " + req.entrada() + "\u001B[0m");
                    log.error("Caminho de entrada inválido informado para análise: {}", req.entrada());
                    return;
                }
                Path pathSaida = normalizarCaminho(req.saida());
                ResultadoAnaliseLote resultadoLote = analisarMidiaUseCase.executar(pathEntrada, pathSaida);
                publicarRelatorioSalvoNoConsole(resultadoLote.relatorioPrincipal());
                System.out.println("\n\u001B[32m========================================================================\u001B[0m");
                System.out.println("\u001B[32m  🎉 [SUCESSO] ANÁLISE DE MÍDIA FINALIZADA COM SUCESSO!\u001B[0m");
                System.out.println("\u001B[32m========================================================================\n\u001B[0m");
                log.info("[SUCESSO] Análise de mídia finalizada.");
            } catch (Exception e) {
                log.error("Erro na análise de mídia em background", e);
                System.out.println("\u001B[31m[ERRO] Falha na análise: " + e.getMessage() + "\u001B[0m");
            }
        });

        return ResponseEntity.ok(new RespostaPadrao("Análise de mídia iniciada no servidor."));
    }

    /**
     * Publica no canal SSE dedicado "analise-relatorio" o conteúdo exato do
     * relatório .txt que a auditoria acabou de gravar em disco, para que o
     * navegador exiba o relatório efetivamente salvo (fonte única de verdade),
     * em vez de reconstruí-lo a partir do log ao vivo da execução.
     */
    private void publicarRelatorioSalvoNoConsole(Path relatorioPrincipal) {
        if (relatorioPrincipal == null) {
            log.warn("Nenhum relatório de análise foi gravado em disco; nada para exibir no navegador.");
            return;
        }
        try {
            // Normaliza CRLF -> LF: o framing de eventos SSE multilinha trata \r
            // e \n como quebras de linha separadas, então um \r\n do arquivo
            // (line separator padrão no Windows) viraria uma linha em branco
            // extra a cada linha real depois de recomposto no navegador.
            String conteudo = Files.readString(relatorioPrincipal).replace("\r\n", "\n");
            logStreamService.publicarLog("analise-relatorio", conteudo);
        } catch (IOException e) {
            log.error("Erro ao ler o relatório salvo em {} para exibição no navegador: {}", relatorioPrincipal, e.getMessage());
        }
    }


    /**
     * Submete um job à fila já com o canal SSE correto e imprime, ao final
     * (sucesso OU falha), a linha padrão de relatório com o tempo total —
     * todos os consoles da UI encerram com o mesmo formato de resumo.
     */
    private void submeterJobComRelatorio(String canal, String nomeOperacao, Runnable corpo) {
        filaExecucao.submeter(() -> {
            logStreamService.definirCanalAtual(canal);
            long inicioMs = System.currentTimeMillis();
            try {
                corpo.run();
            } finally {
                System.out.println(DuracaoUtil.linhaRelatorioFinal(nomeOperacao, inicioMs));
            }
        });
    }

    /**
     * Publica a telemetria sanitizada como dataset público no repositório Git
     * dedicado ({@code kronos-anime-translation-telemetry-dataset}): snapshot em
     * {@code metrics/}, commit e push. Síncrono — o push leva poucos segundos
     * e o resultado volta na própria resposta para o painel exibir.
     */
    @PostMapping("/telemetria/publicar-dataset")
    public ResponseEntity<?> publicarDatasetTelemetria() {
        try {
            var resultado = telemetriaDatasetService.publicar();
            log.info("Publicação do dataset de telemetria: {}", resultado.mensagem());
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.error("Falha ao publicar o dataset de telemetria", e);
            return ResponseEntity.internalServerError()
                .body(new RespostaPadrao("Falha ao publicar o dataset: " + e.getMessage()));
        }
    }

    /**
     * 2. EXTRAÇÃO DE LEGENDAS
     */
    @PostMapping("/extrair")
    public ResponseEntity<RespostaPadrao> extrair(@RequestBody ExtracaoRequest req) {
        if (req.entrada() == null || req.entrada().isBlank()) {
            return ResponseEntity.badRequest().body(new RespostaPadrao("Caminho da pasta de vídeos obrigatório."));
        }

        final FormatoLegenda formatoSelecionado;
        try {
            formatoSelecionado = FormatoLegenda.fromString(req.formato());
        } catch (FormatoLegendaInvalidoException e) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(e.getMessage()));
        }

        submeterJobComRelatorio("extracao", "Extração de Legendas", () -> {
            try {
                Path pathEntrada = normalizarCaminho(req.entrada());
                if (pathEntrada == null) {
                    log.error("Caminho de entrada inválido informado para extração: {}", req.entrada());
                    return;
                }
                Path pathSaida = normalizarCaminho(req.saida());
                FormatoLegenda formato = formatoSelecionado;
                RelatorioExtracao rel = extrairLegendaUseCase.executar(pathEntrada, pathSaida, formato);
                if (rel.getArquivosDetectados() == 0) {
                    System.out.println("\n\u001B[33m========================================================================\u001B[0m");
                    System.out.println("\u001B[33m  ⚠️ [AVISO] NENHUM ARQUIVO DE VÍDEO SUPORTADO FOI ENCONTRADO!\u001B[0m");
                    System.out.println("\u001B[33m========================================================================\u001B[0m");
                    System.out.println("\u001B[36m  • Caminho informado : " + pathEntrada + "\u001B[0m");
                    System.out.println("\u001B[33m  • Formatos suportados: .mkv/.webm (MKVToolNix) e .mp4/.mov/.avi/.ts/.m2ts/.flv/.wmv (ffmpeg).\u001B[0m");
                    System.out.println("\u001B[33m========================================================================\n\u001B[0m");
                    log.warn("[AVISO] Nenhum arquivo de vídeo suportado foi encontrado no caminho: {}", pathEntrada);
                } else if (rel.getLegendasExtraidas() == 0) {
                    System.out.println("\n\u001B[33m========================================================================\u001B[0m");
                    System.out.println("\u001B[33m  ⚠️ [ALERTA] NENHUMA LEGENDA [" + formato.name() + "] FOI EXTRAÍDA!\u001B[0m");
                    System.out.println("\u001B[33m========================================================================\u001B[0m");
                    System.out.println("\u001B[36m  • Arquivos de Vídeo Analisados : " + rel.getArquivosDetectados() + "\u001B[0m");
                    System.out.println("\u001B[31m  • Faixas Extraídas com Sucesso : 0 [" + formato.name() + "]\u001B[0m");
                    System.out.println("\u001B[33m  • Vídeos sem Faixa " + formato.name() + "        : " + rel.getArquivosSemLegenda() + "\u001B[0m");
                    if (rel.getFalhasInesperadas() > 0) {
                        System.out.println("\u001B[31m  • Falhas de Processamento     : " + rel.getFalhasInesperadas() + "\u001B[0m");
                    }
                    System.out.println("\u001B[33m  💡 Dica: Verifique se o vídeo possui legendas em outro formato (ex: PGS ou SRT) ou se a legenda está queimada na imagem (Hardsub).\u001B[0m");
                    System.out.println("\u001B[33m========================================================================\n\u001B[0m");
                    log.warn("[ALERTA] Extração finalizada sem faixas geradas. 0 de {} vídeos possuíam faixa {}", rel.getArquivosDetectados(), formato.name());
                } else {
                    System.out.println("\n\u001B[32m========================================================================\u001B[0m");
                    System.out.println("\u001B[32m  🎉 [SUCESSO] EXTRAÇÃO DE LEGENDAS FINALIZADA COM SUCESSO!\u001B[0m");
                    System.out.println("\u001B[32m========================================================================\u001B[0m");
                    System.out.println("\u001B[36m  • Arquivos de Vídeo Analisados : " + rel.getArquivosDetectados() + "\u001B[0m");
                    System.out.println("\u001B[32m  • Faixas Extraídas com Sucesso : " + rel.getLegendasExtraidas() + " [" + formato.name() + "]\u001B[0m");
                    if (rel.getArquivosSemLegenda() > 0) {
                        System.out.println("\u001B[33m  • Vídeos sem Faixa " + formato.name() + "        : " + rel.getArquivosSemLegenda() + "\u001B[0m");
                    }
                    if (rel.getFalhasInesperadas() > 0) {
                        System.out.println("\u001B[31m  • Falhas de Processamento     : " + rel.getFalhasInesperadas() + "\u001B[0m");
                    }
                    System.out.println("\u001B[32m========================================================================\n\u001B[0m");
                    log.info("[SUCESSO] Extração de legendas finalizada. Extraídas: {} de {}", rel.getLegendasExtraidas(), rel.getArquivosDetectados());
                }
            } catch (Exception e) {
                log.error("Erro na extração de legendas em background", e);
                System.out.println("\u001B[31m[ERRO] Falha na extração: " + e.getMessage() + "\u001B[0m");
            }
        });

        return ResponseEntity.ok(new RespostaPadrao("Extração de legendas iniciada no servidor."));
    }

    /**
     * 3. TRADUÇÃO LOCAL
     */
    @PostMapping("/traduzir")
    public ResponseEntity<RespostaPadrao> traduzir(@RequestBody OperacaoRequest req) {
        if (req.entrada() == null || req.entrada().isBlank()) {
            return ResponseEntity.badRequest().body(new RespostaPadrao("Pasta de legendas de entrada obrigatória."));
        }
        if (req.contextoId() != null && !req.contextoId().isBlank() && !gerenciadorContexto.existeContexto(req.contextoId())) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(
                    "Contexto de tradução desconhecido: \"" + req.contextoId() + "\". Recarregue a página e selecione um contexto válido."));
        }

        submeterJobComRelatorio("traducao", "Tradução Local via LLM", () -> {
            try {
                Path pathEntrada = normalizarCaminho(req.entrada());
                if (pathEntrada == null) {
                    log.error("Caminho de entrada inválido informado para tradução: {}", req.entrada());
                    System.out.println("[FAIL] Caminho de entrada inválido: " + req.entrada());
                    return;
                }
                if (!Files.isDirectory(pathEntrada)) {
                    System.out.println("\u001B[31m[FAIL] Pasta de entrada inválida: " + pathEntrada + "\u001B[0m");
                    return;
                }

                // Verifica LLM
                System.out.println("Verificando se o servidor LLM local está online...");
                StatusLlm status = mistralPort.verificarDisponibilidade();
                if (!status.modeloCarregado()) {
                    System.out.println("\u001B[31m[FAIL] Servidor LLM indisponível: " + status.mensagem() + "\u001B[0m");
                    return;
                }
                System.out.println("\u001B[32m[OK] Servidor LLM ativo.\u001B[0m");

                // Configura as pastas compartilhadas
                String saida = req.saida() != null && !req.saida().isBlank() ? req.saida() : "";
                pastasExecucao.configurar(req.entrada(), saida, propriedades.diretorioCache(), propriedades);

                // Define o contexto de tradução selecionado na UI
                gerenciadorContexto.definirContextoAtivo(req.contextoId());
                System.out.println("\u001B[34m[CONTEXTO] Utilizando contexto: " + gerenciadorContexto.obterNomeContextoAtivo() + "\u001B[0m");

                List<Path> arquivos;
                try (Stream<Path> stream = Files.list(pathEntrada)) {
                    arquivos = stream
                            .filter(Files::isRegularFile)
                            .filter(this::temExtensaoSuportada)
                            .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                            .toList();
                }

                if (arquivos.isEmpty()) {
                    System.out.println("\u001B[33mNenhum arquivo de legenda .ass/.ssa encontrado.\u001B[0m");
                    return;
                }

                System.out.println("Iniciando tradução de " + arquivos.size() + " arquivo(s)...");

                for (int i = 0; i < arquivos.size(); i++) {
                    Path arquivo = arquivos.get(i);
                    System.out.println("\n--------------------------------------------------------------");
                    System.out.println("Processando arquivo [" + (i + 1) + "/" + arquivos.size() + "]: " + arquivo.getFileName());
                    System.out.println("--------------------------------------------------------------");
                    try {
                        processarArquivoUseCase.processar(arquivo);
                        System.out.println("\u001B[32m[OK] Traduzido com sucesso: " + arquivo.getFileName() + "\u001B[0m");
                    } catch (Exception ex) {
                        System.out.println("\u001B[31m[FAIL] Falha no processamento de " + arquivo.getFileName() + ": " + ex.getMessage() + "\u001B[0m");
                    }
                }

                System.out.println("\n\u001B[32m========================================================================\u001B[0m");
                System.out.println("\u001B[32m  🎉 [SUCESSO] TRADUÇÃO LOCAL VIA LLM FINALIZADA COM SUCESSO!\u001B[0m");
                System.out.println("\u001B[32m========================================================================\n\u001B[0m");
                log.info("[SUCESSO] Tradução via LLM processamento finalizado.");

            } catch (Exception e) {
                log.error("Erro na tradução em background", e);
                System.out.println("\u001B[31m[ERRO] Falha geral no tradutor: " + e.getMessage() + "\u001B[0m");
            }
        });

        return ResponseEntity.ok(new RespostaPadrao("Tradução via LLM iniciada no servidor."));
    }

    /**
     * 4. LIMPAR CACHE
     */
    @PostMapping("/corrigir-cache")
    public ResponseEntity<RespostaPadrao> limparCache(@RequestBody OperacaoRequest req) {
        String cacheDir = req.entrada() != null && !req.entrada().isBlank() ? req.entrada() : "cache";
        Path pathCache = normalizarCaminho(cacheDir);
        if (pathCache == null) {
            return ResponseEntity.badRequest().body(new RespostaPadrao("Caminho de cache inválido: " + cacheDir));
        }

        submeterJobComRelatorio("correcao", "Limpeza e Auditoria de Cache", () -> {
            try {
                limparCacheUseCase.executar(pathCache);
                System.out.println("\n\u001B[32m========================================================================\u001B[0m");
                System.out.println("\u001B[32m  🎉 [SUCESSO] LIMPEZA E AUDITORIA DE CACHE CONCLUÍDAS COM SUCESSO!\u001B[0m");
                System.out.println("\u001B[32m========================================================================\n\u001B[0m");
                log.info("[SUCESSO] Limpeza de cache concluída.");
            } catch (Exception e) {
                log.error("Erro ao limpar cache", e);
                System.out.println("\u001B[31m[ERRO] Limpeza do cache falhou: " + e.getMessage() + "\u001B[0m");
            }
        });

        return ResponseEntity.ok(new RespostaPadrao("Limpeza de cache iniciada no servidor."));
    }

    /**
     * 5. CORREÇÃO VIA SCRAPING GOOGLE
     */
    @PostMapping("/corrigir-scraping")
    public ResponseEntity<RespostaPadrao> corrigirScraping(@RequestBody OperacaoRequest req) {
        String cacheDir = req.entrada() != null && !req.entrada().isBlank() ? req.entrada() : "cache";
        Path pathCache = normalizarCaminho(cacheDir);
        if (pathCache == null) {
            return ResponseEntity.badRequest().body(new RespostaPadrao("Caminho de cache inválido: " + cacheDir));
        }

        submeterJobComRelatorio("correcao", "Correção via Google Translate", () -> {
            try {
                corrigirComGoogleUseCase.executar(pathCache);
                System.out.println("\n\u001B[32m========================================================================\u001B[0m");
                System.out.println("\u001B[32m  🎉 [SUCESSO] CORREÇÃO VIA GOOGLE TRANSLATE FINALIZADA COM SUCESSO!\u001B[0m");
                System.out.println("\u001B[32m========================================================================\n\u001B[0m");
                log.info("[SUCESSO] Correção via Google Translate finalizada.");
            } catch (Exception e) {
                log.error("Erro ao executar scraping", e);
                System.out.println("\u001B[31m[ERRO] Raspagem do Google falhou: " + e.getMessage() + "\u001B[0m");
            }
        });

        return ResponseEntity.ok(new RespostaPadrao("Auditoria e correção via Google Translate iniciada."));
    }

    /**
     * 5b. REVISÃO GRAMATICAL DO CACHE (concordância PT-BR via LLM)
     */
    @PostMapping("/revisar-cache")
    public ResponseEntity<RespostaPadrao> revisarCache(@RequestBody OperacaoRequest req) {
        String cacheDir = req.entrada() != null && !req.entrada().isBlank() ? req.entrada() : "cache";
        Path pathCache = normalizarCaminho(cacheDir);
        if (pathCache == null) {
            return ResponseEntity.badRequest().body(new RespostaPadrao("Caminho de cache inválido: " + cacheDir));
        }

        if (req.contextoId() != null && !req.contextoId().isBlank() && !gerenciadorContexto.existeContexto(req.contextoId())) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(
                "Contexto desconhecido: \"" + req.contextoId() + "\"."));
        }

        submeterJobComRelatorio("correcao", "Revisão Gramatical do Cache (LLM)", () -> {
            try {
                StatusLlm status = mistralPort.verificarDisponibilidade();
                if (!status.modeloCarregado()) {
                    System.out.println("\u001B[31m[FAIL] LLM indisponível para revisão: "
                        + status.mensagem() + "\u001B[0m");
                    return;
                }
                revisarCacheUseCase.executar(pathCache, req.contextoId());
                System.out.println("\n\u001B[32m========================================================================\u001B[0m");
                System.out.println("\u001B[32m  🎉 [SUCESSO] REVISÃO GRAMATICAL DO CACHE FINALIZADA COM SUCESSO!\u001B[0m");
                System.out.println("\u001B[32m========================================================================\n\u001B[0m");
                log.info("[SUCESSO] Revisão gramatical do cache finalizada.");
            } catch (Exception e) {
                log.error("Erro na revisão gramatical do cache", e);
                System.out.println("\u001B[31m[ERRO] Revisão gramatical falhou: " + e.getMessage() + "\u001B[0m");
            }
        });

        return ResponseEntity.ok(new RespostaPadrao("Revisão de concordância PT-BR iniciada no servidor."));
    }

    /**
     * 5. REVISÃO DE LEGENDAS TRADUZIDAS (.ass) via Google + auditoria
     */
    @PostMapping("/revisar-legendas")
    public ResponseEntity<RespostaPadrao> revisarLegendas(@RequestBody OperacaoRequest req) {
        if (req.entrada() == null || req.entrada().isBlank()) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(
                "Pasta com legendas traduzidas em português (.ass) é obrigatória."));
        }

        Optional<Path> pathPtOpt = parseCaminhoSeguro(req.entrada(), "legendas traduzidas");
        if (pathPtOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(
                "Caminho inválido para legendas traduzidas. Informe apenas a pasta (ex.: E:\\animes\\legendas_ptbr), "
                    + "sem colar logs ou textos da interface."));
        }
        Path pathPt = pathPtOpt.get();

        final Path pathEnFinal;
        if (req.saida() != null && !req.saida().isBlank()) {
            Optional<Path> pathEnOpt = parseCaminhoSeguro(req.saida(), "legendas originais em inglês");
            if (pathEnOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(new RespostaPadrao(
                    "Caminho inválido para legendas em inglês. Informe apenas a pasta, sem colar logs da interface."));
            }
            pathEnFinal = pathEnOpt.get();
        } else {
            pathEnFinal = null;
        }

        Optional<String> erroValidacao = revisarLegendasUseCase.validarPastaEntrada(pathPt);
        if (erroValidacao.isPresent()) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(erroValidacao.get()));
        }

        submeterJobComRelatorio("revisao", "Revisão de Legendas Traduzidas", () -> {
            try {
                ResultadoRevisaoLegendas resultado = revisarLegendasUseCase.executar(
                    pathPt, pathEnFinal, Path.of("cache"), null);
                if (resultado.arquivosAnalisados() == 0) {
                    System.out.println(
                        "\u001B[33m[AVISO] Revisão concluída sem arquivos .ass/.ssa para analisar.\u001B[0m");
                } else {
                    System.out.println("\n\u001B[32m========================================================================\u001B[0m");
                    System.out.println("\u001B[32m  🎉 [SUCESSO] REVISÃO DE LEGENDAS TRADUZIDAS FINALIZADA!\u001B[0m");
                    System.out.println("\u001B[32m========================================================================\u001B[0m");
                    System.out.println("\u001B[36m  • Arquivos Analisados : " + resultado.arquivosAnalisados() + "\u001B[0m");
                    System.out.println("\u001B[32m  • Falas Corrigidas    : " + resultado.falasCorrigidas() + "\u001B[0m");
                    System.out.println("\u001B[32m========================================================================\n\u001B[0m");
                    log.info("[SUCESSO] Revisão de legendas: {} arquivo(s), {} corrigidas.",
                        resultado.arquivosAnalisados(), resultado.falasCorrigidas());
                }
            } catch (Exception e) {
                log.error("Erro na revisão de legendas", e);
                System.out.println("\u001B[31m[ERRO] Revisão de legendas falhou: " + e.getMessage() + "\u001B[0m");
            }
        });

        return ResponseEntity.ok(new RespostaPadrao("Revisão de legendas traduzidas iniciada no servidor."));
    }

    /**
     * 5c. REVISÃO DE CONCORDÂNCIA PT-BR NAS LEGENDAS (.ass) via LLM local
     */
    @PostMapping("/revisar-legendas-concordancia")
    public ResponseEntity<RespostaPadrao> revisarLegendasConcordancia(@RequestBody OperacaoRequest req) {
        if (req.entrada() == null || req.entrada().isBlank()) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(
                "Pasta com legendas traduzidas em português (.ass) é obrigatória."));
        }

        if (req.contextoId() != null && !req.contextoId().isBlank()
            && !gerenciadorContexto.existeContexto(req.contextoId())) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(
                "Contexto desconhecido: \"" + req.contextoId() + "\"."));
        }

        Optional<Path> pathPtOpt = parseCaminhoSeguro(req.entrada(), "legendas traduzidas");
        if (pathPtOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(
                "Caminho inválido para legendas traduzidas. Informe apenas a pasta (ex.: E:\\animes\\legendas_ptbr), "
                    + "sem colar logs ou textos da interface."));
        }
        Path pathPt = pathPtOpt.get();

        final Path pathEnFinal;
        if (req.saida() != null && !req.saida().isBlank()) {
            Optional<Path> pathEnOpt = parseCaminhoSeguro(req.saida(), "legendas originais em inglês");
            if (pathEnOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(new RespostaPadrao(
                    "Caminho inválido para legendas em inglês. Informe apenas a pasta, sem colar logs da interface."));
            }
            pathEnFinal = pathEnOpt.get();
        } else {
            pathEnFinal = null;
        }

        Optional<String> erroValidacao = revisarLegendasUseCase.validarPastaEntrada(pathPt);
        if (erroValidacao.isPresent()) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(erroValidacao.get()));
        }

        submeterJobComRelatorio("revisao", "Revisão de Concordância PT-BR (LLM)", () -> {
            try {
                StatusLlm status = mistralPort.verificarDisponibilidade();
                if (!status.modeloCarregado()) {
                    System.out.println("\u001B[31m[FAIL] LLM indisponível para revisão de concordância: "
                        + status.mensagem() + "\u001B[0m");
                    return;
                }
                ResultadoRevisaoLegendas resultado = revisarLegendasUseCase.executar(
                    pathPt,
                    pathEnFinal,
                    Path.of("cache"),
                    null,
                    RevisarLegendasUseCase.ModoRevisaoLegendas.LLM_CONCORDANCIA,
                    req.contextoId()
                );
                if (resultado.arquivosAnalisados() == 0) {
                    System.out.println(
                        "\u001B[33m[AVISO] Revisão de concordância concluída sem arquivos .ass/.ssa.\u001B[0m");
                } else {
                    System.out.println("\n\u001B[32m========================================================================\u001B[0m");
                    System.out.println("\u001B[32m  🎉 [SUCESSO] REVISÃO DE CONCORDÂNCIA PT-BR (LLM) FINALIZADA!\u001B[0m");
                    System.out.println("\u001B[32m========================================================================\u001B[0m");
                    System.out.println("\u001B[36m  • Arquivos Analisados : " + resultado.arquivosAnalisados() + "\u001B[0m");
                    System.out.println("\u001B[32m  • Falas Corrigidas    : " + resultado.falasCorrigidas() + "\u001B[0m");
                    System.out.println("\u001B[32m========================================================================\n\u001B[0m");
                    log.info("[SUCESSO] Revisão concordância legendas: {} arquivo(s), {} corrigidas.",
                        resultado.arquivosAnalisados(), resultado.falasCorrigidas());
                }
            } catch (Exception e) {
                log.error("Erro na revisão de concordância das legendas", e);
                System.out.println("\u001B[31m[ERRO] Revisão de concordância falhou: "
                    + e.getMessage() + "\u001B[0m");
            }
        });

        return ResponseEntity.ok(new RespostaPadrao(
            "Revisão de concordância PT-BR (LLM) iniciada no servidor."));
    }

    /**
     * 6. REMUXER
     */
    @PostMapping("/remuxar")
    public ResponseEntity<RespostaPadrao> remuxar(@RequestBody OperacaoRequest req) {
        if (req.entrada() == null || req.entrada().isBlank()) {
            return ResponseEntity.badRequest().body(new RespostaPadrao("Pasta de vídeos de entrada obrigatória."));
        }

        submeterJobComRelatorio("remuxer", "Remuxer (mkvmerge)", () -> {
            try {
                Path pathVideos = normalizarCaminho(req.entrada());
                if (pathVideos == null) {
                    log.error("Caminho de vídeos inválido informado para remuxer: {}", req.entrada());
                    System.out.println("[FAIL] Caminho de vídeos inválido: " + req.entrada());
                    return;
                }
                String saidaDir = req.saida() != null && !req.saida().isBlank() ? req.saida() : "legendas-ptbr"; // Padrão
                Path pathLegendas = normalizarCaminho(saidaDir);
                if (pathLegendas == null) {
                    log.error("Caminho de legendas inválido informado para remuxer: {}", saidaDir);
                    System.out.println("[FAIL] Caminho de legendas inválido: " + saidaDir);
                    return;
                }

                if (!Files.isDirectory(pathVideos)) {
                    System.out.println("\u001B[31m[FAIL] Pasta de vídeos inválida: " + pathVideos + "\u001B[0m");
                    return;
                }
                if (!Files.isDirectory(pathLegendas)) {
                    System.out.println("\u001B[31m[FAIL] Pasta de legendas traduzidas inválida: " + pathLegendas + "\u001B[0m");
                    return;
                }

                long sincronismoMs = req.syncOffsetMs() != null ? req.syncOffsetMs() : 0;
                RelatorioRemux relatorio = remuxarLoteUseCase.executar(pathVideos, pathLegendas, sincronismoMs);
                boolean semFalhas = relatorio.getTotalErros() == 0 && relatorio.getMkvProcessadosSucesso() > 0;
                String resumo = relatorio.getMkvProcessadosSucesso() + " sucesso, " + relatorio.getTotalErros() + " erro(s)"
                    + (relatorio.getErrosLegendaInvalida() > 0
                        ? " (" + relatorio.getErrosLegendaInvalida() + " com legenda vazia/corrompida)"
                        : "");
                String linhaSeparadora = "========================================================================";
                if (semFalhas) {
                    System.out.println("\n" + AnsiCores.GREEN + linhaSeparadora + AnsiCores.RESET);
                    System.out.println(AnsiCores.GREEN + "  [SUCESSO] REMUXER FINALIZADO! (" + resumo + ")" + AnsiCores.RESET);
                    System.out.println(AnsiCores.GREEN + linhaSeparadora + AnsiCores.RESET + "\n");
                    log.info("[SUCESSO] Remuxer de videos finalizado: {}", resumo);
                } else {
                    System.out.println("\n" + AnsiCores.RED + linhaSeparadora + AnsiCores.RESET);
                    System.out.println(AnsiCores.RED + "  [FALHA] REMUXER FINALIZADO COM ERROS! (" + resumo + ")" + AnsiCores.RESET);
                    System.out.println(AnsiCores.RED + linhaSeparadora + AnsiCores.RESET + "\n");
                    log.error("[FALHA] Remuxer de videos finalizado com erros: {}", resumo);
                }
            } catch (Exception e) {
                log.error("Erro no remuxer em background", e);
                System.out.println("\u001B[31m[ERRO] Falha no Remuxer: " + e.getMessage() + "\u001B[0m");
            }
        });

        return ResponseEntity.ok(new RespostaPadrao("Remuxer de vídeos iniciado no servidor."));
    }

    /**
     * 7. MAPA DO PROJETO
     */
    @PostMapping("/mapa")
    public ResponseEntity<MapaResponse> gerarMapa() {
        Path raiz = Path.of(System.getProperty("user.dir"));
        GeradorMapaProjetoUseCase.ResultadoMapa resultado = geradorMapaProjetoUseCase.executar(raiz);
        return ResponseEntity.ok(new MapaResponse(
            resultado.relatorio(), resultado.arvoreGithub(), resultado.nomeProjeto()));
    }

    private Optional<Path> parseCaminhoSeguro(String valor, String rotulo) {
        Path p = normalizarCaminho(valor);
        return Optional.ofNullable(p);
    }

    private Path normalizarCaminho(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            String limpo = valor.trim();
            if ((limpo.startsWith("\"") && limpo.endsWith("\"")) || (limpo.startsWith("'") && limpo.endsWith("'"))) {
                if (limpo.length() >= 2) {
                    limpo = limpo.substring(1, limpo.length() - 1).trim();
                }
            }
            return limpo.isBlank() ? null : Path.of(limpo);
        } catch (InvalidPathException e) {
            log.warn("Caminho inválido informado: {}", valor);
            return null;
        }
    }

    private boolean temExtensaoSuportada(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase();
        return EXTENSOES_SUPORTADAS.stream().anyMatch(nome::endsWith);
    }
}
