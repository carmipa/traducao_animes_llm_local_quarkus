package org.traducao.projeto.auditorConteudoLegendas.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AssAuditoriaFixtures {

    private AssAuditoriaFixtures() {}

    public static void escreverParLimpo(Path original, Path traduzido) throws IOException {
        String cabecalho = cabecalhoComEstilos("1920", "1080", "Default");
        String linhaOrig = "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Hello\\NWorld\n";
        String linhaTrad = "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Ola\\NMundo\n";
        Files.writeString(original, cabecalho + linhaOrig, StandardCharsets.UTF_8);
        Files.writeString(traduzido, cabecalho + linhaTrad, StandardCharsets.UTF_8);
    }

    public static void escreverParComQuebraExcessiva(Path original, Path traduzido) throws IOException {
        String cabecalho = cabecalhoComEstilos("1920", "1080", "Default");
        String linhaOrig = "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,This is a single line.\n";
        String linhaTrad = "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Esta\\Ne\\Numa\\Nunica\\Nlinha.\n";
        Files.writeString(original, cabecalho + linhaOrig, StandardCharsets.UTF_8);
        Files.writeString(traduzido, cabecalho + linhaTrad, StandardCharsets.UTF_8);
    }

    public static void escreverParComPlayResAlterado(Path original, Path traduzido) throws IOException {
        String cabOrig = cabecalhoComEstilos("1920", "1080", "Default");
        String cabTrad = cabecalhoComEstilos("1280", "720", "Default");
        String linha = "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Texto\n";
        Files.writeString(original, cabOrig + linha, StandardCharsets.UTF_8);
        Files.writeString(traduzido, cabTrad + linha, StandardCharsets.UTF_8);
    }

    public static void escreverParComEstiloRemovido(Path original, Path traduzido) throws IOException {
        String cabOrig = cabecalhoComEstilos("1920", "1080", "Default", "Opening");
        String cabTrad = cabecalhoComEstilos("1920", "1080", "Default");
        String linha = "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Texto\n";
        Files.writeString(original, cabOrig + linha, StandardCharsets.UTF_8);
        Files.writeString(traduzido, cabTrad + linha, StandardCharsets.UTF_8);
    }

    private static String cabecalhoComEstilos(String playResX, String playResY, String... estilos) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Script Info]\n");
        sb.append("ScriptType: v4.00+\n");
        sb.append("PlayResX: ").append(playResX).append('\n');
        sb.append("PlayResY: ").append(playResY).append('\n');
        sb.append('\n');
        sb.append("[V4+ Styles]\n");
        sb.append("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\n");
        for (String estilo : estilos) {
            sb.append("Style: ").append(estilo)
                .append(",Arial,20,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,2,0,2,10,10,10,1\n");
        }
        sb.append('\n');
        sb.append("[Events]\n");
        sb.append("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n");
        return sb.toString();
    }
}
