package org.traducao.projeto.correcaoLegendas.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.correcaoLegendas.domain.CorrecaoLegendasRelatorioJson;
import org.traducao.projeto.correcaoLegendas.domain.LogEventoCorrecaoLegendas;
import org.traducao.projeto.correcaoLegendas.domain.ResultadoCorrecaoLegendas;
import org.traducao.projeto.correcaoLegendas.infrastructure.CorrecaoLegendasLogPersistencia;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda;
import org.traducao.projeto.traducao.domain.legenda.EventoLegenda;
import org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto;
import org.traducao.projeto.traducao.infrastructure.legenda.EscritorLegendaAss;
import org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaAss;
import org.traducao.projeto.traducao.presentation.ui.AnsiCores;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class CorrigirLegendasUseCase {

    private static final Logger log = LoggerFactory.getLogger(CorrigirLegendasUseCase.class);
    private static final String TIPO_OPERACAO = "Correcao de Legendas (.ass original->traduzida)";

    private final LeitorLegendaAss leitor;
    private final EscritorLegendaAss escritor;
    private final SanitizadorTagsService sanitizador;
    private final CorretorTraducaoLlmService corretorLlm;
    private final GerenciadorContexto gerenciadorContexto;
    private final TelemetriaService telemetriaService;
    private final CorrecaoLegendasLogPersistencia logPersistencia;

    public CorrigirLegendasUseCase(
        LeitorLegendaAss leitor,
        EscritorLegendaAss escritor,
        SanitizadorTagsService sanitizador,
        CorretorTraducaoLlmService corretorLlm,
        GerenciadorContexto gerenciadorContexto,
        TelemetriaService telemetriaService,
        CorrecaoLegendasLogPersistencia logPersistencia
    ) {
        this.leitor = leitor;
        this.escritor = escritor;
        this.sanitizador = sanitizador;
        this.corretorLlm = corretorLlm;
        this.gerenciadorContexto = gerenciadorContexto;
        this.telemetriaService = telemetriaService;
        this.logPersistencia = logPersistencia;
    }

    public ResultadoCorrecaoLegendas corrigirPasta(Path pastaBase, String contextoId) {
        return corrigirPasta(pastaBase, pastaBase, contextoId);
    }

    public ResultadoCorrecaoLegendas corrigirPasta(Path pastaOriginal, Path pastaTraduzida, String contextoId) {
        Instant inicio = Instant.now();
        List<LogEventoCorrecaoLegendas> eventos = new ArrayList<>();

        if (!Files.isDirectory(pastaOriginal) || !Files.isDirectory(pastaTraduzida)) {
            String msg = "Pastas não encontradas — esperava " + pastaOriginal + " e " + pastaTraduzida;
            out(eventos, inicio, "WARN", null, msg, AnsiCores.YELLOW);
            ResultadoCorrecaoLegendas resultado = new ResultadoCorrecaoLegendas(0, 0, 0, 0, 0, 1, List.of(msg), null);
            registrarTelemetria(pastaOriginal, pastaTraduzida, inicio, false, null, resultado, eventos);
            return resultado;
        }

        boolean llmHabilitado = aplicarContextoLlm(contextoId);
        String contextoAtivo = llmHabilitado ? gerenciadorContexto.obterNomeContextoAtivo() : null;

        out(eventos, inicio, "INFO", null, "=== Iniciando Correcao de Legendas ===", AnsiCores.CYAN);
        out(eventos, inicio, "INFO", null, "Pasta original/ref: " + pastaOriginal, AnsiCores.WHITE);
        out(eventos, inicio, "INFO", null, "Pasta traduzida/alvo: " + pastaTraduzida, AnsiCores.WHITE);
        if (llmHabilitado) {
            out(eventos, inicio, "INFO", null, "Correcao de traducao via LLM ativa (contexto: "
                + contextoAtivo + ")", AnsiCores.CYAN);
        }

        int[] curados = {0};
        int[] corrigidosLlm = {0};
        int[] semAlteracao = {0};
        int[] semPar = {0};
        int[] traducaoAusente = {0};
        List<String> erros = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(pastaOriginal)) {
            List<Path> originais = stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".ass"))
                    .toList();

            for (Path arqOriginal : originais) {
                corrigirArquivo(arqOriginal, pastaTraduzida, llmHabilitado, inicio, eventos,
                    curados, corrigidosLlm, semAlteracao, semPar, traducaoAusente, erros);
            }
        } catch (IOException e) {
            log.error("Erro ao percorrer pasta original de legendas: {}", pastaOriginal, e);
            String msg = "Erro ao percorrer pasta original: " + e.getMessage();
            erros.add(msg);
            out(eventos, inicio, "ERROR", null, msg, AnsiCores.RED);
        }

        ResultadoCorrecaoLegendas resultado = new ResultadoCorrecaoLegendas(
            curados[0], corrigidosLlm[0], semAlteracao[0], semPar[0], traducaoAusente[0], erros.size(), erros, null);
        resultado = registrarTelemetria(pastaOriginal, pastaTraduzida, inicio, llmHabilitado, contextoAtivo, resultado, eventos);

        if (erros.isEmpty()) {
            out(eventos, inicio, "INFO", null, "Correcao de legendas concluida: " + curados[0]
                + " curado(s), " + corrigidosLlm[0] + " corrigido(s) via LLM, "
                + semAlteracao[0] + " ja perfeito(s), " + traducaoAusente[0] + " fala(s) sem traducao.", AnsiCores.GREEN);
        } else {
            out(eventos, inicio, "WARN", null, "Correcao de legendas concluida com " + erros.size()
                + " erro(s): " + curados[0] + " curado(s), " + corrigidosLlm[0] + " corrigido(s) via LLM, "
                + semAlteracao[0] + " ja perfeito(s), " + traducaoAusente[0] + " fala(s) sem traducao.", AnsiCores.RED);
        }
        log.info("Correcao de legendas finalizada em {}: {} curados, {} corrigidos via LLM, {} sem alteração, {} sem par traduzido, {} fala(s) sem traducao, {} erro(s)",
            pastaOriginal.getFileName(), curados[0], corrigidosLlm[0], semAlteracao[0], semPar[0], traducaoAusente[0], erros.size());
        return resultado;
    }

    /**
     * Define o contexto ativo (lore/system prompt) usado pelo MistralPort quando
     * a correção via LLM está habilitada. Sem contextoId, a correção permanece
     * 100% estrutural/regex (sem chamadas ao LLM).
     */
    private boolean aplicarContextoLlm(String contextoId) {
        if (contextoId == null || contextoId.isBlank()) {
            return false;
        }
        if (!gerenciadorContexto.existeContexto(contextoId)) {
            System.out.println(AnsiCores.YELLOW + "Contexto desconhecido \"" + contextoId
                + "\" — cura seguirá apenas estrutural (sem LLM)." + AnsiCores.RESET);
            return false;
        }
        gerenciadorContexto.definirContextoAtivo(contextoId);
        return true;
    }

    private void corrigirArquivo(
        Path arqOriginal,
        Path pastaTraduzida,
        boolean llmHabilitado,
        Instant inicio,
        List<LogEventoCorrecaoLegendas> eventos,
        int[] curados,
        int[] corrigidosLlm,
        int[] semAlteracao,
        int[] semPar,
        int[] traducaoAusente,
        List<String> erros
    ) {
        String nomeOriginal = arqOriginal.getFileName().toString();
        Path arqTraduzido = localizarArquivoTraduzido(arqOriginal, pastaTraduzida);

        if (!Files.exists(arqTraduzido)) {
            semPar[0]++;
            out(eventos, inicio, "WARN", nomeOriginal, "Sem legenda traduzida pareada para " + nomeOriginal, AnsiCores.YELLOW);
            return;
        }

        try {
            DocumentoLegenda docOriginal = leitor.ler(arqOriginal);
            DocumentoLegenda docTraduzido = leitor.ler(arqTraduzido);

            if (docOriginal.eventos().size() != docTraduzido.eventos().size()) {
                // As legendas não estão alinhadas 1:1 (ex.: original foi re-extraído
                // depois da tradução). Tentar curar por posição aqui arrisca cortar
                // ou embaralhar falas sem nenhum aviso — mais seguro recusar e avisar
                // do que gravar um arquivo truncado.
                String msg = arqTraduzido.getFileName() + ": contagem de eventos não corresponde ("
                    + docOriginal.eventos().size() + " no original vs " + docTraduzido.eventos().size()
                    + " na tradução) — arquivo pulado, nenhuma alteração feita.";
                log.warn(msg);
                out(eventos, inicio, "WARN", arqTraduzido.getFileName().toString(), "[Pulado] " + msg, AnsiCores.YELLOW);
                erros.add(msg);
                return;
            }

            boolean houveModificacao = false;
            int linhasCuradas = 0;
            int linhasCorrigidasLlm = 0;
            List<EventoLegenda> novosEventos = new ArrayList<>(docTraduzido.eventos().size());

            for (int i = 0; i < docOriginal.eventos().size(); i++) {
                EventoLegenda evtOriginal = docOriginal.eventos().get(i);
                EventoLegenda evtTraduzido = docTraduzido.eventos().get(i);

                if (evtOriginal.isDialogo() && evtTraduzido.isDialogo()
                    && evtOriginal.temTexto() && evtTraduzido.temTexto()) {
                    String textoOriginal = evtOriginal.texto();
                    String textoPtBrAntigo = evtTraduzido.texto();

                    if (!textoOriginal.isBlank() && textoPtBrAntigo.isBlank()) {
                        // Original tem fala real, mas a traducao chegou vazia (falha
                        // silenciosa de um passo anterior do pipeline). Gravar aqui so
                        // o prefixo de tags da original criaria uma legenda "corrigida"
                        // sem nenhum texto — reportar como pendente em vez de mascarar.
                        traducaoAusente[0]++;
                        novosEventos.add(evtTraduzido);
                        out(eventos, inicio, "WARN", arqTraduzido.getFileName().toString(),
                            "Fala " + (i + 1) + " sem traducao (vazia) — original possui texto; nao corrigido, revisao manual necessaria.",
                            AnsiCores.YELLOW);
                        continue;
                    }

                    String textoCurado = sanitizador.curarTags(textoOriginal, textoPtBrAntigo);
                    boolean corrigidoPorLlm = false;

                    if (llmHabilitado) {
                        Optional<String> corrigidoLlm = corretorLlm.corrigirSeNecessario(textoOriginal, textoCurado);
                        if (corrigidoLlm.isPresent()) {
                            // Passagem estrutural final: garante que a retradução do LLM
                            // não perdeu/alucinou as tags de formatação do original.
                            textoCurado = sanitizador.curarTags(textoOriginal, corrigidoLlm.get());
                            corrigidoPorLlm = true;
                            out(eventos, inicio, "INFO", arqTraduzido.getFileName().toString(),
                                "Fala " + (i + 1) + " corrigida via LLM apos validacao.", AnsiCores.MAGENTA);
                        }
                    }

                    if (!textoPtBrAntigo.equals(textoCurado)) {
                        novosEventos.add(evtTraduzido.comTexto(textoCurado));
                        houveModificacao = true;
                        if (corrigidoPorLlm) {
                            linhasCorrigidasLlm++;
                        } else {
                            linhasCuradas++;
                        }
                    } else {
                        novosEventos.add(evtTraduzido);
                    }
                } else {
                    if (evtOriginal.isDialogo() && evtOriginal.temTexto() && !evtOriginal.texto().isBlank()
                        && (!evtTraduzido.isDialogo() || !evtTraduzido.temTexto())) {
                        // Original tem fala real na posicao i, mas o evento traduzido no
                        // mesmo indice nao e reconhecido como dialogo/texto (tipo de linha
                        // divergente). Contagem de eventos bateu, mas o alinhamento 1:1
                        // pode estar quebrado — melhor avisar do que corrigir as cegas.
                        out(eventos, inicio, "WARN", arqTraduzido.getFileName().toString(),
                            "Fala " + (i + 1) + ": estrutura do evento traduzido diverge da original (tipo/texto ausente) — mantido sem alteracao.",
                            AnsiCores.YELLOW);
                    }
                    novosEventos.add(evtTraduzido);
                }
            }

            if (houveModificacao) {
                DocumentoLegenda documentoCurado = new DocumentoLegenda(
                    docTraduzido.cabecalho(),
                    novosEventos,
                    docTraduzido.quebraDeLinha(),
                    docTraduzido.comBom()
                );
                escritor.escrever(arqTraduzido, documentoCurado);
                curados[0]++;
                corrigidosLlm[0] += linhasCorrigidasLlm;
                out(eventos, inicio, "INFO", arqTraduzido.getFileName().toString(), "[Corrigido] "
                    + arqTraduzido.getFileName() + " (" + linhasCuradas + " tags restauradas, "
                    + linhasCorrigidasLlm + " corrigidas via LLM)", AnsiCores.GREEN);
            } else {
                semAlteracao[0]++;
                out(eventos, inicio, "INFO", arqTraduzido.getFileName().toString(), "[OK] "
                    + arqTraduzido.getFileName() + " (sem alteracao)", AnsiCores.DIM);
            }

        } catch (Exception e) {
            String msg = "Falha ao curar " + arqTraduzido.getFileName() + ": " + e.getMessage();
            log.error(msg, e);
            out(eventos, inicio, "ERROR", arqTraduzido.getFileName().toString(), "[Erro] " + msg, AnsiCores.RED);
            erros.add(msg);
        }
    }

    private Path localizarArquivoTraduzido(Path arqOriginal, Path pastaTraduzida) {
        String nomeOriginal = arqOriginal.getFileName().toString();
        String nomeBase = nomeOriginal.substring(0, nomeOriginal.lastIndexOf("."));
        Set<String> candidatos = new LinkedHashSet<>();

        candidatos.add(nomeBase + "_PT-BR.ass");
        candidatos.add(nomeBase + "_PTBR.ass");
        candidatos.add(nomeBase.replace("_ENG", "_PT-BR") + ".ass");
        candidatos.add(nomeBase.replace("_ENG", "_PTBR") + ".ass");
        candidatos.add(nomeBase.replace("_EN", "_PT-BR") + ".ass");
        candidatos.add(nomeBase.replace("_EN", "_PTBR") + ".ass");
        candidatos.add(nomeBase.replace("_eng", "_PT-BR") + ".ass");
        candidatos.add(nomeBase.replace("_eng", "_PTBR") + ".ass");
        candidatos.add(nomeBase.replace("_en", "_PT-BR") + ".ass");
        candidatos.add(nomeBase.replace("_en", "_PTBR") + ".ass");

        for (String candidato : candidatos) {
            Path caminho = pastaTraduzida.resolve(candidato);
            if (Files.exists(caminho)) {
                return caminho;
            }
        }

        return pastaTraduzida.resolve(nomeBase + "_PT-BR.ass");
    }

    private ResultadoCorrecaoLegendas registrarTelemetria(
        Path pastaOriginal,
        Path pastaTraduzida,
        Instant inicio,
        boolean llmHabilitado,
        String contexto,
        ResultadoCorrecaoLegendas resultado,
        List<LogEventoCorrecaoLegendas> eventos
    ) {
        long tempoTotalMs = Duration.between(inicio, Instant.now()).toMillis();
        int itensDetectados = resultado.curados() + resultado.corrigidosLlm() + resultado.traducaoAusente() + resultado.totalErros();
        int itensCorrigidos = resultado.curados() + resultado.corrigidosLlm();
        OperacaoTelemetria operacao = TelemetriaService.criarOperacao(
            TIPO_OPERACAO,
            "Original: " + pastaOriginal + " | Traduzida: " + pastaTraduzida,
            tempoTotalMs,
            resultado.totalArquivosAnalisados(),
            itensDetectados,
            itensCorrigidos
        );

        String relatorioJson = null;
        try {
            CorrecaoLegendasRelatorioJson relatorio = new CorrecaoLegendasRelatorioJson(
                operacao,
                pastaOriginal.toString(),
                pastaTraduzida.toString(),
                llmHabilitado,
                contexto,
                resultado,
                List.copyOf(eventos)
            );
            relatorioJson = logPersistencia.salvarRelatorioJson(pastaTraduzida, relatorio).toString();
            out(eventos, inicio, "INFO", null, "Relatorio JSON salvo em: " + relatorioJson, AnsiCores.CYAN);
        } catch (Exception e) {
            log.warn("Falha ao salvar relatorio JSON de correcao de legendas: {}", e.getMessage());
        }

        telemetriaService.registrarOperacao(operacao);
        telemetriaService.salvar(TelemetriaService.resolverPastaRelatorios(pastaTraduzida));
        return new ResultadoCorrecaoLegendas(
            resultado.curados(),
            resultado.corrigidosLlm(),
            resultado.semAlteracao(),
            resultado.semPar(),
            resultado.traducaoAusente(),
            resultado.totalErros(),
            resultado.erros(),
            relatorioJson
        );
    }

    private void out(
        List<LogEventoCorrecaoLegendas> eventos,
        Instant inicio,
        String nivel,
        String arquivo,
        String mensagem,
        String cor
    ) {
        String prefixo = "[UTC " + Instant.now() + " | +" + Duration.between(inicio, Instant.now()).toMillis() + "ms]";
        String linha = prefixo + " " + mensagem;
        System.out.println(cor + linha + AnsiCores.RESET);
        eventos.add(new LogEventoCorrecaoLegendas(Instant.now().toString(), nivel, arquivo, mensagem));
        if ("ERROR".equals(nivel)) {
            log.error(mensagem);
        } else if ("WARN".equals(nivel)) {
            log.warn(mensagem);
        } else {
            log.info(mensagem);
        }
    }
}
