package org.traducao.projeto.analisadorMidia.application;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.analisadorMidia.domain.*;
import org.traducao.projeto.analisadorMidia.infrastructure.adapters.FfprobeAdapter;
import org.traducao.projeto.telemetria.MidiaTelemetria;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AnalisarMidiaUseCase {

    private static final Logger log = LoggerFactory.getLogger(AnalisarMidiaUseCase.class);
    private static final List<String> EXTENSOES_VIDEO = List.of(
        ".mkv", ".mp4", ".avi", ".mov", ".flv", ".wmv", ".webm", ".m4v", ".ts", ".m2ts"
    );

    // Classificação por traduzibilidade (o dado vital da análise): legenda de
    // TEXTO é extraível e traduzível; BITMAP/hardsub exige OCR e não entra no
    // pipeline de tradução direto. Baseado no tipoCurto de classificarLegenda().
    private static final Set<String> TIPOS_TEXTO = Set.of("ASS", "SSA", "SRT", "WEBVTT", "MOV_TEXT");
    private static final Set<String> TIPOS_BITMAP = Set.of("PGS", "VOBSUB", "DVB", "HARDSUB");

    private final FfprobeAdapter ffprobeAdapter;
    private final TelemetriaService telemetriaService;

    public AnalisarMidiaUseCase(FfprobeAdapter ffprobeAdapter, TelemetriaService telemetriaService) {
        this.ffprobeAdapter = ffprobeAdapter;
        this.telemetriaService = telemetriaService;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: audita tecnicamente um lote de vídeos (Opção 1),
     * classifica as legendas por traduzibilidade e alimenta o dataset permanente
     * de telemetria. O resultado estruturado volta para a UI; NENHUM relatório é
     * gravado em disco e NENHUMA pasta {@code relatorios/} é criada junto da
     * mídia — a exportação TXT é manual no navegador.
     *
     * <p>INVARIANTES DO DOMÍNIO: a telemetria de mídia é um dataset permanente,
     * acumulada e deduplicada por arquivo (reanalisar a mesma mídia atualiza a
     * entrada, não duplica), e o histórico de mídias anteriores NUNCA é apagado
     * ao analisar um novo lote. Uma falha em um arquivo não aborta o lote; falha
     * cosmética da barra de progresso também não.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lote vazio lança
     * {@link AnalisadorException}; falha por arquivo vira uma {@link FalhaAnalise}
     * no resultado; falhas de persistência da telemetria são registradas sem
     * interromper a análise.
     *
     * @param entrada pasta ou arquivo de vídeo a auditar
     * @param saidaEfetiva reservado para compatibilidade; a análise não grava
     *                     relatório em disco, portanto não é utilizado
     */
    public ResultadoAnaliseLote executar(Path entrada, Path saidaEfetiva) {
        List<Path> arquivosAnalisar = encontrarVideos(entrada);

        if (arquivosAnalisar.isEmpty()) {
            throw new AnalisadorException("Nenhum arquivo de vídeo suportado encontrado no caminho especificado.");
        }

        log.info("Iniciando auditoria técnica para {} arquivo(s) de vídeo.", arquivosAnalisar.size());

        List<AuditoriaResultado> resultados = new ArrayList<>();
        List<FalhaAnalise> falhas = new ArrayList<>();
        // NÃO limpa o lote: a telemetria de mídia é um dataset permanente. Cada
        // registrarMidia() deduplica por nome de arquivo (atualiza a entrada),
        // preservando as mídias analisadas em lotes anteriores.

        // Barra de progresso para a análise do lote. É puramente cosmética: uma
        // falha de renderização dela (ex.: terminal incompatível) nunca deve
        // abortar a análise dos arquivos do lote, por isso é criada e fechada
        // manualmente (sem try-with-resources) com erros contidos.
        ProgressBar pb = null;
        try {
            pb = new ProgressBarBuilder()
                    .setTaskName("Analisando vídeos")
                    .setInitialMax(arquivosAnalisar.size())
                    .setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
                    .build();
        } catch (RuntimeException e) {
            log.warn("Não foi possível iniciar a barra de progresso (terminal incompatível); continuando sem ela: {}", e.getMessage());
        }

        try {
            for (Path arquivo : arquivosAnalisar) {
                try {
                    AuditoriaResultado resultado = analisarArquivo(arquivo);
                    resultados.add(resultado);

                    // Registra na telemetria da comunidade
                    registrarNaTelemetria(resultado, entrada);

                } catch (Exception e) {
                    log.error("Falha ao analisar o arquivo {}: {}", arquivo.getFileName(), e.getMessage(), e);
                    falhas.add(new FalhaAnalise(arquivo.getFileName().toString(), e.getMessage()));
                } finally {
                    if (pb != null) {
                        try {
                            pb.step();
                        } catch (RuntimeException e) {
                            log.warn("Barra de progresso falhou ao avançar durante a análise (ignorada): {}", e.getMessage());
                            pb = null;
                        }
                    }
                }
            }
        } finally {
            if (pb != null) {
                try {
                    pb.close();
                } catch (RuntimeException e) {
                    log.warn("Falha ao fechar a barra de progresso (ignorada): {}", e.getMessage());
                }
            }
        }

        // Registra a operação do lote na telemetria: os dados por arquivo já vão
        // via registrarMidia; aqui entram o total analisado e as falhas.
        telemetriaService.registrarOperacao(new OperacaoTelemetria(
            "Analise de Midia",
            "Analisados: " + resultados.size() + " | Falhas: " + falhas.size(),
            null,
            arquivosAnalisar.size(),
            resultados.size(),
            0,
            java.time.Instant.now().toString()
        ));

        // A telemetria canônica já foi persistida por registrarMidia() e
        // registrarOperacao() no destino interno (logs/); não há cópia para
        // relatorios/ nem relatório gravado junto da mídia.
        return new ResultadoAnaliseLote(resultados, falhas);
    }

    private List<Path> encontrarVideos(Path entrada) {
        if (Files.isRegularFile(entrada)) {
            String nome = entrada.getFileName().toString().toLowerCase();
            for (String ext : EXTENSOES_VIDEO) {
                if (nome.endsWith(ext)) {
                    return List.of(entrada);
                }
            }
            return List.of();
        }

        try (Stream<Path> walk = Files.walk(entrada)) {
            return walk
                .filter(Files::isRegularFile)
                .filter(p -> {
                    String nome = p.getFileName().toString().toLowerCase();
                    return EXTENSOES_VIDEO.stream().anyMatch(nome::endsWith);
                })
                .sorted()
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new AnalisadorException("Erro ao escanear diretório de entrada para encontrar vídeos: " + entrada, e);
        }
    }

    private AuditoriaResultado analisarArquivo(Path arquivo) {
        // Executa ffprobe inicial
        AuditoriaResultado base = ffprobeAdapter.analisarMidia(arquivo);
        
        List<LegendaInfo> legendasProcessadas = new ArrayList<>();
        List<String> logs = base.logsAuditoria();

        logs.add("=".repeat(80));
        logs.add("INICIANDO AUDITORIA TECNICA: " + base.nomeArquivo());
        logs.add("=".repeat(80));
        
        long tamanhoBytes = base.container().tamanhoBytes();
        double tamanhoMB = tamanhoBytes / (1024.0 * 1024.0);
        double tamanhoGB = tamanhoBytes / (1024.0 * 1024.0 * 1024.0);
        
        logs.add("Validação OK");
        logs.add(String.format("Tamanho: %.2f GiB (%.0f MB)", tamanhoGB, tamanhoMB));
        logs.add("Formatos detectados e mapeados via ffprobe com sucesso.\n");

        logs.add("=".repeat(80));
        logs.add("FORMATO DE LEGENDA DETECTADO");
        logs.add("=".repeat(80));
        if (base.legendas().isEmpty()) {
            logs.add("  NENHUMA FAIXA DE LEGENDA ENCONTRADA (RAW; hardsub nao confirmado por esta analise)");
        } else {
            for (LegendaInfo leg : base.legendas()) {
                String[] classifResumo = classificarLegenda(leg.codecId(), leg.formato());
                String tituloResumo = leg.titulo() != null && !leg.titulo().isBlank() ? " - " + leg.titulo() : "";
                logs.add(String.format("  [%d] %s | Idioma: %s%s",
                    leg.indexRelativo() + 1, classifResumo[0], leg.idioma(), tituloResumo));
            }
        }
        logs.add("");

        logs.add("=".repeat(80));
        logs.add("ESTRUTURA GERAL");
        logs.add("=".repeat(80));
        logs.add("Formato do Conteiner");
        logs.add("  " + base.container().formato());
        logs.add("Tamanho do Arquivo");
        logs.add(String.format("  %.2f GiB", tamanhoGB));
        logs.add("Duracao Total");
        logs.add("  " + formatarSegundos(base.container().duracaoSegundos()));
        logs.add("Bitrate Geral");
        logs.add("  " + (base.container().bitrateGeral() > 0 ? (base.container().bitrateGeral() / 1000) + " kbps" : "N/A"));
        logs.add("Aplicacao de Escrita");
        logs.add("  " + base.container().aplicacaoEscrita());

        logs.add("\n" + "=".repeat(80));
        logs.add("FLUXOS DE VIDEO");
        logs.add("=".repeat(80));
        for (VideoInfo v : base.videos()) {
            logs.add(String.format("\n  Fluxo %d (Track ID: %d)", v.index(), v.index()));
            logs.add(String.format("    Codec: %s (%s)", v.codecId(), v.format()));
            logs.add(String.format("    Resolucao: %dx%dp", v.width(), v.height()));
            logs.add(String.format("    Profundidade de Cor: %d bits", v.bitDepth()));
            logs.add(String.format("    Taxa de Quadros (FPS): %.3f fps", v.fps()));
            logs.add(String.format("    Aspect Ratio: %s", v.displayAspectRatio()));
            logs.add(String.format("    Bitrate: %s", v.bitrate() > 0 ? (v.bitrate() / 1000) + " kbps" : "N/A"));
        }

        logs.add("\n" + "=".repeat(80));
        logs.add("FLUXOS DE AUDIO");
        logs.add("=".repeat(80));
        for (AudioInfo a : base.audios()) {
            logs.add(String.format("\n  Fluxo %d (Track ID: %d)", a.index(), a.index()));
            logs.add(String.format("    Idioma: %s", a.idioma()));
            logs.add(String.format("    Codec/Formato: %s", a.format()));
            logs.add(String.format("    Canais: %d", a.channels()));
            logs.add(String.format("    Taxa de Amostragem: %.1f kHz", a.sampleRateKHz()));
            logs.add(String.format("    Bitrate: %s", a.bitrate() > 0 ? (a.bitrate() / 1000) + " kbps" : "N/A"));
            logs.add(String.format("    Titulo: %s", a.titulo()));
        }

        logs.add("\n" + "=".repeat(80));
        logs.add("FAIXAS DE LEGENDAS");
        logs.add("=".repeat(80));

        double duracaoVideoSegundos = base.container().duracaoSegundos();
        if (duracaoVideoSegundos <= 0.0 && !base.videos().isEmpty()) {
            // Se duracao geral do container falhar, tenta pegar a duracao do primeiro stream de video
            // ffprobe as vezes reporta duracao do stream mas nao do container
        }

        if (base.legendas().isEmpty()) {
            logs.add("\n    NENHUMA FAIXA DE LEGENDA ENCONTRADA");
            logs.add("    - Pode ser um arquivo RAW (sem faixa de legenda softsub)");
            logs.add("    - A legenda pode estar embutida como hardsub (NAO confirmado por esta analise)");
        } else {
            for (LegendaInfo leg : base.legendas()) {
                logs.add(String.format("\n  Legenda %d (Track ID: %d)", leg.indexRelativo() + 1, leg.index()));
                logs.add(String.format("    Idioma: %s", leg.idioma()));
                logs.add(String.format("    Formato: %s", leg.formato()));

                String[] classif = classificarLegenda(leg.codecId(), leg.formato());
                String tipoCompleto = classif[0];
                String tipoCurto = classif[1];
                String categoria = categoriaDe(tipoCurto);
                boolean texto = TIPOS_TEXTO.contains(tipoCurto);
                boolean bitmap = TIPOS_BITMAP.contains(tipoCurto);

                logs.add(String.format("    Tipo: %s (%s)", tipoCompleto, categoria));
                logs.add(String.format("    Codec ID: %s", leg.codecId()));
                logs.add(String.format("    Titulo: %s", leg.titulo()));
                logs.add(String.format("    Flags: default=%s forced=%s acessibilidade=%s",
                    leg.isDefault(), leg.isForced(), leg.acessibilidade()));
                logs.add(String.format("    Extraivel: %s | Traduzivel: %s | Exige OCR: %s", texto, texto, bitmap));

                // Indicadores temporais como INFORMACAO TECNICA (sem veredito de sincronia).
                Double duracaoLegenda = leg.duracaoSegundos();
                Double diferencaFim = (duracaoVideoSegundos > 0.0 && duracaoLegenda != null)
                    ? duracaoVideoSegundos - duracaoLegenda : null;
                if (duracaoLegenda != null) {
                    logs.add(String.format("    Duracao Legenda: %s", formatarSegundos(duracaoLegenda)));
                }
                if (diferencaFim != null) {
                    logs.add(String.format("    Diferenca p/ o video: %+.3fs (informativo)", diferencaFim));
                }

                legendasProcessadas.add(new LegendaInfo(
                    leg.index(), leg.indexRelativo(), leg.idioma(), leg.formato(), leg.codecId(), leg.titulo(),
                    tipoCompleto, tipoCurto, categoria, texto, texto, bitmap,
                    leg.isDefault(), leg.isForced(), leg.acessibilidade(),
                    duracaoLegenda, diferencaFim
                ));
            }
        }

        // Resumo final
        logs.add("\n" + "=".repeat(80));
        logs.add("RESUMO FINAL");
        logs.add("=".repeat(80));
        logs.add("  Total de Faixas: " + (1 + base.videos().size() + base.audios().size() + base.legendas().size()));
        logs.add("    Video(s): " + base.videos().size());
        logs.add("    Audio(s): " + base.audios().size());
        logs.add("    Legenda(s): " + base.legendas().size());

        for (LegendaInfo info : legendasProcessadas) {
            String tituloStr = info.titulo() != null && !info.titulo().isBlank() ? " - " + info.titulo() : "";
            logs.add(String.format("      [%d] Idioma: %s | Tipo: %s | Formato: %s%s",
                info.index(), info.idioma(), info.tipoCurto(), info.formato(), tituloStr
            ));
        }

        // Dado vital para a tradução: dá para extrair texto e traduzir este arquivo?
        logs.add("  Traduzivel (legenda de texto): " + verdictTraducao(legendasProcessadas));

        logs.add("\n" + "=".repeat(80));
        logs.add("Auditoria finalizada com sucesso!");
        logs.add("=".repeat(80) + "\n");

        return new AuditoriaResultado(
            arquivo, base.nomeArquivo(), base.container(), base.videos(), base.audios(), legendasProcessadas,
            base.capitulos(), base.anexos(), logs
        );
    }

    private static String categoriaDe(String tipoCurto) {
        if (TIPOS_TEXTO.contains(tipoCurto)) {
            return "TEXTO";
        }
        if (TIPOS_BITMAP.contains(tipoCurto)) {
            return "BITMAP";
        }
        return "DESCONHECIDO";
    }

    static String[] classificarLegenda(String codecId, String formato) {
        String codecUpper = (codecId != null ? codecId : "").toUpperCase();
        String formatoUpper = (formato != null ? formato : "").toUpperCase();

        if (codecUpper.contains("ASS") || formatoUpper.equals("ASS")) {
            return new String[]{"ASS (Estilizada com cores e posicionamento)", "ASS"};
        }
        if (codecUpper.contains("SSA") || formatoUpper.equals("SSA")) {
            return new String[]{"SSA (Estilizada - SubStation Alpha)", "SSA"};
        }
        if (codecUpper.contains("PGS") || codecUpper.contains("HDMV") || formatoUpper.contains("PGS")) {
            return new String[]{"PGS (Bitmap/Hardsub - Nao extraivel para texto)", "PGS"};
        }
        if (codecUpper.contains("VOBSUB") || formatoUpper.contains("VOBSUB") || formatoUpper.contains("DVD_SUBTITLE")) {
            return new String[]{"VobSub (Bitmap DVD - Nao extraivel para texto)", "VOBSUB"};
        }
        if (codecUpper.contains("DVBSUB") || formatoUpper.contains("DVB")) {
            return new String[]{"DVB Subtitle (Bitmap - Nao extraivel para texto)", "DVB"};
        }
        if (codecUpper.contains("WEBVTT") || codecUpper.contains("VTT") || formatoUpper.contains("WEBVTT")) {
            return new String[]{"WebVTT (Texto simples com timing web)", "WEBVTT"};
        }
        if (codecUpper.contains("UTF8") || codecUpper.contains("SUBRIP") || formatoUpper.contains("SRT") || formatoUpper.equals("UTF-8")) {
            return new String[]{"SRT/SubRip (Simples - Recomendado para traducao)", "SRT"};
        }
        if (codecUpper.contains("TX3G") || codecUpper.contains("MOV_TEXT") || formatoUpper.contains("TIMED TEXT")) {
            return new String[]{"MOV_TEXT/TX3G (Legenda de texto MP4)", "MOV_TEXT"};
        }
        if (codecUpper.equals("IN_SCREEN")) {
            return new String[]{"Hardsub (Queimada na tela - Nao extraivel)", "HARDSUB"};
        }

        return new String[]{"Desconhecido", "DESCONHECIDO"};
    }

    private String formatarSegundos(Double seconds) {
        if (seconds == null || seconds <= 0.0) {
            return "N/A";
        }
        long h = (long) (seconds / 3600.0);
        long m = (long) ((seconds % 3600.0) / 60.0);
        double s = seconds % 60.0;
        return String.format("%02d:%02d:%06.3f", h, m, s);
    }

    private void registrarNaTelemetria(AuditoriaResultado resultado, Path entrada) {
        // Relativiza o nome/caminho do arquivo para privacidade da telemetria
        String nomeRelativo = resultado.caminhoArquivo().getFileName().toString();
        try {
            nomeRelativo = entrada.toAbsolutePath().relativize(resultado.caminhoArquivo().toAbsolutePath()).toString();
        } catch (Exception ignored) {}

        double tamanhoMB = resultado.container().tamanhoBytes() / (1024.0 * 1024.0);
        
        String codecVideo = "N/A";
        String resolucao = "N/A";
        double fps = 0.0;
        
        if (!resultado.videos().isEmpty()) {
            VideoInfo v = resultado.videos().getFirst();
            codecVideo = v.codecId();
            resolucao = v.width() + "x" + v.height();
            fps = v.fps();
        }

        List<MidiaTelemetria.LegendaTelemetria> legTels = new ArrayList<>();
        for (LegendaInfo leg : resultado.legendas()) {
            legTels.add(new MidiaTelemetria.LegendaTelemetria(
                leg.indexRelativo() + 1,
                leg.idioma(),
                leg.formato(),
                leg.tipoCurto(),
                leg.categoria(),
                leg.traduzivel(),
                leg.diferencaFimSegundos()
            ));
        }

        MidiaTelemetria tel = new MidiaTelemetria(
            nomeRelativo,
            resultado.container().formato(),
            tamanhoMB,
            resultado.container().duracaoSegundos(),
            codecVideo,
            resolucao,
            fps,
            legTels,
            java.time.Instant.now().toString()
        );

        telemetriaService.registrarMidia(tel);
    }

    /**
     * Veredicto de traduzibilidade de um arquivo a partir das legendas detectadas:
     * texto (ASS/SRT/...) é extraível e traduzível; bitmap (PGS/VobSub/...) exige
     * OCR; sem legenda é RAW/hardsub.
     */
    static String verdictTraducao(List<LegendaInfo> legendas) {
        List<String> tiposTexto = legendas.stream().map(LegendaInfo::tipoCurto)
            .filter(TIPOS_TEXTO::contains).distinct().collect(Collectors.toList());
        if (!tiposTexto.isEmpty()) {
            return "SIM (" + String.join(", ", tiposTexto) + ")";
        }
        List<String> tiposBitmap = legendas.stream().map(LegendaInfo::tipoCurto)
            .filter(TIPOS_BITMAP::contains).distinct().collect(Collectors.toList());
        if (!tiposBitmap.isEmpty()) {
            return "NAO - bitmap (" + String.join(", ", tiposBitmap) + "), precisa de OCR";
        }
        if (!legendas.isEmpty()) {
            return "INDETERMINADO (" + legendas.getFirst().tipoCurto() + ")";
        }
        return "N/A - sem legenda (RAW ou hardsub)";
    }

}
