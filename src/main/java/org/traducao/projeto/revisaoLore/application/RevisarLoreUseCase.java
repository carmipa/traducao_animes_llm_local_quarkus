package org.traducao.projeto.revisaoLore.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.revisaoLore.domain.LogEventoRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.RevisaoLoreRelatorioJson;
import org.traducao.projeto.revisaoLore.domain.ResultadoDeteccaoLore;
import org.traducao.projeto.revisaoLore.domain.ResultadoRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.exceptions.RevisaoLoreException;
import org.traducao.projeto.revisaoLore.infrastructure.RevisaoLoreLogPersistencia;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.traducao.application.ValidadorTraducaoService;
import org.traducao.projeto.traducao.domain.StatusLlm;
import org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda;
import org.traducao.projeto.traducao.domain.legenda.EventoLegenda;
import org.traducao.projeto.traducao.domain.ports.MistralPort;
import org.traducao.projeto.traducao.infrastructure.legenda.EscritorLegendaAss;
import org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaAss;
import org.traducao.projeto.traducao.infrastructure.legenda.MascaradorTags;
import org.traducao.projeto.traducao.presentation.ui.AnsiCores;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class RevisarLoreUseCase {

    private static final Logger log = LoggerFactory.getLogger(RevisarLoreUseCase.class);
    private static final DateTimeFormatter UTC_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private static final int TAMANHO_TRECHO_LOG = 120;

    private final LeitorLegendaAss leitor;
    private final EscritorLegendaAss escritor;
    private final MascaradorTags mascarador;
    private final DetectorTermosLoreService detector;
    private final ValidadorTraducaoService validador;
    private final MistralPort mistralPort;
    private final GerenciadorPromptRevisaoLore gerenciadorPromptRevisaoLore;
    private final TelemetriaService telemetriaService;
    private final RevisaoLoreLogPersistencia logPersistencia;

    private final List<LogEventoRevisaoLore> eventosSessao = new ArrayList<>();
    private Path pastaOriginalRef;
    private Path pastaTraduzidaRef;
    private String contextoIdRef;
    private boolean revisarTodasFalasRef;
    private long inicioSessaoMs;

    public RevisarLoreUseCase(
        LeitorLegendaAss leitor,
        EscritorLegendaAss escritor,
        MascaradorTags mascarador,
        DetectorTermosLoreService detector,
        ValidadorTraducaoService validador,
        MistralPort mistralPort,
        GerenciadorPromptRevisaoLore gerenciadorPromptRevisaoLore,
        TelemetriaService telemetriaService,
        RevisaoLoreLogPersistencia logPersistencia
    ) {
        this.leitor = leitor;
        this.escritor = escritor;
        this.mascarador = mascarador;
        this.detector = detector;
        this.validador = validador;
        this.mistralPort = mistralPort;
        this.gerenciadorPromptRevisaoLore = gerenciadorPromptRevisaoLore;
        this.telemetriaService = telemetriaService;
        this.logPersistencia = logPersistencia;
    }

    public ResultadoRevisaoLore executar(
        Path pastaOriginal,
        Path pastaTraduzida,
        String contextoId,
        boolean revisarTodasFalas
    ) {
        eventosSessao.clear();
        pastaOriginalRef = pastaOriginal;
        pastaTraduzidaRef = pastaTraduzida;
        contextoIdRef = contextoId;
        revisarTodasFalasRef = revisarTodasFalas;
        inicioSessaoMs = System.currentTimeMillis();

        validarEntrada(pastaOriginal, pastaTraduzida, contextoId);

        StatusLlm status = mistralPort.verificarDisponibilidade();
        if (!status.modeloCarregado()) {
            throw new RevisaoLoreException("LLM indisponivel para revisao de lore: " + status.mensagem());
        }

        String nomePromptRevisao = gerenciadorPromptRevisaoLore.obterNome(contextoId);
        String promptSistemaRevisaoLore = gerenciadorPromptRevisaoLore.obterPromptSistema(contextoId);
        long inicioMs = inicioSessaoMs;

        out(AnsiCores.CYAN + "\n=== Revisao de Lore (nomes, locais e terminologia) ===" + AnsiCores.RESET);
        out("Inicio UTC: " + UTC_FORMATTER.format(Instant.now()));
        out("Pasta original (EN): " + pastaOriginal.toAbsolutePath());
        out("Pasta traduzida (PT-BR): " + pastaTraduzida.toAbsolutePath());
        out("Prompt de revisao de lore ativo: " + nomePromptRevisao + " (" + contextoId + ")");
        out(revisarTodasFalas
            ? "Modo: revisar TODAS as falas com dialogo"
            : "Modo: revisar apenas falas sinalizadas pela heuristica");

        int[] arquivosAnalisados = {0};
        int[] arquivosAlterados = {0};
        int[] falasAuditadas = {0};
        int[] falasSinalizadas = {0};
        int[] falasCorrigidas = {0};
        int[] falasSemAlteracao = {0};
        List<String> erros = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(pastaOriginal)) {
            List<Path> originais = stream
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().toLowerCase().endsWith(".ass"))
                .toList();

            out("Arquivos .ass encontrados na pasta original: " + originais.size());

            for (Path arqOriginal : originais) {
                processarArquivo(
                    arqOriginal, pastaTraduzida, revisarTodasFalas, promptSistemaRevisaoLore,
                    arquivosAnalisados, arquivosAlterados, falasAuditadas, falasSinalizadas,
                    falasCorrigidas, falasSemAlteracao, erros
                );
            }
        } catch (IOException e) {
            throw new RevisaoLoreException("Falha ao percorrer pasta original: " + pastaOriginal, e);
        }

        long duracaoMs = System.currentTimeMillis() - inicioMs;

        out("Arquivos analisados: " + arquivosAnalisados[0]);
        out("Arquivos alterados: " + arquivosAlterados[0]);
        out("Falas auditadas: " + falasAuditadas[0]);
        out("Falas sinalizadas (heuristica/LLM): " + falasSinalizadas[0]);
        out("Falas corrigidas: " + falasCorrigidas[0]);
        out("Falas ja conformes: " + falasSemAlteracao[0]);

        if (erros.isEmpty()) {
            out(AnsiCores.GREEN + "\nRevisao de lore concluida com sucesso." + AnsiCores.RESET);
        } else {
            out(AnsiCores.YELLOW + "\nRevisao de lore concluida com " + erros.size() + " aviso(s)/erro(s)." + AnsiCores.RESET);
            for (String erro : erros) {
                out(AnsiCores.YELLOW + "  - " + erro + AnsiCores.RESET);
            }
        }

        String caminhoRelatorioJson = persistirRelatorioJson(
            pastaOriginal, pastaTraduzida, duracaoMs,
            arquivosAnalisados[0], arquivosAlterados[0],
            falasAuditadas[0], falasSinalizadas[0], falasCorrigidas[0],
            falasSemAlteracao[0], erros
        );

        if (caminhoRelatorioJson != null) {
            out("Relatorio JSON (log + telemetria) salvo em: " + caminhoRelatorioJson);
        }

        return new ResultadoRevisaoLore(
            arquivosAnalisados[0],
            arquivosAlterados[0],
            falasAuditadas[0],
            falasSinalizadas[0],
            falasCorrigidas[0],
            falasSemAlteracao[0],
            erros.size(),
            erros,
            caminhoRelatorioJson
        );
    }

    private void validarEntrada(Path pastaOriginal, Path pastaTraduzida, String contextoId) {
        if (contextoId == null || contextoId.isBlank()) {
            throw new RevisaoLoreException(
                "Contexto da obra obrigatorio. Selecione o anime/filme no menu para carregar a lore oficial.");
        }
        if (!gerenciadorPromptRevisaoLore.existePrompt(contextoId)) {
            throw new RevisaoLoreException(
                "Prompt de revisao de lore desconhecido: \"" + contextoId
                    + "\". Recarregue a pagina e selecione uma obra valida.");
        }
        if (!Files.isDirectory(pastaOriginal) || !Files.isDirectory(pastaTraduzida)) {
            throw new RevisaoLoreException(
                "Pastas nao encontradas — esperava original em " + pastaOriginal + " e traduzida em " + pastaTraduzida);
        }
    }

    private void processarArquivo(
        Path arqOriginal,
        Path pastaTraduzida,
        boolean revisarTodasFalas,
        String promptSistemaRevisaoLore,
        int[] arquivosAnalisados,
        int[] arquivosAlterados,
        int[] falasAuditadas,
        int[] falasSinalizadas,
        int[] falasCorrigidas,
        int[] falasSemAlteracao,
        List<String> erros
    ) {
        String nomeOriginal = arqOriginal.getFileName().toString();
        String nomeBase = nomeOriginal.substring(0, nomeOriginal.lastIndexOf('.'));
        Path arqTraduzido = pastaTraduzida.resolve(nomeBase + "_PT-BR.ass");
        if (!Files.exists(arqTraduzido)) {
            arqTraduzido = pastaTraduzida.resolve(nomeBase + "_PTBR.ass");
        }
        if (!Files.exists(arqTraduzido)) {
            arqTraduzido = pastaTraduzida.resolve(nomeOriginal);
        }
        if (!Files.exists(arqTraduzido)) {
            String msg = "Sem par traduzido para: " + nomeOriginal;
            erros.add(msg);
            out(AnsiCores.YELLOW + "  [Pulado] " + msg + AnsiCores.RESET);
            return;
        }

        arquivosAnalisados[0]++;
        out("\n[Arquivo] Analisando: " + arqTraduzido.getFileName());

        try {
            DocumentoLegenda docOriginal = leitor.ler(arqOriginal);
            DocumentoLegenda docTraduzido = leitor.ler(arqTraduzido);
            out("[Arquivo] Eventos original/traduzido: " + docOriginal.eventos().size()
                + "/" + docTraduzido.eventos().size());

            if (docOriginal.eventos().size() != docTraduzido.eventos().size()) {
                String msg = arqTraduzido.getFileName() + ": contagem de eventos divergente ("
                    + docOriginal.eventos().size() + " vs " + docTraduzido.eventos().size() + ") — arquivo pulado.";
                out(AnsiCores.YELLOW + "  [Pulado] " + msg + AnsiCores.RESET);
                erros.add(msg);
                return;
            }

            boolean houveModificacao = false;
            int corrigidasNoArquivo = 0;
            int totalDialogos = contarDialogosAuditaveis(docOriginal, docTraduzido);
            int dialogoAtual = 0;
            List<EventoLegenda> novosEventos = new ArrayList<>(docTraduzido.eventos().size());

            for (int i = 0; i < docOriginal.eventos().size(); i++) {
                EventoLegenda evtOriginal = docOriginal.eventos().get(i);
                EventoLegenda evtTraduzido = docTraduzido.eventos().get(i);

                if (!evtOriginal.isDialogo() || !evtTraduzido.isDialogo()
                    || !evtOriginal.temTexto() || !evtTraduzido.temTexto()) {
                    novosEventos.add(evtTraduzido);
                    continue;
                }

                String textoEn = evtOriginal.texto();
                String textoPt = evtTraduzido.texto();
                falasAuditadas[0]++;
                dialogoAtual++;

                String marcadorFala = "[Fala " + dialogoAtual + "/" + totalDialogos
                    + " | evento " + (i + 1) + "]";
                out(AnsiCores.DIM + marcadorFala + " auditando lore | EN: "
                    + trecho(textoEn) + " | PT: " + trecho(textoPt) + AnsiCores.RESET);

                MascaradorTags.Mascarado mascaraEn = mascarador.mascarar(textoEn);
                MascaradorTags.Mascarado mascaraPt = mascarador.mascarar(textoPt);

                ResultadoDeteccaoLore deteccao = detector.auditar(mascaraEn.texto(), mascaraPt.texto());
                if (!revisarTodasFalas && !deteccao.suspeito()) {
                    falasSemAlteracao[0]++;
                    out(AnsiCores.DIM + marcadorFala + " limpo pela heuristica" + AnsiCores.RESET);
                    novosEventos.add(evtTraduzido);
                    continue;
                }

                falasSinalizadas[0]++;
                out(AnsiCores.YELLOW + marcadorFala + " enviada ao LLM | motivos: "
                    + formatarMotivos(deteccao.motivos()) + AnsiCores.RESET);

                Optional<String> revisadaOpt = mistralPort.revisarLore(
                    promptSistemaRevisaoLore,
                    mascaraEn.texto(),
                    mascaraPt.texto(),
                    deteccao.motivos()
                );

                if (revisadaOpt.isEmpty()) {
                    out(AnsiCores.YELLOW + marcadorFala + " LLM sem resposta valida; mantendo traducao atual" + AnsiCores.RESET);
                    novosEventos.add(evtTraduzido);
                    continue;
                }

                String revisada = mascarador.desmascarar(revisadaOpt.get(), mascaraPt.tags());
                if (revisada.equals(textoPt)) {
                    falasSemAlteracao[0]++;
                    out(AnsiCores.DIM + marcadorFala + " conforme apos revisao LLM" + AnsiCores.RESET);
                    novosEventos.add(evtTraduzido);
                    continue;
                }

                try {
                    validador.validarFala(revisada);
                } catch (Exception e) {
                    log.warn("Revisao de lore descartada (validacao): {}", e.getMessage());
                    out(AnsiCores.YELLOW + marcadorFala + " revisao descartada pela validacao: "
                        + e.getMessage() + AnsiCores.RESET);
                    novosEventos.add(evtTraduzido);
                    continue;
                }

                novosEventos.add(evtTraduzido.comTexto(revisada));
                houveModificacao = true;
                corrigidasNoArquivo++;
                falasCorrigidas[0]++;
                out(AnsiCores.GREEN + marcadorFala + " corrigida | Antes: "
                    + trecho(textoPt) + " | Depois: " + trecho(revisada) + AnsiCores.RESET);
            }

            if (houveModificacao) {
                DocumentoLegenda revisado = new DocumentoLegenda(
                    docTraduzido.cabecalho(),
                    novosEventos,
                    docTraduzido.quebraDeLinha(),
                    docTraduzido.comBom()
                );
                escritor.escrever(arqTraduzido, revisado);
                arquivosAlterados[0]++;
                out(AnsiCores.GREEN + "  [Revisado] " + arqTraduzido.getFileName()
                    + " (" + corrigidasNoArquivo + " fala(s) corrigida(s))" + AnsiCores.RESET);
            } else {
                out(AnsiCores.DIM + "  [OK]     " + arqTraduzido.getFileName() + " (lore conforme)" + AnsiCores.RESET);
            }

        } catch (Exception e) {
            String msg = "Falha ao revisar lore em " + arqTraduzido.getFileName() + ": " + e.getMessage();
            log.error(msg, e);
            out(AnsiCores.RED + "  [Erro] " + msg + AnsiCores.RESET);
            erros.add(msg);
        }
    }

    private String persistirRelatorioJson(
        Path pastaOriginal,
        Path pastaTraduzida,
        long duracaoMs,
        int arquivosAnalisados,
        int arquivosAlterados,
        int falasAuditadas,
        int falasSinalizadas,
        int falasCorrigidas,
        int falasSemAlteracao,
        List<String> erros
    ) {
        String detalhe = pastaTraduzida.toAbsolutePath()
            + " | promptRevisaoLore=" + gerenciadorPromptRevisaoLore.obterNome(contextoIdRef);

        OperacaoTelemetria operacao = TelemetriaService.criarOperacao(
            "Revisao de Lore (.ass LLM)",
            detalhe,
            duracaoMs,
            arquivosAnalisados,
            falasSinalizadas,
            falasCorrigidas
        );

        RevisaoLoreRelatorioJson relatorio = new RevisaoLoreRelatorioJson(
            "revisao_lore",
            operacao,
            new RevisaoLoreRelatorioJson.ContextoObra(
                contextoIdRef,
                gerenciadorPromptRevisaoLore.obterNome(contextoIdRef)
            ),
            new RevisaoLoreRelatorioJson.PastasOperacao(
                pastaOriginal.toAbsolutePath().toString(),
                pastaTraduzida.toAbsolutePath().toString()
            ),
            revisarTodasFalasRef ? "todas_as_falas" : "apenas_sinalizadas",
            new RevisaoLoreRelatorioJson.MetricasRevisaoLore(
                duracaoMs,
                formatarDuracaoMs(duracaoMs),
                arquivosAnalisados,
                arquivosAlterados,
                falasAuditadas,
                falasSinalizadas,
                falasCorrigidas,
                falasSemAlteracao,
                erros.size()
            ),
            List.copyOf(erros),
            List.copyOf(eventosSessao)
        );

        try {
            Path arquivo = logPersistencia.salvarRelatorioJson(pastaTraduzida, relatorio);
            telemetriaService.registrarOperacao(operacao);
            telemetriaService.salvar(TelemetriaService.resolverPastaRelatorios(pastaTraduzida));
            return arquivo.toString();
        } catch (IOException e) {
            log.warn("Falha ao salvar relatorio JSON da revisao de lore: {}", e.getMessage());
            out(AnsiCores.YELLOW + "Aviso: nao foi possivel salvar o relatorio JSON em disco." + AnsiCores.RESET);
            telemetriaService.registrarOperacao(operacao);
            return null;
        }
    }

    private String formatarDuracaoMs(long ms) {
        long segundos = ms / 1000;
        return segundos >= 60 ? (segundos / 60) + "min " + (segundos % 60) + "s" : segundos + "s";
    }

    private void out(String msg) {
        String limpo = removerAnsi(msg);
        String prefixo = prefixoLog();
        String mensagemComPrefixo = prefixo + " " + msg;
        String limpoComPrefixo = prefixo + " " + limpo;
        System.out.println(mensagemComPrefixo);
        log.info(limpoComPrefixo);
        eventosSessao.add(new LogEventoRevisaoLore(
            Instant.now().toString(),
            inferirNivel(limpoComPrefixo),
            limpoComPrefixo
        ));
    }

    private String prefixoLog() {
        long decorridoMs = Math.max(0, System.currentTimeMillis() - inicioSessaoMs);
        return "[UTC " + UTC_FORMATTER.format(Instant.now()) + " | +" + formatarDuracaoDetalhada(decorridoMs) + "]";
    }

    private String formatarDuracaoDetalhada(long ms) {
        long totalSegundos = ms / 1000;
        long minutos = totalSegundos / 60;
        long segundos = totalSegundos % 60;
        long millis = ms % 1000;
        if (minutos > 0) {
            return "%02d:%02d.%03d".formatted(minutos, segundos, millis);
        }
        return "%02d.%03ds".formatted(segundos, millis);
    }

    private int contarDialogosAuditaveis(DocumentoLegenda docOriginal, DocumentoLegenda docTraduzido) {
        int total = 0;
        int limite = Math.min(docOriginal.eventos().size(), docTraduzido.eventos().size());
        for (int i = 0; i < limite; i++) {
            EventoLegenda original = docOriginal.eventos().get(i);
            EventoLegenda traduzido = docTraduzido.eventos().get(i);
            if (original.isDialogo() && traduzido.isDialogo()
                && original.temTexto() && traduzido.temTexto()) {
                total++;
            }
        }
        return total;
    }

    private String formatarMotivos(List<String> motivos) {
        if (motivos == null || motivos.isEmpty()) {
            return "revisao preventiva";
        }
        return String.join(" | ", motivos.stream().map(this::trecho).toList());
    }

    private String trecho(String texto) {
        if (texto == null || texto.isBlank()) {
            return "(vazio)";
        }
        String normalizado = texto
            .replace("\\N", " ")
            .replace("\\n", " ")
            .replaceAll("\\s+", " ")
            .strip();
        if (normalizado.length() <= TAMANHO_TRECHO_LOG) {
            return normalizado;
        }
        return normalizado.substring(0, TAMANHO_TRECHO_LOG - 3).stripTrailing() + "...";
    }

    private static String inferirNivel(String mensagem) {
        if (mensagem.contains("[Erro]") || mensagem.contains("Falha")) {
            return "ERROR";
        }
        if (mensagem.contains("[Pulado]") || mensagem.contains("Aviso:")) {
            return "WARN";
        }
        if (mensagem.contains("[Revisado]") || mensagem.contains("concluida com sucesso")) {
            return "SUCCESS";
        }
        return "INFO";
    }

    private static String removerAnsi(String texto) {
        return texto.replaceAll("\u001B\\[[0-9;]*m", "");
    }
}
