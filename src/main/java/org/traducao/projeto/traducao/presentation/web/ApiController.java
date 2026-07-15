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
import org.traducao.projeto.legendasExtracao.presentation.ui.TabelaExtracaoRenderer;
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
import org.traducao.projeto.core.io.DiretorioBaseKronos;
import org.traducao.projeto.core.util.DuracaoUtil;
import org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto;
import org.traducao.projeto.traducao.presentation.ui.AnsiCores;
import org.traducao.projeto.traducao.presentation.ui.PastasExecucao;
import org.traducao.projeto.traducaoCorrige.application.LimparCacheUseCase;
import org.traducao.projeto.traducaoCorrige.domain.ResultadoManutencaoCache;

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
 * PROPÓSITO DE NEGÓCIO: expõe os módulos do pipeline local à interface web e
 * coordena sua entrada na fila única de execução.
 *
 * <p>INVARIANTES DO DOMÍNIO: caminhos são normalizados antes do uso; jobs
 * pesados passam pela fila compartilhada; respostas não executam shell com
 * concatenação de entrada do navegador.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: entradas inválidas retornam 400, conflitos
 * de execução retornam 409 e falhas internas são registradas no canal SSE.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger log = LoggerFactory.getLogger(ApiController.class);
    private static final Set<String> EXTENSOES_SUPORTADAS = Set.of(".ass", ".ssa", ".srt");

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
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

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
    public record OperacaoRequest(String entrada, String saida, String contextoId, Long syncOffsetMs,
                                  Boolean permitirRetraducao, String modoReferencia, String caminhoCache) {}
    /**
     * PROPÓSITO DE NEGÓCIO: transporta as opções exclusivas do Remuxer.
     * INVARIANTES DO DOMÍNIO: pasta de vídeo é obrigatória; offset e política de
     * faixas são validados pelo endpoint.
     * COMPORTAMENTO EM CASO DE FALHA: campos ausentes recebem fallback seguro ou
     * geram HTTP 400 antes de entrar na fila.
     */
    public record RemuxRequest(String entrada, String saida, Long syncOffsetMs,
                               Boolean preservarLegendasOriginais) {}
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
                publicarResultadoAnalise(resultadoLote);
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
     * Publica o resultado ESTRUTURADO (JSON) da análise no canal SSE
     * "analise-relatorio"; o navegador renderiza cartões/tabelas a partir dele.
     * A análise não grava mais relatório em disco — a exportação TXT é manual.
     */
    private void publicarResultadoAnalise(ResultadoAnaliseLote lote) {
        try {
            String json = objectMapper.writeValueAsString(lote);
            logStreamService.publicarLog("analise-relatorio", json);
        } catch (Exception e) {
            log.error("Erro ao serializar o resultado da análise para o navegador: {}", e.getMessage());
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

                String tabela = TabelaExtracaoRenderer.render(rel);
                if (!tabela.isBlank()) {
                    System.out.print("[36m" + tabela + "[0m");
                }

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
        if (req.contextoId() == null || req.contextoId().isBlank()) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(
                    "Contexto de tradução (lore) obrigatório: selecione o contexto antes de traduzir. "
                    + "Não há fallback silencioso para a obra usada na execução anterior."));
        }
        if (!gerenciadorContexto.existeContexto(req.contextoId())) {
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
                    System.out.println("\u001B[33mNenhum arquivo de legenda .ass/.ssa/.srt encontrado.\u001B[0m");
                    return;
                }

                System.out.println("Iniciando tradução de " + arquivos.size() + " arquivo(s)...");

                java.util.List<org.traducao.projeto.traducao.domain.ResultadoTraducaoArquivo> resultados = new java.util.ArrayList<>();
                boolean permitir = Boolean.TRUE.equals(req.permitirRetraducao());
                String loreNome = gerenciadorContexto.obterNomeContextoAtivo();
                for (int i = 0; i < arquivos.size(); i++) {
                    Path arquivo = arquivos.get(i);
                    System.out.println("\n--------------------------------------------------------------");
                    System.out.println("Processando arquivo [" + (i + 1) + "/" + arquivos.size() + "]: " + arquivo.getFileName());
                    System.out.println("--------------------------------------------------------------");
                    try {
                        var resultado = processarArquivoUseCase.processar(arquivo, permitir);
                        resultados.add(resultado);
                        if (resultado.status() == org.traducao.projeto.traducao.domain.StatusArquivoTraducao.CONCLUIDO) {
                            System.out.println("[OK] Traduzido: " + arquivo.getFileName());
                        } else {
                            System.out.println("[PARCIAL] " + arquivo.getFileName()
                                + ": saída parcial em " + resultado.arquivoSaida()
                                + "; corrija o cache e execute novamente.");
                        }
                    } catch (org.traducao.projeto.traducao.domain.exceptions.EntradaJaTraduzidaException ex) {
                        resultados.add(org.traducao.projeto.traducao.domain.ResultadoTraducaoArquivo.bloqueado(arquivo.getFileName().toString(), loreNome));
                        registrarTelemetriaFalhaTraducao(arquivo, loreNome, org.traducao.projeto.traducao.domain.StatusArquivoTraducao.BLOQUEADO, ex.getMessage());
                        System.out.println("[BLOQUEADO] " + arquivo.getFileName() + ": " + ex.getMessage());
                    } catch (org.traducao.projeto.traducao.domain.exceptions.TraducaoParcialException ex) {
                        // Abortou antes de escrever a legenda de saída: é FALHA deste run
                        // (nenhum _PT-BR gerado), mesmo que N linhas tenham sido salvas no
                        // cache para retomar depois. Não pode contar como "ok" no lote.
                        int salvas = ex.getDicionarioParcial() != null ? ex.getDicionarioParcial().size() : 0;
                        resultados.add(org.traducao.projeto.traducao.domain.ResultadoTraducaoArquivo.falha(arquivo.getFileName().toString(), loreNome));
                        registrarTelemetriaFalhaTraducao(arquivo, loreNome, org.traducao.projeto.traducao.domain.StatusArquivoTraducao.FALHOU, ex.getMessage());
                        System.out.println("[FALHA] " + arquivo.getFileName() + " abortado sem gerar saída (" + salvas + " linha(s) salvas no cache para retomar).");
                    } catch (Exception ex) {
                        resultados.add(org.traducao.projeto.traducao.domain.ResultadoTraducaoArquivo.falha(arquivo.getFileName().toString(), loreNome));
                        registrarTelemetriaFalhaTraducao(arquivo, loreNome, org.traducao.projeto.traducao.domain.StatusArquivoTraducao.FALHOU, ex.getMessage());
                        System.out.println("[FAIL] " + arquivo.getFileName() + ": " + ex.getMessage());
                    }
                }

                String tabelaTraducao = org.traducao.projeto.traducao.presentation.ui.TabelaTraducaoRenderer.render(resultados);
                if (!tabelaTraducao.isBlank()) {
                    System.out.println(tabelaTraducao);
                }

                org.traducao.projeto.traducao.domain.StatusLoteTraducao statusLote =
                    org.traducao.projeto.traducao.domain.StatusLoteTraducao.consolidar(resultados);
                long okCount = resultados.stream().filter(r ->
                    r.status() == org.traducao.projeto.traducao.domain.StatusArquivoTraducao.CONCLUIDO).count();
                long parcialCount = resultados.stream().filter(r ->
                    r.status() == org.traducao.projeto.traducao.domain.StatusArquivoTraducao.PARCIAL).count();
                long falhaCount = resultados.size() - okCount - parcialCount;
                System.out.println("\n========================================================================");
                System.out.println("  [" + statusLote.getRotulo().toUpperCase() + "] TRADUCAO LOCAL VIA LLM: "
                    + okCount + " concluído(s), " + parcialCount + " parcial(is), " + falhaCount
                    + " com falha/bloqueio de " + resultados.size() + " arquivo(s).");
                System.out.println("========================================================================\n");
                log.info("[{}] Traducao via LLM finalizada. {} concluido(s), {} parcial(is), {} falha/bloqueio de {}.",
                    statusLote.name(), okCount, parcialCount, falhaCount, resultados.size());

            } catch (Exception e) {
                log.error("Erro na tradução em background", e);
                System.out.println("\u001B[31m[ERRO] Falha geral no tradutor: " + e.getMessage() + "\u001B[0m");
            }
        });

        return ResponseEntity.ok(new RespostaPadrao("Tradução via LLM iniciada no servidor."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aceita a limpeza segura da pasta persistente de cache.
     * <p>INVARIANTES DO DOMÍNIO: caminho e contexto informado são validados antes da fila.
     * <p>COMPORTAMENTO EM CASO DE FALHA: retorna 400 para entrada inválida; falhas do job aparecem no console/status final.
     */
    @PostMapping("/corrigir-cache")
    public ResponseEntity<RespostaPadrao> limparCache(@RequestBody OperacaoRequest req) {
        String cacheDir = req.entrada() != null && !req.entrada().isBlank() ? req.entrada() : "cache";
        Path pathCache = normalizarCaminho(cacheDir);
        if (pathCache == null) {
            return ResponseEntity.badRequest().body(new RespostaPadrao("Caminho de cache inválido: " + cacheDir));
        }
        if (req.contextoId() != null && !req.contextoId().isBlank()
            && !gerenciadorContexto.existeContexto(req.contextoId())) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(
                "Contexto desconhecido: \"" + req.contextoId() + "\"."));
        }

        submeterJobComRelatorio("correcao", "Limpeza e Auditoria de Cache", () -> {
            try {
                ResultadoManutencaoCache resultado = limparCacheUseCase.executar(pathCache, req.contextoId());
                imprimirResultadoCache("LIMPEZA E AUDITORIA DE CACHE", resultado);
            } catch (Exception e) {
                log.error("Erro ao limpar cache", e);
                System.out.println("\u001B[31m[ERRO] Limpeza do cache falhou: " + e.getMessage() + "\u001B[0m");
            }
        });

        return ResponseEntity.ok(new RespostaPadrao(
            "Limpeza de cache aceita pela fila. A conclusão e o status real aparecerão no console."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aceita o preenchimento online de lacunas do cache.
     * <p>INVARIANTES DO DOMÍNIO: somente contexto conhecido entra na fila; o uso online é explícito.
     * <p>COMPORTAMENTO EM CASO DE FALHA: retorna 400 antes da fila ou registra falha real no console do job.
     */
    @PostMapping("/corrigir-scraping")
    public ResponseEntity<RespostaPadrao> corrigirScraping(@RequestBody OperacaoRequest req) {
        String cacheDir = req.entrada() != null && !req.entrada().isBlank() ? req.entrada() : "cache";
        Path pathCache = normalizarCaminho(cacheDir);
        if (pathCache == null) {
            return ResponseEntity.badRequest().body(new RespostaPadrao("Caminho de cache inválido: " + cacheDir));
        }
        if (req.contextoId() != null && !req.contextoId().isBlank()
            && !gerenciadorContexto.existeContexto(req.contextoId())) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(
                "Contexto desconhecido: \"" + req.contextoId() + "\"."));
        }

        submeterJobComRelatorio("correcao", "Correção via Google Translate", () -> {
            try {
                ResultadoManutencaoCache resultado = corrigirComGoogleUseCase.executar(pathCache, req.contextoId());
                imprimirResultadoCache("CORREÇÃO ONLINE VIA GOOGLE TRANSLATE", resultado);
            } catch (Exception e) {
                log.error("Erro ao executar scraping", e);
                System.out.println("\u001B[31m[ERRO] Raspagem do Google falhou: " + e.getMessage() + "\u001B[0m");
            }
        });

        return ResponseEntity.ok(new RespostaPadrao(
            "Correção online aceita pela fila. A conclusão e o status real aparecerão no console."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aceita a revisão de concordância do cache via LLM local.
     * <p>INVARIANTES DO DOMÍNIO: contexto é validado e disponibilidade do modelo é checada antes da revisão.
     * <p>COMPORTAMENTO EM CASO DE FALHA: rejeita contexto inválido e registra indisponibilidade/status parcial no console.
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
                ResultadoManutencaoCache resultado = revisarCacheUseCase.executar(pathCache, req.contextoId());
                imprimirResultadoCache("REVISÃO GRAMATICAL DO CACHE", resultado);
            } catch (Exception e) {
                log.error("Erro na revisão gramatical do cache", e);
                System.out.println("\u001B[31m[ERRO] Revisão gramatical falhou: " + e.getMessage() + "\u001B[0m");
            }
        });

        return ResponseEntity.ok(new RespostaPadrao(
            "Revisão local aceita pela fila. A conclusão e o status real aparecerão no console."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: apresenta no console web o desfecho real dos três
     * modos de manutenção do banco de cache, incluindo falhas e cancelamento.
     *
     * <p>INVARIANTES DO DOMÍNIO: somente {@code CONCLUIDO} usa banner verde;
     * qualquer outro status informa que o resultado exige atenção; a orientação
     * de avançar à Opção 6 aparece após toda execução que altera o cache.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: resultado nulo é tratado como falha e
     * não provoca {@link NullPointerException} no job de background.
     */
    private void imprimirResultadoCache(String operacao, ResultadoManutencaoCache resultado) {
        if (resultado == null) {
            System.out.println(AnsiCores.RED + "[FALHA] " + operacao + " não retornou resultado." + AnsiCores.RESET);
            return;
        }
        String resumo = operacao + " — status=" + resultado.status()
            + ", arquivos=" + resultado.arquivosAnalisados()
            + ", alterados=" + resultado.arquivosAlterados()
            + ", corrigidos=" + resultado.itensCorrigidos()
            + ", pendentes=" + resultado.itensPendentes()
            + ", falhas=" + resultado.falhas();
        if ("CONCLUIDO".equals(resultado.status())) {
            System.out.println(AnsiCores.GREEN + "[SUCESSO] " + resumo + AnsiCores.RESET);
        } else {
            System.out.println(AnsiCores.YELLOW + "[ATENÇÃO] " + resumo + AnsiCores.RESET);
        }
        if (resultado.arquivosAlterados() > 0) {
            System.out.println(AnsiCores.CYAN
                + "[PRÓXIMO PASSO] Avance para a Opção 6. Ela sincronizará este cache mais novo no ASS antes da revisão."
                + AnsiCores.RESET);
        }
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

        RevisarLegendasUseCase.ModoReferenciaRevisao referencia = resolverModoReferencia(req.modoReferencia());
        final Path cacheDir = resolverCacheDir(referencia, req.caminhoCache());
        final Path pathEnUso = referencia == RevisarLegendasUseCase.ModoReferenciaRevisao.CACHE ? null : pathEnFinal;

        Optional<String> erroCache = validarCacheDirModo(referencia, cacheDir);
        if (erroCache.isPresent()) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(erroCache.get()));
        }

        submeterJobComRelatorio("revisao", "Revisão de Legendas Traduzidas", () -> {
            try {
                ResultadoRevisaoLegendas resultado = revisarLegendasUseCase.executar(
                    pathPt, pathEnUso, cacheDir, null,
                    RevisarLegendasUseCase.ModoRevisaoLegendas.GOOGLE, req.contextoId(), referencia);
                imprimirResultadoRevisaoLegendas("REVISÃO DE LEGENDAS TRADUZIDAS", resultado);
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

        RevisarLegendasUseCase.ModoReferenciaRevisao referencia = resolverModoReferencia(req.modoReferencia());
        final Path cacheDir = resolverCacheDir(referencia, req.caminhoCache());
        final Path pathEnUso = referencia == RevisarLegendasUseCase.ModoReferenciaRevisao.CACHE ? null : pathEnFinal;

        Optional<String> erroCache = validarCacheDirModo(referencia, cacheDir);
        if (erroCache.isPresent()) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(erroCache.get()));
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
                    pathEnUso,
                    cacheDir,
                    null,
                    RevisarLegendasUseCase.ModoRevisaoLegendas.LLM_CONCORDANCIA,
                    req.contextoId(),
                    referencia
                );
                imprimirResultadoRevisaoLegendas("REVISÃO DE CONCORDÂNCIA PT-BR (LLM)", resultado);
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
     * PROPÓSITO DE NEGÓCIO: apresenta o desfecho verdadeiro da Opção 6 sem
     * transformar execução tecnicamente estável com pendências em sucesso total.
     *
     * <p>INVARIANTES DO DOMÍNIO: verde exige zero pendências; amarelo informa
     * problemas restantes e zero arquivos mantém o aviso histórico.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: resultado nulo é tratado como falha e
     * não provoca erro no job de background.
     */
    private void imprimirResultadoRevisaoLegendas(String titulo, ResultadoRevisaoLegendas resultado) {
        if (resultado == null) {
            System.out.println(AnsiCores.RED + "[FALHA] " + titulo + " não retornou resultado."
                + AnsiCores.RESET);
            return;
        }
        if (resultado.arquivosAnalisados() == 0) {
            System.out.println(AnsiCores.YELLOW
                + "[AVISO] Revisão concluída sem arquivos .ass/.ssa para analisar."
                + AnsiCores.RESET);
            return;
        }

        boolean concluido = "CONCLUIDO".equals(resultado.status());
        String cor = concluido ? AnsiCores.GREEN : AnsiCores.YELLOW;
        String rotulo = concluido ? "[SUCESSO]" : "[ATENÇÃO]";
        System.out.println("\n" + cor + "========================================================================" + AnsiCores.RESET);
        System.out.println(cor + "  " + rotulo + " " + titulo + " — " + resultado.status() + AnsiCores.RESET);
        System.out.println(cor + "========================================================================" + AnsiCores.RESET);
        System.out.println(AnsiCores.CYAN + "  • Arquivos Analisados : " + resultado.arquivosAnalisados() + AnsiCores.RESET);
        System.out.println(AnsiCores.CYAN + "  • Problemas Detectados: " + resultado.falasComProblema() + AnsiCores.RESET);
        System.out.println(AnsiCores.GREEN + "  • Falas Corrigidas    : " + resultado.falasCorrigidas() + AnsiCores.RESET);
        System.out.println((resultado.falasPendentes() > 0 ? AnsiCores.YELLOW : AnsiCores.GREEN)
            + "  • Falas Pendentes     : " + resultado.falasPendentes() + AnsiCores.RESET);
        System.out.println(cor + "========================================================================\n" + AnsiCores.RESET);
        log.info("[{}] {}: arquivos={}, corrigidas={}, pendentes={}.", resultado.status(), titulo,
            resultado.arquivosAnalisados(), resultado.falasCorrigidas(), resultado.falasPendentes());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: valida e agenda um único lote de remux com política
     * explícita para legendas originais.
     *
     * <p>INVARIANTES DO DOMÍNIO: pastas existem antes da aceitação; offset fica em
     * faixa operacional; nenhuma segunda operação entra enquanto a fila está
     * ocupada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada inválida retorna 400, fila ocupada
     * retorna 409 e falha do lote aparece no status final do console.
     */
    @PostMapping("/remuxar")
    public synchronized ResponseEntity<RespostaPadrao> remuxar(@RequestBody RemuxRequest req) {
        if (req == null || req.entrada() == null || req.entrada().isBlank()) {
            return ResponseEntity.badRequest().body(new RespostaPadrao("Pasta de vídeos de entrada obrigatória."));
        }
        Path pathVideos = normalizarCaminho(req.entrada());
        if (pathVideos == null || !Files.isDirectory(pathVideos)) {
            return ResponseEntity.badRequest().body(new RespostaPadrao("Pasta de vídeos inválida: " + req.entrada()));
        }
        Path pathLegendas = req.saida() == null || req.saida().isBlank()
            ? localizarPastaLegendasAutomatica(pathVideos)
            : normalizarCaminho(req.saida());
        if (pathLegendas == null || !Files.isDirectory(pathLegendas)) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(
                "Pasta de legendas inválida. Informe-a ou crie uma subpasta como 'legendas pt' dentro da pasta de vídeos."));
        }
        long sincronismoMs = req.syncOffsetMs() == null ? 0 : req.syncOffsetMs();
        if (sincronismoMs < -86_400_000L || sincronismoMs > 86_400_000L) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(
                "Sincronismo fora do limite seguro de ±86.400.000 ms (24 horas)."));
        }
        if (filaExecucao.ocupada()) {
            return ResponseEntity.status(409).body(new RespostaPadrao(
                "O pipeline já possui uma operação em andamento ou aguardando. Aguarde a conclusão antes de iniciar o Remuxer."));
        }
        boolean preservarOriginais = Boolean.TRUE.equals(req.preservarLegendasOriginais());
        submeterJobComRelatorio("remuxer", "Remuxer (mkvmerge)", () -> {
            try {
                RelatorioRemux relatorio = remuxarLoteUseCase.executar(
                    pathVideos, pathLegendas, sincronismoMs, preservarOriginais);
                String status = relatorio.getStatusFinal();
                String resumo = "status=" + status
                    + ", sucessos=" + relatorio.getMkvProcessadosSucesso()
                    + ", pendências=" + relatorio.getTotalPendencias()
                    + ", falhas=" + relatorio.getTotalErros()
                    + ", semLegenda=" + relatorio.getVideosSemLegenda()
                    + ", ambíguos=" + relatorio.getPareamentosAmbiguos()
                    + ", existentesPreservados=" + relatorio.getSaidasJaExistentes();
                String linhaSeparadora = "========================================================================";
                if ("CONCLUIDO".equals(status)) {
                    System.out.println("\n" + AnsiCores.GREEN + linhaSeparadora + AnsiCores.RESET);
                    System.out.println(AnsiCores.GREEN + "  [SUCESSO] REMUXER FINALIZADO! (" + resumo + ")" + AnsiCores.RESET);
                    System.out.println(AnsiCores.GREEN + linhaSeparadora + AnsiCores.RESET + "\n");
                    log.info("[SUCESSO] Remuxer de videos finalizado: {}", resumo);
                } else if ("CONCLUIDO_COM_PENDENCIAS".equals(status) || "SEM_ARQUIVOS".equals(status)) {
                    System.out.println("\n" + AnsiCores.YELLOW + linhaSeparadora + AnsiCores.RESET);
                    System.out.println(AnsiCores.YELLOW + "  [ATENÇÃO] REMUXER FINALIZADO! (" + resumo + ")" + AnsiCores.RESET);
                    System.out.println(AnsiCores.YELLOW + linhaSeparadora + AnsiCores.RESET + "\n");
                    log.warn("[ATENÇÃO] Remuxer finalizado: {}", resumo);
                } else {
                    System.out.println("\n" + AnsiCores.RED + linhaSeparadora + AnsiCores.RESET);
                    System.out.println(AnsiCores.RED + "  [FALHA/CANCELADO] REMUXER FINALIZADO! (" + resumo + ")" + AnsiCores.RESET);
                    System.out.println(AnsiCores.RED + linhaSeparadora + AnsiCores.RESET + "\n");
                    log.error("[FALHA/CANCELADO] Remuxer finalizado: {}", resumo);
                }
            } catch (Exception e) {
                log.error("Erro no remuxer em background", e);
                System.out.println("\u001B[31m[ERRO] Falha no Remuxer: " + e.getMessage() + "\u001B[0m");
            }
        });

        return ResponseEntity.ok(new RespostaPadrao(
            "Remuxer aceito pela fila. O resultado real, arquivo por arquivo, aparecerá no console."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: encontra automaticamente a pasta local de legendas ao
     * lado dos vídeos usando nomes adotados no pipeline do Paulo.
     *
     * <p>INVARIANTES DO DOMÍNIO: somente subdiretórios diretos são considerados e
     * a comparação ignora caixa, espaço, hífen e underscore.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: erro de leitura ou ausência devolve
     * {@code null}, fazendo o endpoint pedir um caminho explícito.
     */
    private Path localizarPastaLegendasAutomatica(Path pastaVideos) {
        Set<String> nomesAceitos = Set.of("legendaspt", "legendasptbr", "legendasportugues");
        try (Stream<Path> stream = Files.list(pastaVideos)) {
            return stream.filter(Files::isDirectory)
                .filter(p -> nomesAceitos.contains(p.getFileName().toString().toLowerCase()
                    .replace("-", "").replace("_", "").replace(" ", "")))
                .sorted()
                .findFirst()
                .orElse(null);
        } catch (IOException e) {
            log.warn("Não foi possível procurar pasta automática de legendas em {}: {}", pastaVideos, e.getMessage());
            return null;
        }
    }

    /**
     * 7. MAPA DO PROJETO
     */
    @PostMapping("/mapa")
    public ResponseEntity<MapaResponse> gerarMapa() {
        // Raiz do projeto a mapear. Via DiretorioBaseKronos: em produção é o
        // diretório de trabalho (raiz do repositório); sob a suíte de testes é
        // a árvore descartável, evitando reescrever o mapa_projeto.md real.
        Path raiz = DiretorioBaseKronos.base().toAbsolutePath();
        GeradorMapaProjetoUseCase.ResultadoMapa resultado = geradorMapaProjetoUseCase.executar(raiz);
        return ResponseEntity.ok(new MapaResponse(
            resultado.relatorio(), resultado.arvoreGithub(), resultado.nomeProjeto()));
    }

    private Optional<Path> parseCaminhoSeguro(String valor, String rotulo) {
        Path p = normalizarCaminho(valor);
        return Optional.ofNullable(p);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: traduz a aba escolhida na Opção 6 (Ambos/Cache) para o
     * modo de referência do use case de revisão.
     * <p>INVARIANTES DO DOMÍNIO: qualquer valor diferente de "CACHE" cai em AMBOS
     * (comportamento histórico e retrocompatível).
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada nula/vazia resulta em AMBOS.
     */
    private RevisarLegendasUseCase.ModoReferenciaRevisao resolverModoReferencia(String modo) {
        return modo != null && "CACHE".equalsIgnoreCase(modo.trim())
            ? RevisarLegendasUseCase.ModoReferenciaRevisao.CACHE
            : RevisarLegendasUseCase.ModoReferenciaRevisao.AMBOS;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: resolve a pasta de cache usada como referência no modo
     * Cache; nos demais casos mantém a pasta padrão do projeto.
     * <p>INVARIANTES DO DOMÍNIO: só o modo Cache respeita a pasta informada pelo
     * usuário; AMBOS sempre usa {@code cache}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: caminho inválido/ausente no modo Cache
     * volta ao padrão {@code cache}.
     */
    private Path resolverCacheDir(RevisarLegendasUseCase.ModoReferenciaRevisao referencia, String caminhoCache) {
        if (referencia == RevisarLegendasUseCase.ModoReferenciaRevisao.CACHE) {
            Path escolhido = normalizarCaminho(caminhoCache);
            if (escolhido != null) {
                return escolhido;
            }
        }
        return Path.of("cache");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: no modo Cache, garante que a pasta informada existe, é
     * um diretório e contém ao menos um {@code .cache.json} antes de a fila iniciar
     * um job que não teria referência alguma.
     * <p>INVARIANTES DO DOMÍNIO: só valida no modo Cache; AMBOS não é afetado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve mensagem de erro (vira HTTP 400);
     * ausência de erro devolve {@link Optional#empty()}.
     */
    private Optional<String> validarCacheDirModo(
            RevisarLegendasUseCase.ModoReferenciaRevisao referencia, Path cacheDir) {
        if (referencia != RevisarLegendasUseCase.ModoReferenciaRevisao.CACHE) {
            return Optional.empty();
        }
        if (cacheDir == null || !java.nio.file.Files.isDirectory(cacheDir)) {
            return Optional.of("Pasta de cache inexistente ou não é um diretório: "
                + (cacheDir == null ? "(vazio)" : cacheDir.toString()));
        }
        try (java.util.stream.Stream<Path> walk = java.nio.file.Files.walk(cacheDir)) {
            boolean temCache = walk.filter(java.nio.file.Files::isRegularFile)
                .anyMatch(p -> p.getFileName().toString().toLowerCase().endsWith(".cache.json"));
            if (!temCache) {
                return Optional.of("Nenhum arquivo .cache.json encontrado na pasta de cache: " + cacheDir);
            }
        } catch (java.io.IOException e) {
            return Optional.of("Falha ao ler a pasta de cache: " + e.getMessage());
        }
        return Optional.empty();
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

    /**
     * Registra na telemetria um arquivo que FALHOU/foi BLOQUEADO na tradução —
     * sem isso, o dataset perdia justamente os casos mais úteis (falhas). Carrega
     * o lore e o status final para dar proveniência ao registro.
     */
    private void registrarTelemetriaFalhaTraducao(Path arquivo, String lore,
            org.traducao.projeto.traducao.domain.StatusArquivoTraducao status, String motivo) {
        String nome = arquivo.getFileName().toString();
        Path pai = arquivo.getParent();
        String anime = (pai != null && pai.getParent() != null && pai.getParent().getFileName() != null)
            ? pai.getParent().getFileName().toString() : "Desconhecido";
        telemetriaService.registrarTraducao(new org.traducao.projeto.telemetria.LlmTelemetria(
            nome, null, 0, 0, 0, 0L,
            java.util.List.of(motivo != null ? motivo : status.getRotulo()),
            anime, "Temporada Única", java.time.Instant.now().toString(),
            lore, status.name()));
    }

    private boolean temExtensaoSuportada(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase();
        return EXTENSOES_SUPORTADAS.stream().anyMatch(nome::endsWith);
    }
}
