package org.traducao.projeto.remuxer.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.remuxer.domain.RemuxTarefa;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MapeadorMidiaServiceTest {

    private final MapeadorMidiaService mapeador = new MapeadorMidiaService();

    @Test
    void pareiaCorretamenteVideosELegendasComNomesDeAnime(@TempDir Path tempDir) throws IOException {
        Path pastaVideos = tempDir.resolve("videos");
        Path pastaLegendas = tempDir.resolve("legendas");
        Path pastaSaida = tempDir.resolve("saida");

        Files.createDirectories(pastaVideos);
        Files.createDirectories(pastaLegendas);
        Files.createDirectories(pastaSaida);

        // Criar arquivos de vídeo MKV com padrão "EpsXX" (como nos arquivos de 86 do usuário)
        Files.createFile(pastaVideos.resolve("86-Eighty-Six-Eps01-Ptbr_PTBR.mkv"));
        Files.createFile(pastaVideos.resolve("86-Eighty-Six-Eps02-Ptbr_PTBR.mkv"));

        // Criar arquivos de legenda ASS com padrão "_-_XX" e colchetes
        Files.createFile(pastaLegendas.resolve("[DB]86_-_01_(Dual Audio_10bit_BD1080p_x265)_PTBR_PT-BR.ass"));
        Files.createFile(pastaLegendas.resolve("[DB]86_-_02_(Dual Audio_10bit_BD1080p_x265)_PTBR_PT-BR.ass"));

        List<RemuxTarefa> fila = mapeador.construirFilaProcessamento(pastaVideos, pastaLegendas, pastaSaida);

        assertNotNull(fila);
        assertEquals(2, fila.size());

        // Validar pareamento do episódio 1
        RemuxTarefa tarefa1 = fila.stream()
            .filter(t -> t.nomeVideo().contains("Eps01"))
            .findFirst()
            .orElse(null);
        assertNotNull(tarefa1);
        assertEquals("[DB]86_-_01_(Dual Audio_10bit_BD1080p_x265)_PTBR_PT-BR.ass", tarefa1.caminhoLegenda().getFileName().toString());
        assertEquals("86-Eighty-Six-Eps01-Ptbr_PTBR.mkv", tarefa1.caminhoVideo().getFileName().toString());

        // Validar pareamento do episódio 2
        RemuxTarefa tarefa2 = fila.stream()
            .filter(t -> t.nomeVideo().contains("Eps02"))
            .findFirst()
            .orElse(null);
        assertNotNull(tarefa2);
        assertEquals("[DB]86_-_02_(Dual Audio_10bit_BD1080p_x265)_PTBR_PT-BR.ass", tarefa2.caminhoLegenda().getFileName().toString());
        assertEquals("86-Eighty-Six-Eps02-Ptbr_PTBR.mkv", tarefa2.caminhoVideo().getFileName().toString());
    }

    @Test
    void pareiaPorArquivoUnicoQuandoHouverApenasUmDeCada(@TempDir Path tempDir) throws IOException {
        Path pastaVideos = tempDir.resolve("videos");
        Path pastaLegendas = tempDir.resolve("legendas");
        Path pastaSaida = tempDir.resolve("saida");

        Files.createDirectories(pastaVideos);
        Files.createDirectories(pastaLegendas);
        Files.createDirectories(pastaSaida);

        // Nomes completamente diferentes, mas apenas 1 de cada na pasta
        Files.createFile(pastaVideos.resolve("Mobile_Suit_Gundam_Narrative_720p.mkv"));
        Files.createFile(pastaLegendas.resolve("[Pinkusub]Gundam_Narrative_PT-BR.ass"));

        List<RemuxTarefa> fila = mapeador.construirFilaProcessamento(pastaVideos, pastaLegendas, pastaSaida);

        assertNotNull(fila);
        assertEquals(1, fila.size());
        assertEquals("[Pinkusub]Gundam_Narrative_PT-BR.ass", fila.get(0).caminhoLegenda().getFileName().toString());
    }
}
