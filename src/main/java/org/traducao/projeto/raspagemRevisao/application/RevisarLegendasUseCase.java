package org.traducao.projeto.raspagemRevisao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.correcaoLegendas.application.SanitizadorTagsService;
import org.traducao.projeto.raspagemCorrecao.infrastructure.GoogleTranslateScraper;
import org.traducao.projeto.raspagemCorrecao.infrastructure.ResultadoRaspagem;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoDeteccaoConcordancia;
import org.traducao.projeto.raspagemRevisao.domain.exceptions.RaspagemRevisaoException;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.traducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.traducao.application.ValidadorTraducaoService;
import org.traducao.projeto.traducao.domain.exceptions.AlucinacaoDetectadaException;
import org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda;
import org.traducao.projeto.traducao.domain.legenda.EventoLegenda;
import org.traducao.projeto.traducao.domain.ports.MistralPort;
import org.traducao.projeto.traducao.infrastructure.cache.EntradaCache;
import org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto;
import org.traducao.projeto.traducao.infrastructure.legenda.EscritorLegendaAss;
import org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaAss;
import org.traducao.projeto.traducao.infrastructure.legenda.MascaradorTags;
import org.traducao.projeto.traducao.presentation.ui.AnsiCores;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class RevisarLegendasUseCase {

    public enum ModoRevisaoLegendas {
        GOOGLE,
        LLM_CONCORDANCIA
    }

    private static final Set<String> EXTENSOES = Set.of(".ass", ".ssa");
    private static final long PAUSA_GOOGLE_MS = 400;
    private static final Pattern CODIGO_EPISODIO = Pattern.compile("(?i)(S\\d{1,2}E\\d{1,3})");
    private static final Pattern SUFIXO_PTBR_TRACK = Pattern.compile("(?i)_PT-?BR(_Track\\d+)?$");
    private static final DateTimeFormatter TS_BACKUP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final LeitorLegendaAss leitor;
    private final EscritorLegendaAss escritor;
    private final GoogleTranslateScraper googleScraper;
    private final AuditorProblemasLegendaService auditor;
    private final ValidadorTraducaoService validador;
    private final LeitorCacheReferenciaService leitorCache;
    private final SincronizadorLegendaCacheService sincronizadorCache;
    private final MistralPort mistralPort;
    private final MascaradorTags mascaradorTags;
    private final GerenciadorContexto gerenciadorContexto;
    private final TelemetriaService telemetriaService;
    private final SanitizadorTagsService sanitizadorTags;
    private final TradutorProperties propriedades;
    private final DetectorEfeitoKaraokeService detectorKaraoke;
    private final ProtecaoLegendaAssService protecaoAss;

    /**
     * PROPÓSITO DE NEGÓCIO: compõe a revisão final de legendas com leitura de
     * cache versionado, validação linguística, proteção ASS e persistência segura.
     *
     * <p>INVARIANTES DO DOMÍNIO: todas as dependências usam o mesmo catálogo de
     * contexto e o cache é aberto pela porta canônica de manutenção.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência obrigatória ausente impede a
     * construção do serviço pelo contêiner de injeção.
     */
    public RevisarLegendasUseCase(
        LeitorLegendaAss leitor,
        EscritorLegendaAss escritor,
        GoogleTranslateScraper googleScraper,
        AuditorProblemasLegendaService auditor,
        ValidadorTraducaoService validador,
        LeitorCacheReferenciaService leitorCache,
        SincronizadorLegendaCacheService sincronizadorCache,
        MistralPort mistralPort,
        MascaradorTags mascaradorTags,
        GerenciadorContexto gerenciadorContexto,
        TelemetriaService telemetriaService,
        SanitizadorTagsService sanitizadorTags,
        TradutorProperties propriedades,
        DetectorEfeitoKaraokeService detectorKaraoke,
        ProtecaoLegendaAssService protecaoAss
    ) {
        this.leitor = leitor;
        this.escritor = escritor;
        this.googleScraper = googleScraper;
        this.auditor = auditor;
        this.validador = validador;
        this.leitorCache = leitorCache;
        this.sincronizadorCache = sincronizadorCache;
        this.mistralPort = mistralPort;
        this.mascaradorTags = mascaradorTags;
        this.gerenciadorContexto = gerenciadorContexto;
        this.telemetriaService = telemetriaService;
        this.sanitizadorTags = sanitizadorTags;
        this.propriedades = propriedades;
        this.detectorKaraoke = detectorKaraoke;
        this.protecaoAss = protecaoAss;
    }

    /**
     * Valida a pasta informada antes de iniciar a revisão (API/CLI).
     * Retorna mensagem de erro quando a pasta não contém legendas .ass/.ssa.
     */
    public Optional<String> validarPastaEntrada(Path pasta) {
        if (pasta == null || pasta.toString().isBlank()) {
            return Optional.of("Pasta com legendas traduzidas em português (.ass) é obrigatória.");
        }
        if (!Files.isDirectory(pasta)) {
            return Optional.of("Pasta não encontrada: " + pasta.toAbsolutePath());
        }

        try (Stream<Path> stream = Files.list(pasta)) {
            List<Path> arquivos = stream.filter(Files::isRegularFile).toList();
            if (arquivos.stream().anyMatch(this::temExtensaoSuportada)) {
                return Optional.empty();
            }

            long cacheJson = arquivos.stream()
                .filter(p -> p.getFileName().toString().endsWith(".cache.json"))
                .count();
            String abs = pasta.toAbsolutePath().toString().replace('\\', '/').toLowerCase();
            boolean pareceCache = cacheJson > 0
                || abs.contains("/cache/")
                || abs.endsWith("/cache");

            if (pareceCache) {
                return Optional.of(
                    "Esta pasta parece ser de CACHE ("
                        + cacheJson + " arquivo(s) .cache.json, nenhum .ass/.ssa). "
                        + "Informe a pasta com os arquivos de legenda traduzidos (.ass), por exemplo: "
                        + "E:\\animes\\DANMACHI\\temporada_5\\legendas_extraidas_ass.");
            }
            return Optional.of(
                "Nenhum arquivo .ass/.ssa encontrado em: " + pasta.toAbsolutePath());
        } catch (IOException e) {
            return Optional.of("Erro ao ler pasta: " + e.getMessage());
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mantém o contrato histórico da revisão Google e
     * delega ao fluxo completo com sincronização prévia do cache corrigido.
     *
     * <p>INVARIANTES DO DOMÍNIO: somente arquivos PT-BR suportados entram no
     * lote; a fila respeita interrupção e toda sobrescrita cria backup.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: pasta inválida devolve resultado vazio;
     * falha de listagem lança exceção de domínio sem alterar legendas.
     *
     * @param pastaLegendasPt pasta com arquivos .ass/.ssa já traduzidos
     * @param pastaLegendasEn pasta opcional com originais em inglês
     * @param pastaCache pasta de cache; padrão {@code cache}
     * @param pastaSaida pasta opcional de saída; padrão sobrescreve PT com backup
     */
    public ResultadoRevisaoLegendas executar(
        Path pastaLegendasPt,
        Path pastaLegendasEn,
        Path pastaCache,
        Path pastaSaida
    ) {
        return executar(pastaLegendasPt, pastaLegendasEn, pastaCache, pastaSaida,
            ModoRevisaoLegendas.GOOGLE, null);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: executa a revisão em lote no modo Google ou LLM,
     * incluindo a sincronização prévia das correções confirmadas no cache.
     *
     * <p>INVARIANTES DO DOMÍNIO: somente arquivos PT-BR suportados entram no
     * lote; o modo Google não corrige concordância reservada à lore local.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: pasta inválida devolve resultado vazio;
     * falha de listagem lança exceção de domínio sem alterar legendas.
     */
    public ResultadoRevisaoLegendas executar(
        Path pastaLegendasPt,
        Path pastaLegendasEn,
        Path pastaCache,
        Path pastaSaida,
        ModoRevisaoLegendas modo,
        String contextoId
    ) {
        long inicioMs = System.currentTimeMillis();
        if (modo == ModoRevisaoLegendas.LLM_CONCORDANCIA) {
            out("Iniciando revisão de concordância PT-BR (LLM) em legendas: "
                + pastaLegendasPt.toAbsolutePath());
            aplicarContextoLlm(contextoId);
        } else {
            out("Iniciando revisão de legendas traduzidas em: " + pastaLegendasPt.toAbsolutePath());
        }

        if (!Files.isDirectory(pastaLegendasPt)) {
            out(AnsiCores.RED + "Erro: pasta de legendas traduzidas inválida." + AnsiCores.RESET);
            return new ResultadoRevisaoLegendas(0, 0);
        }

        Path pastaEn = pastaLegendasEn != null ? pastaLegendasEn : pastaLegendasPt;
        Path cacheDir = pastaCache != null ? pastaCache : Path.of("cache");
        Path saidaDir = pastaSaida != null ? pastaSaida : pastaLegendasPt;
        Path pastaBackup = Path.of("backups", "revisao-legendas",
            "revisao_" + LocalDateTime.now().format(TS_BACKUP)).toAbsolutePath().normalize();

        int[] arquivosProcessados = {0};
        int[] falasCorrigidas = {0};
        int[] falasComProblema = {0};
        int[] falasAuditadas = {0};
        int[] falasSemOriginal = {0};

        try (Stream<Path> stream = Files.list(pastaLegendasPt)) {
            List<Path> arquivos = stream
                .filter(Files::isRegularFile)
                .filter(this::temExtensaoSuportada)
                .filter(this::eLegendaTraduzida)
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .toList();

            if (arquivos.isEmpty()) {
                Optional<String> erro = validarPastaEntrada(pastaLegendasPt);
                out(AnsiCores.YELLOW + erro.orElse("Nenhum arquivo .ass/.ssa traduzido encontrado na pasta.")
                    + AnsiCores.RESET);
                registrarTelemetria(pastaLegendasPt, inicioMs, 0, 0, 0, 0, 0, modo);
                return new ResultadoRevisaoLegendas(0, 0);
            }

            out("Originais EN: .ass em " + pastaEn.toAbsolutePath()
                + " (se existir) + cache/ em " + cacheDir.toAbsolutePath());

            for (Path arquivoPt : arquivos) {
                // Parada cooperativa (botão "Parar" da UI): arquivos já
                // revisados ficaram salvos; os restantes não são tocados.
                if (Thread.currentThread().isInterrupted()) {
                    out(AnsiCores.YELLOW + "Revisão interrompida pelo usuário — "
                        + "arquivos restantes não foram processados." + AnsiCores.RESET);
                    break;
                }
                processarArquivo(
                    arquivoPt, pastaEn, cacheDir, saidaDir, pastaBackup, modo,
                    arquivosProcessados, falasCorrigidas, falasComProblema,
                    falasAuditadas, falasSemOriginal);
            }
        } catch (IOException e) {
            out(AnsiCores.RED + "Erro ao listar legendas: " + e.getMessage() + AnsiCores.RESET);
            throw new RaspagemRevisaoException("Falha ao listar legendas em: " + pastaLegendasPt, e);
        }

        out("Arquivos analisados: " + arquivosProcessados[0]);
        out("Falas auditadas: " + falasAuditadas[0]);
        out("Falas sem original EN (ignoradas): " + falasSemOriginal[0]);
        out("Falas com problemas detectados: " + falasComProblema[0]);
        if (modo == ModoRevisaoLegendas.LLM_CONCORDANCIA) {
            out("Falas corrigidas via LLM e salvas: " + falasCorrigidas[0]);
        } else {
            out("Falas corrigidas via Google e salvas: " + falasCorrigidas[0]);
        }
        registrarTelemetria(pastaLegendasPt, inicioMs, arquivosProcessados[0], falasComProblema[0],
            falasCorrigidas[0], falasAuditadas[0], falasSemOriginal[0], modo);
        return new ResultadoRevisaoLegendas(arquivosProcessados[0], falasCorrigidas[0]);
    }

    private void aplicarContextoLlm(String contextoId) {
        if (contextoId != null && !contextoId.isBlank()) {
            if (gerenciadorContexto.existeContexto(contextoId)) {
                gerenciadorContexto.definirContextoAtivo(contextoId);
                out(AnsiCores.CYAN + "Contexto ativo: "
                    + gerenciadorContexto.obterNomeContextoAtivo() + AnsiCores.RESET);
            } else {
                out(AnsiCores.YELLOW + "Contexto desconhecido \""
                    + contextoId + "\" — usando contexto padrão." + AnsiCores.RESET);
            }
        }
    }

    private void registrarTelemetria(
        Path pastaLegendasPt,
        long inicioMs,
        int arquivos,
        int problemas,
        int corrigidas,
        int auditadas,
        int semOriginal,
        ModoRevisaoLegendas modo
    ) {
        long duracaoMs = System.currentTimeMillis() - inicioMs;
        boolean llm = modo == ModoRevisaoLegendas.LLM_CONCORDANCIA;
        String nomeOperacao = llm
            ? "Revisão Concordância (.ass LLM)"
            : "Revisão Legendas (.ass Google)";
        OperacaoTelemetria operacao = TelemetriaService.criarOperacao(
            nomeOperacao,
            pastaLegendasPt.toAbsolutePath().toString(),
            duracaoMs,
            arquivos,
            problemas,
            corrigidas
        );
        String relatorio = llm ? """
            REVISÃO DE CONCORDÂNCIA PT-BR (.ass via LLM)
            ============================================
            Pasta: %s
            Duração: %s
            Arquivos analisados: %d
            Falas auditadas: %d
            Falas sem original EN (ignoradas): %d
            Problemas detectados: %d
            Falas corrigidas via LLM: %d
            """.formatted(
            pastaLegendasPt.toAbsolutePath(),
            formatarDuracaoMs(duracaoMs),
            arquivos,
            auditadas,
            semOriginal,
            problemas,
            corrigidas
        ) : """
            REVISÃO DE LEGENDAS (.ass)
            ==========================
            Pasta: %s
            Duração: %s
            Arquivos analisados: %d
            Falas auditadas: %d
            Falas sem original EN (ignoradas): %d
            Problemas detectados: %d
            Falas corrigidas via Google: %d
            """.formatted(
            pastaLegendasPt.toAbsolutePath(),
            formatarDuracaoMs(duracaoMs),
            arquivos,
            auditadas,
            semOriginal,
            problemas,
            corrigidas
        );
        telemetriaService.finalizarOperacao(
            operacao, pastaLegendasPt,
            llm ? "revisao_concordancia_legendas" : "revisao_legendas",
            relatorio);
        out("Relatório salvo em: " + TelemetriaService.resolverPastaRelatorios(pastaLegendasPt));
    }

    private String formatarDuracaoMs(long ms) {
        long segundos = ms / 1000;
        return segundos >= 60 ? (segundos / 60) + "min " + (segundos % 60) + "s" : segundos + "s";
    }

    private void out(String mensagem) {
        System.out.println(mensagem);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: sincroniza uma legenda com o cache corrigido mais
     * recente e aplica a revisão linguística correspondente ao modo selecionado.
     *
     * <p>INVARIANTES DO DOMÍNIO: cache vazio nunca apaga fala; cache mais antigo
     * nunca sobrescreve revisão posterior; qualquer gravação sobre a origem cria
     * backup e preserva tempos, estilos e estrutura ASS.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: exceções de leitura/escrita interrompem
     * o arquivo atual sem produzir uma substituição parcial.
     */
    private void processarArquivo(
        Path arquivoPt,
        Path pastaLegendasEn,
        Path cacheDir,
        Path saidaDir,
        Path pastaBackup,
        ModoRevisaoLegendas modo,
        int[] totalArquivos,
        int[] totalCorrigidas,
        int[] totalProblemas,
        int[] totalAuditadas,
        int[] totalSemOriginal
    ) {
        totalArquivos[0]++;
        out("\nAnalisando legenda: " + arquivoPt.getFileName());

        DocumentoLegenda documentoPt = leitor.ler(arquivoPt);
        Path arquivoEn = resolverArquivoOriginal(arquivoPt, pastaLegendasEn);
        Map<Integer, String> originaisPorIndice = carregarOriginaisDeLegenda(arquivoEn);

        Path cachePath = resolverArquivoCache(arquivoPt, cacheDir);
        List<EntradaCache> entradasCache = carregarEntradasCache(cachePath);
        Map<String, String> originalPorTraduzido = indexarOriginalPorTraduzido(entradasCache);
        for (EntradaCache entrada : entradasCache) {
            if (entrada.original() != null && !entrada.original().isBlank()) {
                originaisPorIndice.putIfAbsent(entrada.indice(), entrada.original());
            }
        }

        if (!entradasCache.isEmpty()) {
            out("  Cache carregado: " + cachePath.getFileName()
                + " (" + entradasCache.size() + " entradas EN)");
        } else {
            out(AnsiCores.YELLOW + "  Aviso: cache EN não encontrado. Procurado em: "
                + cacheDir.toAbsolutePath()
                + " (esperado algo como " + cachePath.getFileName() + ")"
                + AnsiCores.RESET);
        }
        if (Files.isRegularFile(arquivoEn) && !originaisPorIndice.isEmpty()) {
            out("  Legenda .ass EN: " + arquivoEn.getFileName());
        }

        List<EventoLegenda> eventosAtualizados = new ArrayList<>();
        Map<String, String> cacheRevisaoMasc = new HashMap<>();
        Set<String> revisoesSemAlteracao = new LinkedHashSet<>();
        int corrigidasNesteArquivo = 0;
        boolean sincronizarCache = cacheMaisNovoQueLegenda(cachePath, arquivoPt);
        SincronizadorLegendaCacheService.Resultado sincronizacao = sincronizadorCache.sincronizar(
            documentoPt, entradasCache, sincronizarCache);
        documentoPt = sincronizacao.documento();
        int sincronizadasNesteArquivo = sincronizacao.total();
        int problemasNesteArquivo = 0;
        int falasAuditadas = 0;
        int falasSemOriginal = 0;
        boolean modificado = sincronizadasNesteArquivo > 0;
        if (sincronizarCache) {
            out(AnsiCores.CYAN + "  Cache corrigido é mais novo que a legenda; "
                + "sincronizando traduções antes da revisão." + AnsiCores.RESET);
            for (Integer indice : sincronizacao.indicesSincronizados()) {
                out("  [CACHE] Evento " + indice + " sincronizado com a correção da Opção 5.");
            }
        }

        boolean interrompido = false;
        for (EventoLegenda evento : documentoPt.eventos()) {
            // Parada cooperativa no meio do arquivo: as falas restantes entram
            // sem alteração e o que já foi corrigido é gravado normalmente.
            if (interrompido || Thread.currentThread().isInterrupted()) {
                if (!interrompido) {
                    out("  " + AnsiCores.YELLOW
                        + "[STOP] Interrompido pelo usuário — falas restantes mantidas como estão."
                        + AnsiCores.RESET);
                    interrompido = true;
                }
                eventosAtualizados.add(evento);
                continue;
            }
            if (!evento.isDialogo() || evento.texto() == null) {
                eventosAtualizados.add(evento);
                continue;
            }

            if (evento.texto().isBlank()) {
                eventosAtualizados.add(evento);
                continue;
            }

            if (deveIgnorarAuditoria(evento, evento.texto())) {
                eventosAtualizados.add(evento);
                continue;
            }

            // Localiza o original EN ANTES da correção de karaokê: a busca por
            // texto traduzido usa o texto como está no cache (pré-correção), e o
            // original serve de referência para preservar comentários {...}
            // legítimos em vez de escapá-los como alucinação.
            String textoNormalizado = evento.texto();
            String originalEn = originaisPorIndice.get(evento.indice());
            if (originalEn == null || originalEn.isBlank()) {
                originalEn = originalPorTraduzido.get(normalizarTexto(textoNormalizado));
            }
            boolean temOriginalEn = originalEn != null && !originalEn.isBlank();

            String textoCorrigidoKaraoke = sanitizadorTags.escaparChavesInvalidas(textoNormalizado, originalEn);
            if (!textoNormalizado.equals(textoCorrigidoKaraoke)) {
                evento = evento.comTexto(textoCorrigidoKaraoke);
                modificado = true;
                corrigidasNesteArquivo++;
                out("  -> Karaoke corrigido na linha " + evento.indice() + ":");
                out("     De : " + textoNormalizado);
                out("     Para: " + textoCorrigidoKaraoke);
            }

            String traducaoAtual = evento.texto();
            if (!temOriginalEn) {
                falasSemOriginal++;
                totalSemOriginal[0]++;
                if (modo != ModoRevisaoLegendas.LLM_CONCORDANCIA) {
                    eventosAtualizados.add(evento);
                    continue;
                }
            }

            falasAuditadas++;
            totalAuditadas[0]++;

            ResultadoDeteccaoConcordancia auditoria = auditor.auditar(originalEn, traducaoAtual);
            if (!auditoria.suspeito()) {
                eventosAtualizados.add(evento);
                continue;
            }

            problemasNesteArquivo++;
            totalProblemas[0]++;

            out("  -> Linha " + evento.indice() + " [" + evento.estilo() + "]:");
            out("     EN: " + AnsiCores.YELLOW + originalEn + AnsiCores.RESET);
            out("     PT: " + AnsiCores.YELLOW + traducaoAtual + AnsiCores.RESET);
            auditoria.motivos().forEach(m ->
                out("     " + AnsiCores.DIM + "• " + m + AnsiCores.RESET));

            String textoMascOriginal = temOriginalEn
                ? mascaradorTags.mascarar(originalEn).texto()
                : null;
            if (textoMascOriginal != null && revisoesSemAlteracao.contains(textoMascOriginal)) {
                eventosAtualizados.add(evento);
                continue;
            }
            if (textoMascOriginal != null && cacheRevisaoMasc.containsKey(textoMascOriginal)) {
                String respostaMascCorrigida = cacheRevisaoMasc.get(textoMascOriginal);
                MascaradorTags.Mascarado mascTraducaoAtual = mascaradorTags.mascarar(traducaoAtual);
                String novaTraducaoCache;
                try {
                    novaTraducaoCache = mascaradorTags.desmascarar(respostaMascCorrigida, mascTraducaoAtual.tags());
                } catch (AlucinacaoDetectadaException e) {
                    out("  " + AnsiCores.YELLOW
                        + "Cache local ignorado na linha " + evento.indice()
                        + ": marcadores de tags incompatíveis com a tradução atual."
                        + AnsiCores.RESET);
                    eventosAtualizados.add(evento);
                    continue;
                }

                if (novaTraducaoCache.equals(traducaoAtual)
                    || !correcaoEhSegura(originalEn, traducaoAtual, novaTraducaoCache, auditoria)) {
                    revisoesSemAlteracao.add(textoMascOriginal);
                    eventosAtualizados.add(evento);
                    continue;
                }

                out("  -> Linha " + evento.indice() + " [" + evento.estilo() + "] (Reutilizando correção do cache local):");
                out("     EN: " + AnsiCores.YELLOW + originalEn + AnsiCores.RESET);
                out("     PT: " + AnsiCores.YELLOW + traducaoAtual + AnsiCores.RESET);
                out("     PT corrigido: " + AnsiCores.GREEN + novaTraducaoCache + AnsiCores.RESET);

                eventosAtualizados.add(evento.comTexto(novaTraducaoCache));
                corrigidasNesteArquivo++;
                modificado = true;
                continue;
            }

            String novaTraducao;
            if (modo == ModoRevisaoLegendas.LLM_CONCORDANCIA) {
                Optional<String> revisadoOpt = tentarRevisarConcordancia(
                    originalEn, traducaoAtual, auditoria.motivos());
                if (revisadoOpt.isEmpty()) {
                    out("     " + AnsiCores.RED
                        + "Revisão não aplicada (LLM indisponível ou resposta inválida)."
                        + AnsiCores.RESET);
                    eventosAtualizados.add(evento);
                    continue;
                }
                novaTraducao = revisadoOpt.get();
                if (novaTraducao.equals(traducaoAtual)) {
                    out("     " + AnsiCores.DIM + "LLM manteve o texto original." + AnsiCores.RESET);
                    registrarSemAlteracao(textoMascOriginal, revisoesSemAlteracao);
                    eventosAtualizados.add(evento);
                    continue;
                }
            } else {
                if (!exigeRetraducao(auditoria)) {
                    out("     " + AnsiCores.DIM
                        + "Google não acionado: problema reservado à revisão LLM." + AnsiCores.RESET);
                    registrarSemAlteracao(textoMascOriginal, revisoesSemAlteracao);
                    eventosAtualizados.add(evento);
                    continue;
                }
                ResultadoRaspagem resultadoGoogle = googleScraper.traduzir(originalEn);
                pausaGoogle();

                if (!resultadoGoogle.sucesso() || resultadoGoogle.texto().equals(traducaoAtual)) {
                    out("     " + AnsiCores.DIM + "Google sem alteração aplicável ("
                        + resultadoGoogle.status() + "); mantido." + AnsiCores.RESET);
                    registrarSemAlteracao(textoMascOriginal, revisoesSemAlteracao);
                    eventosAtualizados.add(evento);
                    continue;
                }
                novaTraducao = resultadoGoogle.texto();
            }

            if (!correcaoEhSegura(originalEn, traducaoAtual, novaTraducao, auditoria)) {
                String motivo = modo == ModoRevisaoLegendas.LLM_CONCORDANCIA
                    ? "Correção descartada: resposta LLM inválida ou sem melhoria."
                    : "Correção descartada: resposta Google inválida ou sem melhoria.";
                out("     " + AnsiCores.YELLOW + motivo + AnsiCores.RESET);
                registrarSemAlteracao(textoMascOriginal, revisoesSemAlteracao);
                eventosAtualizados.add(evento);
                continue;
            }

            out("     PT corrigido: " + AnsiCores.GREEN + novaTraducao + AnsiCores.RESET);
            eventosAtualizados.add(evento.comTexto(novaTraducao));
            corrigidasNesteArquivo++;
            modificado = true;

            MascaradorTags.Mascarado mascNova = mascaradorTags.mascarar(novaTraducao);
            if (textoMascOriginal != null) {
                cacheRevisaoMasc.put(textoMascOriginal, mascNova.texto());
            }
        }

        if (modificado) {
            DocumentoLegenda revisado = new DocumentoLegenda(
                documentoPt.cabecalho(),
                eventosAtualizados,
                documentoPt.quebraDeLinha(),
                documentoPt.comBom()
            );
            Path destino = saidaDir.resolve(arquivoPt.getFileName());
            Path backup = criarBackupSeSobrescrever(arquivoPt, destino, pastaBackup);
            escritor.escrever(destino, revisado);
            totalCorrigidas[0] += corrigidasNesteArquivo;
            out(AnsiCores.GREEN + "  [OK] sincronizadas=" + sincronizadasNesteArquivo
                + ", revisadas=" + corrigidasNesteArquivo
                + ". Salvo em: " + destino.getFileName() + AnsiCores.RESET);
            if (backup != null) {
                out(AnsiCores.CYAN + "  Backup anterior: " + backup + AnsiCores.RESET);
            }
        } else if (problemasNesteArquivo > 0) {
            out(AnsiCores.YELLOW + "  Problemas encontrados, mas nenhuma correção aplicada."
                + AnsiCores.RESET);
        } else if (falasAuditadas == 0 && falasSemOriginal > 0) {
            out(AnsiCores.YELLOW + "  -> Nenhuma fala auditada ("
                + falasSemOriginal + " ignoradas por falta de original EN)." + AnsiCores.RESET);
        } else {
            out("  -> Nenhum problema detectado neste arquivo ("
                + falasAuditadas + " falas auditadas).");
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: decide se uma resposta externa pode substituir a
     * fala atual sem introduzir alucinação ou piorar a auditoria.
     *
     * <p>INVARIANTES DO DOMÍNIO: texto vazio, suspeita estrutural e resultado com
     * quantidade igual/maior de problemas são sempre rejeitados.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: validação que lança exceção retorna
     * {@code false} e mantém a legenda anterior.
     */
    private boolean correcaoEhSegura(
        String original,
        String traducaoAtual,
        String candidata,
        ResultadoDeteccaoConcordancia auditoriaAnterior
    ) {
        if (candidata == null || candidata.isBlank() || candidata.equals(traducaoAtual)) return false;
        try {
            validador.validarFala(candidata);
            if (protecaoAss.respostaSuspeita(original, candidata)) return false;
        } catch (AlucinacaoDetectadaException e) {
            return false;
        }
        ResultadoDeteccaoConcordancia posterior = auditor.auditar(original, candidata);
        return !posterior.suspeito()
            || posterior.motivos().size() < auditoriaAnterior.motivos().size();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: restringe o Google a falhas objetivas de tradução,
     * deixando concordância e estilo para o LLM local com lore.
     *
     * <p>INVARIANTES DO DOMÍNIO: gênero, pronome e tratamento isolados nunca
     * provocam retradução completa pelo Google.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: motivo desconhecido retorna falso e a
     * fala é preservada para inspeção segura.
     */
    private boolean exigeRetraducao(ResultadoDeteccaoConcordancia auditoria) {
        return auditoria.motivos().stream().anyMatch(motivo ->
            motivo.contains("Resíduo gringo")
                || motivo.contains("Fala não traduzida")
                || motivo.contains("Idioma incorreto")
                || motivo.contains("Preâmbulo")
                || motivo.contains("Marcador de erro de tradução"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: registra que uma origem já foi analisada e não teve
     * correção aplicável sem usar o próprio inglês como sentinela textual.
     *
     * <p>INVARIANTES DO DOMÍNIO: chave nula — caso de fala sem original — nunca
     * entra no conjunto compartilhado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: chave ausente não produz efeito.
     */
    private void registrarSemAlteracao(String chave, Set<String> revisoesSemAlteracao) {
        if (chave != null && !chave.isBlank()) revisoesSemAlteracao.add(chave);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: ativa a ponte 5→6 somente quando a manutenção do
     * cache ocorreu depois da geração da legenda.
     *
     * <p>INVARIANTES DO DOMÍNIO: arquivo inexistente ou empate de data não
     * autoriza sobrescrita da legenda.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: erro de metadados desativa a
     * sincronização e preserva o ASS atual.
     */
    private boolean cacheMaisNovoQueLegenda(Path cache, Path legenda) {
        if (!Files.isRegularFile(cache) || !Files.isRegularFile(legenda)) return false;
        try {
            return Files.getLastModifiedTime(cache).compareTo(Files.getLastModifiedTime(legenda)) > 0;
        } catch (IOException e) {
            out(AnsiCores.YELLOW + "  Aviso: não foi possível comparar cache e legenda; "
                + "sincronização automática desativada." + AnsiCores.RESET);
            return false;
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: preserva a legenda anterior antes de a Opção 6
     * sobrescrever o arquivo de trabalho.
     *
     * <p>INVARIANTES DO DOMÍNIO: backup só é necessário quando origem e destino
     * são o mesmo arquivo; a primeira fotografia da sessão nunca é substituída.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lança exceção de domínio e bloqueia a
     * escrita da nova legenda.
     */
    private Path criarBackupSeSobrescrever(Path origem, Path destino, Path pastaBackup) {
        Path origemAbs = origem.toAbsolutePath().normalize();
        Path destinoAbs = destino.toAbsolutePath().normalize();
        if (!origemAbs.equals(destinoAbs)) return null;
        Path backup = pastaBackup.resolve(origem.getFileName()).normalize();
        if (!backup.startsWith(pastaBackup)) {
            throw new RaspagemRevisaoException("Caminho de backup inválido para: " + origem);
        }
        try {
            Files.createDirectories(backup.getParent());
            if (Files.notExists(backup)) {
                Files.copy(origemAbs, backup, StandardCopyOption.COPY_ATTRIBUTES);
            }
            return backup;
        } catch (IOException e) {
            throw new RaspagemRevisaoException("Falha ao criar backup da legenda: " + origem, e);
        }
    }

    private boolean deveIgnorarAuditoria(EventoLegenda evento, String texto) {
        if (evento.estilo() != null
            && propriedades.estiloIgnorado(evento.estilo())
            && !detectorKaraoke.eKaraokeOuMusicaTraduzivel(evento.estilo(), texto)) {
            return true;
        }
        if (detectorKaraoke.eEfeitoKaraoke(texto)
            && !detectorKaraoke.eKaraokeOuMusicaTraduzivel(evento.estilo(), texto)) {
            return true;
        }
        if (protecaoAss.deveIgnorarIntervencaoIa(evento.estilo(), texto)) {
            return true;
        }
        String estilo = evento.estilo() != null ? evento.estilo().toLowerCase() : "";
        if (estilo.contains("sign")) {
            return true;
        }
        String visivel = extrairTextoVisivel(texto);
        return estilo.contains("romaji") && visivel.equalsIgnoreCase("you");
    }

    private String extrairTextoVisivel(String texto) {
        return texto.replaceAll("\\{[^}]*\\}", "").replace("\\N", " ").trim();
    }

    private String normalizarTexto(String texto) {
        return texto == null ? "" : texto.replaceAll("\\s+", " ").trim();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: localiza deterministicamente o cache correspondente
     * à legenda PT-BR mesmo quando a raiz contém subpastas por obra.
     *
     * <p>INVARIANTES DO DOMÍNIO: candidatos diretos têm prioridade; busca
     * recursiva é ordenada e nunca seleciona arquivo fora da raiz informada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: erro de varredura devolve o caminho
     * esperado, que será tratado como cache ausente pelo leitor.
     */
    private Path resolverArquivoCache(Path arquivoPt, Path cacheDir) {
        String baseLegenda = nomeBaseLegenda(arquivoPt);
        String baseMidia = normalizarBaseLegenda(baseLegenda);
        String codigoEpisodio = extrairCodigoEpisodio(baseLegenda);

        for (String candidato : candidatosNomeCache(baseLegenda, baseMidia)) {
            Path direto = cacheDir.resolve(candidato);
            if (Files.isRegularFile(direto)) {
                return direto;
            }
        }

        if (Files.isDirectory(cacheDir)) {
            try (Stream<Path> stream = Files.walk(cacheDir)) {
                return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> correspondeCache(p.getFileName().toString(), baseMidia, codigoEpisodio))
                    .sorted()
                    .findFirst()
                    .orElse(cacheDir.resolve(baseMidia + "_ENG.cache.json"));
            } catch (IOException e) {
                return cacheDir.resolve(baseMidia + "_ENG.cache.json");
            }
        }

        return cacheDir.resolve(baseMidia + "_ENG.cache.json");
    }

    private List<String> candidatosNomeCache(String baseLegenda, String baseMidia) {
        Set<String> candidatos = new LinkedHashSet<>();
        candidatos.add(baseMidia + "_ENG.cache.json");
        candidatos.add(baseMidia + ".cache.json");
        candidatos.add(baseLegenda + ".cache.json");
        candidatos.add(baseLegenda + "_ENG.cache.json");
        return List.copyOf(candidatos);
    }

    private boolean correspondeCache(String nomeArquivo, String baseMidia, String codigoEpisodio) {
        if (!nomeArquivo.toLowerCase().endsWith(".cache.json")) {
            return false;
        }
        String stem = nomeArquivo.substring(0, nomeArquivo.length() - ".cache.json".length());
        if (normalizarBaseLegenda(stem).equalsIgnoreCase(baseMidia)) {
            return true;
        }
        return codigoEpisodio != null
            && nomeArquivo.toUpperCase().contains(codigoEpisodio)
            && nomeArquivo.toUpperCase().contains("_ENG");
    }

    private String nomeBaseLegenda(Path arquivoPt) {
        String nome = arquivoPt.getFileName().toString();
        String ext = extensaoLegenda(nome);
        return nome.substring(0, nome.length() - ext.length());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: fornece à revisão as referências EN/PT produzidas
     * pelas etapas 4 e 5, aceitando cache legado e versionado.
     *
     * <p>INVARIANTES DO DOMÍNIO: somente objetos de entrada são convertidos e a
     * leitura nunca modifica o banco de cache.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: registra aviso e devolve lista vazia,
     * permitindo usar uma legenda inglesa externa como fallback.
     */
    private List<EntradaCache> carregarEntradasCache(Path cachePath) {
        if (!Files.isRegularFile(cachePath)) {
            return new ArrayList<>();
        }
        try {
            return leitorCache.carregar(cachePath);
        } catch (IOException e) {
            out(AnsiCores.YELLOW + "  Aviso: não foi possível ler cache "
                + cachePath.getFileName() + ": " + e.getMessage() + AnsiCores.RESET);
            return new ArrayList<>();
        }
    }

    private Map<String, String> indexarOriginalPorTraduzido(List<EntradaCache> entradas) {
        Map<String, String> mapa = new HashMap<>();
        for (EntradaCache entrada : entradas) {
            if (entrada.traduzido() == null || entrada.traduzido().isBlank()) {
                continue;
            }
            if (entrada.original() == null || entrada.original().isBlank()) {
                continue;
            }
            mapa.putIfAbsent(normalizarTexto(entrada.traduzido()), entrada.original());
        }
        return mapa;
    }

    private Map<Integer, String> carregarOriginaisDeLegenda(Path arquivoEn) {
        Map<Integer, String> mapa = new HashMap<>();
        if (!Files.isRegularFile(arquivoEn)) {
            return mapa;
        }

        DocumentoLegenda docEn = leitor.ler(arquivoEn);
        for (EventoLegenda evento : docEn.eventos()) {
            if (evento.isDialogo() && evento.texto() != null && !evento.texto().isBlank()) {
                mapa.put(evento.indice(), evento.texto());
            }
        }
        return mapa;
    }

    private Path resolverArquivoOriginal(Path arquivoPt, Path pastaLegendasEn) {
        String nome = arquivoPt.getFileName().toString();
        String ext = extensaoLegenda(nome);
        String baseSemPt = normalizarBaseLegenda(nome.substring(0, nome.length() - ext.length()));
        String codigoEpisodio = extrairCodigoEpisodio(baseSemPt);

        Set<String> candidatos = new LinkedHashSet<>();
        candidatos.add(baseSemPt + ext);
        candidatos.add(baseSemPt + "_ENG" + ext);
        candidatos.add(baseSemPt + "_Eng" + ext);
        for (int track = 1; track <= 9; track++) {
            candidatos.add(baseSemPt + "_Track" + track + ext);
        }

        Matcher ptbrTrack = SUFIXO_PTBR_TRACK.matcher(nome.substring(0, nome.length() - ext.length()));
        if (ptbrTrack.find()) {
            String baseMidia = nome.substring(0, ptbrTrack.start());
            candidatos.add(baseMidia + "_Track2" + ext);
            candidatos.add(baseMidia + "_Track1" + ext);
            candidatos.add(baseMidia + ext);
        }

        for (String candidato : candidatos) {
            Path path = pastaLegendasEn.resolve(candidato);
            if (Files.isRegularFile(path) && !path.equals(arquivoPt) && !eLegendaTraduzida(path)) {
                return path;
            }
        }

        if (codigoEpisodio != null && Files.isDirectory(pastaLegendasEn)) {
            try (Stream<Path> stream = Files.list(pastaLegendasEn)) {
                return stream
                    .filter(Files::isRegularFile)
                    .filter(this::temExtensaoSuportada)
                    .filter(p -> !p.equals(arquivoPt))
                    .filter(p -> !eLegendaTraduzida(p))
                    .filter(p -> p.getFileName().toString().toUpperCase().contains(codigoEpisodio))
                    .min(Comparator.comparingInt(p -> preferenciaArquivoOriginal(p.getFileName().toString())))
                    .orElse(pastaLegendasEn.resolve(baseSemPt + ext));
            } catch (IOException e) {
                return pastaLegendasEn.resolve(baseSemPt + ext);
            }
        }

        return pastaLegendasEn.resolve(baseSemPt + ext);
    }

    private int preferenciaArquivoOriginal(String nome) {
        String n = nome.toLowerCase();
        if (n.contains("_track2")) {
            return 0;
        }
        if (n.contains("_eng")) {
            return 1;
        }
        if (n.contains("_track1")) {
            return 2;
        }
        return 10;
    }

    private boolean eLegendaTraduzida(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase();
        return nome.contains("_ptbr") || nome.contains("_pt-br");
    }

    private String normalizarBaseLegenda(String base) {
        return base
            .replaceFirst("(?i)_PT-?BR(_Track\\d+)?$", "")
            .replaceFirst("(?i)_Track\\d+$", "")
            .replaceFirst("(?i)_ENG$", "");
    }

    private String extensaoLegenda(String nome) {
        return nome.toLowerCase().endsWith(".ssa") ? ".ssa" : ".ass";
    }

    private String extrairCodigoEpisodio(String nome) {
        Matcher matcher = CODIGO_EPISODIO.matcher(nome);
        return matcher.find() ? matcher.group(1).toUpperCase() : null;
    }

    private Optional<String> tentarRevisarConcordancia(
        String original, String traduzido, List<String> motivos
    ) {
        String textoOriginal = original != null ? original : "";
        MascaradorTags.Mascarado mascOriginal = mascaradorTags.mascarar(textoOriginal);
        MascaradorTags.Mascarado mascTraduzido = mascaradorTags.mascarar(traduzido);

        boolean precisaRetraducaoCompleta = motivos.stream().anyMatch(
            m -> m.contains("Resíduo gringo") || m.contains("não traduzida"));
        Optional<String> resposta;

        if (precisaRetraducaoCompleta) {
            resposta = mistralPort.corrigirTraducao(
                mascOriginal.texto(),
                mascTraduzido.texto(),
                String.join(", ", motivos)
            );
        } else {
            resposta = mistralPort.revisarConcordancia(
                mascOriginal.texto(),
                mascTraduzido.texto(),
                motivos
            );
        }

        if (resposta.isEmpty()) {
            return Optional.empty();
        }

        try {
            String desmascarado = mascaradorTags.desmascarar(resposta.get(), mascTraduzido.tags());
            validador.validarFala(desmascarado);
            if (protecaoAss.respostaSuspeita(original, desmascarado)) {
                return Optional.empty();
            }
            return Optional.of(desmascarado);
        } catch (AlucinacaoDetectadaException e) {
            return Optional.empty();
        }
    }

    private void pausaGoogle() {
        try {
            Thread.sleep(PAUSA_GOOGLE_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean temExtensaoSuportada(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase();
        return EXTENSOES.stream().anyMatch(nome::endsWith);
    }
}
