package org.traducao.projeto.analisadorMidia.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.analisadorMidia.domain.*;
import org.traducao.projeto.analisadorMidia.infrastructure.adapters.FfprobeAdapter;
import org.traducao.projeto.telemetria.MidiaTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final ObjectMapper objectMapper;

    public AnalisarMidiaUseCase(FfprobeAdapter ffprobeAdapter, TelemetriaService telemetriaService) {
        this.ffprobeAdapter = ffprobeAdapter;
        this.telemetriaService = telemetriaService;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public ResultadoAnaliseLote executar(Path entrada, Path saidaEfetiva) {
        List<Path> arquivosAnalisar = encontrarVideos(entrada);

        if (arquivosAnalisar.isEmpty()) {
            throw new AnalisadorException("Nenhum arquivo de vídeo suportado encontrado no caminho especificado.");
        }

        // Define a pasta de relatórios/logs
        Path pastaRelatorios = saidaEfetiva;
        if (pastaRelatorios == null) {
            Path entradaAbsoluta = entrada.toAbsolutePath();
            Path pastaPai = Files.isDirectory(entrada) ? entradaAbsoluta : entradaAbsoluta.getParent();
            pastaRelatorios = (pastaPai != null) ? pastaPai.resolve("relatorios") : Path.of("relatorios").toAbsolutePath();
        }
        try {
            Files.createDirectories(pastaRelatorios);
        } catch (IOException e) {
            throw new AnalisadorException("Não foi possível criar a pasta de relatórios: " + pastaRelatorios, e);
        }

        log.info("Iniciando auditoria técnica para {} arquivo(s) de vídeo.", arquivosAnalisar.size());
        log.info("Relatórios serão salvos em: {}", pastaRelatorios.toAbsolutePath());

        List<AuditoriaResultado> resultados = new ArrayList<>();
        telemetriaService.limparLote();

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

        // Salvar relatórios consolidados e telemetria consolidada
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        // Se for um único arquivo, já salvou o individual. Se for mais, salvamos o consolidado
        Path relatorioPrincipal = null;
        if (resultados.size() > 1) {
            relatorioPrincipal = salvarRelatorioConsolidado(resultados, pastaRelatorios, entrada.getFileName().toString(), timestamp);
        } else if (resultados.size() == 1) {
            relatorioPrincipal = salvarRelatorioIndividual(resultados.getFirst(), pastaRelatorios, timestamp);
        }

        // Persiste a telemetria consolidada na pasta de relatórios
        telemetriaService.salvar(pastaRelatorios);

        return new ResultadoAnaliseLote(resultados, relatorioPrincipal);
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
            logs.add("  NENHUMA LEGENDA ENCONTRADA (arquivo RAW ou legenda queimada/hardsub)");
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
            logs.add("\n    NENHUMA LEGENDA ENCONTRADA");
            logs.add("    - Arquivo eh uma RAW (sem legenda)");
            logs.add("    - Ou a legenda esta com HARDSUB (queimada na imagem)");
            logs.add("    - Verifique o arquivo antes de usar no pipeline");
        } else {
            for (LegendaInfo leg : base.legendas()) {
                logs.add(String.format("\n  Legenda %d (Track ID: %d)", leg.indexRelativo() + 1, leg.index()));
                logs.add(String.format("    Idioma: %s", leg.idioma()));
                logs.add(String.format("    Formato: %s", leg.formato()));
                
                String[] classif = classificarLegenda(leg.codecId(), leg.formato());
                String tipoCompleto = classif[0];
                String tipoCurto = classif[1];
                
                logs.add(String.format("    Tipo: %s", tipoCompleto));
                logs.add(String.format("    Codec ID: %s", leg.codecId()));
                logs.add(String.format("    Titulo: %s", leg.titulo()));

                // Auditoria de Sincronia de Legenda
                Double duracaoLegendaSegundos = leg.duracaoMetadadosSegundos();
                String metodoDuracao = "Metadados";
                Double duracaoPacotesSegundos = null;

                // Se a duracao nos metadados for nula ou muito proxima da duracao do video (um placeholder comum),
                // tenta obter de forma precisa via ffprobe por pacotes
                if (duracaoVideoSegundos > 0.0) {
                    if (duracaoLegendaSegundos == null || Math.abs(duracaoLegendaSegundos - duracaoVideoSegundos) < 0.05) {
                        double[] range = ffprobeAdapter.obterTimestampsLegenda(arquivo, leg.indexRelativo());
                        if (range != null) {
                            duracaoPacotesSegundos = range[1];
                            duracaoLegendaSegundos = range[1];
                            metodoDuracao = "Analise de Pacotes (ffprobe)";
                        }
                    }
                }

                Double diferencaFim = null;
                Double driftRatio = null;
                String veredicto = "N/A";

                if (duracaoVideoSegundos > 0.0 && duracaoLegendaSegundos != null) {
                    diferencaFim = duracaoVideoSegundos - duracaoLegendaSegundos;
                    double diffAbs = Math.abs(diferencaFim);
                    double duracaoHoras = duracaoVideoSegundos / 3600.0;
                    driftRatio = diffAbs / duracaoHoras;

                    logs.add(String.format("    Duracao Legenda: %s (via %s)", formatarSegundos(duracaoLegendaSegundos), metodoDuracao));
                    logs.add(String.format("    Diferenca Fim: %+.3fs (Video - Legenda)", diferencaFim));
                    logs.add(String.format("    Taxa de Drift: %.3f s/hora", driftRatio));

                    // Veredicto
                    if (duracaoLegendaSegundos < (duracaoVideoSegundos * 0.5)) {
                        veredicto = "Legenda Parcial Muxed (Sem necessidade de alteracao de sync global)";
                    } else if (diffAbs <= 1.5) {
                        veredicto = "Legenda Sincronizada! (Diferenca dentro da margem segura)";
                    } else {
                        double ratio = duracaoVideoSegundos / duracaoLegendaSegundos;
                        boolean fpsMismatch = false;
                        String label = "";

                        if (Math.abs(ratio - 1.042709) < 0.0015) {
                            fpsMismatch = true;
                            label = "25.000 -> 23.976 fps (Estiramento PAL para NTSC)";
                        } else if (Math.abs(ratio - 0.959040) < 0.0015) {
                            fpsMismatch = true;
                            label = "23.976 -> 25.000 fps (Estiramento NTSC para PAL)";
                        } else if (Math.abs(ratio - 1.001001) < 0.0015) {
                            fpsMismatch = true;
                            label = "24.000 -> 23.976 fps";
                        } else if (Math.abs(ratio - 0.999000) < 0.0015) {
                            fpsMismatch = true;
                            label = "23.976 -> 24.000 fps";
                        }

                        if (fpsMismatch) {
                            veredicto = "Legenda Desalinhada - Necessita Estiramento de Tempo! (FPS Mismatch: " + label + ")";
                        } else {
                            long sugeridoMs = Math.round(diferencaFim * 1000.0);
                            veredicto = String.format("Legenda Desalinhada - Possivel atraso constante! (Sugestao: Offset de %d ms)", sugeridoMs);
                        }
                    }

                    logs.add("    Veredito de Sincronia: " + veredicto);
                }

                legendasProcessadas.add(new LegendaInfo(
                    leg.index(), leg.indexRelativo(), leg.idioma(), leg.formato(), leg.codecId(), leg.titulo(),
                    tipoCompleto, tipoCurto, leg.duracaoMetadadosSegundos(), duracaoPacotesSegundos,
                    metodoDuracao, duracaoLegendaSegundos, diferencaFim, driftRatio, veredicto
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
            arquivo, base.nomeArquivo(), base.container(), base.videos(), base.audios(), legendasProcessadas, logs
        );
    }

    private String[] classificarLegenda(String codecId, String formato) {
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
                leg.veredicto(),
                leg.driftRatio(),
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

    private Path salvarRelatorioIndividual(AuditoriaResultado res, Path pasta, String timestamp) {
        String nomeBase = getNomeSemExtensao(res.nomeArquivo());
        Path arqTxt = pasta.resolve(nomeBase + "_" + timestamp + ".txt");
        Path arqJson = pasta.resolve(nomeBase + "_" + timestamp + ".json");

        try {
            // Salva TXT
            Files.write(arqTxt, res.logsAuditoria());
            log.info("Relatório de texto salvo: {}", arqTxt);

            // Salva JSON
            objectMapper.writeValue(arqJson.toFile(), res);
            log.info("Relatório JSON salvo: {}", arqJson);
            return arqTxt;
        } catch (IOException e) {
            log.error("Erro ao salvar relatórios individuais para {}: {}", res.nomeArquivo(), e.getMessage());
            return null;
        }
    }

    private Path salvarRelatorioConsolidado(List<AuditoriaResultado> resultados, Path pasta, String nomeEntrada, String timestamp) {
        Path arqTxt = pasta.resolve("consolidado_" + nomeEntrada + "_" + timestamp + ".txt");
        List<String> consolidado = new ArrayList<>();

        // Resumo escaneável no topo: por arquivo, o tipo de legenda e se é
        // traduzível — o dado vital para decidir o que segue no pipeline.
        consolidado.addAll(montarResumoLegendasLote(resultados));

        for (AuditoriaResultado res : resultados) {
            consolidado.addAll(res.logsAuditoria());
            consolidado.add("\n" + "=".repeat(100) + "\n\n");

            // Também salvamos o JSON individual de cada arquivo para auditoria fina
            salvarRelatorioIndividual(res, pasta, timestamp);
        }

        try {
            Files.write(arqTxt, consolidado);
            log.info("Relatório consolidado de texto salvo com {} arquivos em: {}", resultados.size(), arqTxt);
            return arqTxt;
        } catch (IOException e) {
            log.error("Erro ao salvar relatório consolidado em {}: {}", arqTxt, e.getMessage());
            return null;
        }
    }

    /**
     * Veredicto de traduzibilidade de um arquivo a partir das legendas detectadas:
     * texto (ASS/SRT/...) é extraível e traduzível; bitmap (PGS/VobSub/...) exige
     * OCR; sem legenda é RAW/hardsub.
     */
    private String verdictTraducao(List<LegendaInfo> legendas) {
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

    /**
     * Resumo escaneável do lote com o dado vital para a tradução: por arquivo, o
     * tipo de legenda encontrado e se é traduzível, mais contagens agregadas.
     */
    private List<String> montarResumoLegendasLote(List<AuditoriaResultado> resultados) {
        int comTexto = 0, comBitmap = 0, semLegenda = 0, indeterminado = 0;
        Map<String, Integer> contagemTipo = new LinkedHashMap<>();
        List<String> linhasArquivos = new ArrayList<>();

        for (AuditoriaResultado res : resultados) {
            List<LegendaInfo> legs = res.legendas();
            LegendaInfo texto = legs.stream().filter(l -> TIPOS_TEXTO.contains(l.tipoCurto())).findFirst().orElse(null);
            LegendaInfo bitmap = legs.stream().filter(l -> TIPOS_BITMAP.contains(l.tipoCurto())).findFirst().orElse(null);
            long faixasTexto = legs.stream().filter(l -> TIPOS_TEXTO.contains(l.tipoCurto())).count();

            String verdict;
            String tipo;
            String idioma;
            if (texto != null) {
                verdict = "SIM"; tipo = texto.tipoCurto(); idioma = idiomaCurto(texto.idioma());
                contagemTipo.merge(tipo, 1, Integer::sum);
                comTexto++;
            } else if (bitmap != null) {
                verdict = "NAO"; tipo = bitmap.tipoCurto(); idioma = idiomaCurto(bitmap.idioma());
                contagemTipo.merge(tipo, 1, Integer::sum);
                comBitmap++;
            } else if (!legs.isEmpty()) {
                verdict = "?"; tipo = legs.getFirst().tipoCurto(); idioma = idiomaCurto(legs.getFirst().idioma());
                contagemTipo.merge(tipo, 1, Integer::sum);
                indeterminado++;
            } else {
                verdict = "---"; tipo = "SEM"; idioma = "-";
                semLegenda++;
            }
            String extra = faixasTexto > 1 ? "  (+" + (faixasTexto - 1) + " faixa(s) de texto)" : "";
            linhasArquivos.add(String.format(" [%-3s] %-9s %-7s %s%s", verdict, tipo, idioma, res.nomeArquivo(), extra));
        }

        List<String> linhas = new ArrayList<>();
        linhas.add("=".repeat(80));
        linhas.add("RESUMO DE LEGENDAS DO LOTE  (dado vital para a traducao)");
        linhas.add("=".repeat(80));
        linhas.add("  Arquivos analisados                  : " + resultados.size());
        linhas.add("  Com legenda de TEXTO (traduzivel)    : " + comTexto);
        linhas.add("  Com legenda BITMAP (precisa de OCR)  : " + comBitmap);
        if (indeterminado > 0) {
            linhas.add("  Tipo indeterminado                   : " + indeterminado);
        }
        linhas.add("  Sem legenda (RAW/hardsub)            : " + semLegenda);
        if (!contagemTipo.isEmpty()) {
            String detalhe = contagemTipo.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("  "));
            linhas.add("  Contagem por tipo detectado          : " + detalhe);
        }
        linhas.add("-".repeat(80));
        linhas.add("  Legenda:  [SIM]=texto traduzivel   [NAO]=bitmap (OCR)   [---]=sem legenda");
        linhas.add("  Colunas:  [verdict] TIPO  IDIOMA  ARQUIVO");
        linhas.add("-".repeat(80));
        linhas.addAll(linhasArquivos);
        linhas.add("=".repeat(80));
        linhas.add("");
        return linhas;
    }

    private String idiomaCurto(String idioma) {
        if (idioma == null || idioma.isBlank() || idioma.equalsIgnoreCase("Desconhecido")) {
            return "?";
        }
        return idioma.length() > 6 ? idioma.substring(0, 6) : idioma;
    }

    private String getNomeSemExtensao(String nome) {
        int dotIdx = nome.lastIndexOf('.');
        if (dotIdx > 0) {
            return nome.substring(0, dotIdx);
        }
        return nome;
    }
}
