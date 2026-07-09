package org.traducao.projeto.novoKaraoke.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.traducao.presentation.web.LogStreamService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversorKaraokeUseCaseTest {

    @TempDir
    Path tempDir;

    @Test
    void arquivoSemMusicaEhCopiadoByteIdentico() throws Exception {
        Path origem = tempDir.resolve("sem-musica.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        byte[] original = """
            [Script Info]\r
            PlayResY: 1080\r
            \r
            [V4+ Styles]\r
            Format: Name,Fontname,Fontsize,PrimaryColour,SecondaryColour,OutlineColour,BackColour,Bold,Italic,Underline,StrikeOut,ScaleX,ScaleY,Spacing,Angle,BorderStyle,Outline,Shadow,Alignment,MarginL,MarginR,MarginV,Encoding\r
            Style: Default,Arial,48,&H00FFFFFF,&H000000FF,&H00000000,&H96000000,0,0,0,0,100,100,0,0,1,2,1,2,30,30,30,1\r
            \r
            [Events]\r
            Format: Layer,Start,End,Style,Name,MarginL,MarginR,MarginV,Effect,Text\r
            Comment: 0,0:00:00.00,0:00:01.00,Default,,0,0,0,,template preservado\r
            Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Fala comum.\r
            """.getBytes(StandardCharsets.UTF_8);
        Files.write(origem, original);

        novoConversor().converterArquivo(origem, destino, true);

        assertEquals(new String(original, StandardCharsets.UTF_8),
            Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8));
    }

    @Test
    void deduplicaRomajiEPtBrNoMesmoTempoELimpaTagsVisiveis() throws Exception {
        Path origem = tempDir.resolve("karaoke.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(origem, cabecalho()
            + "Dialogue: 0,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\pos(100,40)}aigan shitemo kongan shitemo kawaranai ya, mou\n"
            + "Dialogue: 0,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\pos(100,80)}[]Não importa o quanto eu deseje, nada muda [![TAG1]]\n",
            StandardCharsets.UTF_8);

        novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        assertTrue(saida.contains("Dialogue: 0,0:00:01.00,0:00:04.00,Karaoke Simples,,0,0,0,,Não importa o quanto eu deseje, nada muda"));
        assertFalse(saida.contains("aigan shitemo"));
        assertFalse(saida.contains("[]"));
        assertFalse(saida.contains("TAG1"));
    }

    @Test
    void preservaEventoCurtoSemCoberturaRealMesmoPertoDaLinhaPrincipal() throws Exception {
        Path origem = tempDir.resolve("curta.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(origem, cabecalho()
            + "Dialogue: 0,0:00:10.00,0:00:12.00,Opening,,0,0,0,,{\\pos(100,40)}Linha principal da música\n"
            + "Dialogue: 0,0:00:19.00,0:00:20.00,Opening,,0,0,0,,{\\pos(100,80)}Ei\n",
            StandardCharsets.UTF_8);

        novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        assertTrue(saida.contains("Dialogue: 0,0:00:10.00,0:00:12.00,Karaoke Simples,,0,0,0,,Linha principal da música"));
        assertTrue(saida.contains("Dialogue: 0,0:00:19.00,0:00:20.00,Opening,,0,0,0,,{\\pos(100,80)}Ei"));
    }

    @Test
    void ignoraArquivosAuxiliaresQuandoHaEpisodiosPrincipais() throws Exception {
        Path origem = Files.createDirectory(tempDir.resolve("origem"));
        Path destino = tempDir.resolve("saida");
        Files.writeString(origem.resolve("[DB]86_-_01_(Dual Audio)_Track6_PT-BR.ass"), cabecalho()
            + "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Fala comum.\n",
            StandardCharsets.UTF_8);
        Files.writeString(origem.resolve("[DB]86_-_NCOP01_(10bit)_Track2_PT-BR.ass"), cabecalho()
            + "Dialogue: 0,0:00:01.00,0:00:03.00,Opening,,0,0,0,,Letra auxiliar.\n",
            StandardCharsets.UTF_8);
        Files.writeString(origem.resolve("[DB]86 Special Edition Senya_-_SP_(10bit)_Track2_PT-BR.ass"), cabecalho()
            + "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Especial.\n",
            StandardCharsets.UTF_8);

        List<String> processados = novoConversor().simular(origem, destino).stream()
            .map(r -> r.getArquivoOrigem())
            .toList();

        assertEquals(List.of("[DB]86_-_01_(Dual Audio)_Track6_PT-BR.ass"), processados);
    }

    @Test
    void processaAuxiliaresQuandoPastaTemApenasAuxiliares() throws Exception {
        Path origem = Files.createDirectory(tempDir.resolve("origem"));
        Path destino = tempDir.resolve("saida");
        Files.writeString(origem.resolve("[DB]86_-_NCED01_(10bit)_Track2_PT-BR.ass"), cabecalho()
            + "Dialogue: 0,0:00:01.00,0:00:03.00,Ending,,0,0,0,,Letra auxiliar.\n",
            StandardCharsets.UTF_8);

        List<String> processados = novoConversor().simular(origem, destino).stream()
            .map(r -> r.getArquivoOrigem())
            .toList();

        assertEquals(List.of("[DB]86_-_NCED01_(10bit)_Track2_PT-BR.ass"), processados);
    }

    private static ConversorKaraokeUseCase novoConversor() {
        ConversorKaraokeUseCase conversor = new ConversorKaraokeUseCase();
        conversor.detectorKaraoke = new DetectorEfeitoKaraokeService();
        conversor.logStream = new LogStreamSilencioso();
        return conversor;
    }

    private static String cabecalho() {
        return """
            [Script Info]
            PlayResY: 1080

            [V4+ Styles]
            Format: Name,Fontname,Fontsize,PrimaryColour,SecondaryColour,OutlineColour,BackColour,Bold,Italic,Underline,StrikeOut,ScaleX,ScaleY,Spacing,Angle,BorderStyle,Outline,Shadow,Alignment,MarginL,MarginR,MarginV,Encoding
            Style: Default,Arial,48,&H00FFFFFF,&H000000FF,&H00000000,&H96000000,0,0,0,0,100,100,0,0,1,2,1,2,30,30,30,1

            [Events]
            Format: Layer,Start,End,Style,Name,MarginL,MarginR,MarginV,Effect,Text
            """;
    }

    private static final class LogStreamSilencioso extends LogStreamService {
        @Override
        public void publicarLog(String canal, String mensagem) {
            // Testes unitarios nao precisam de SSE nem arquivo de log.
        }
    }
}
