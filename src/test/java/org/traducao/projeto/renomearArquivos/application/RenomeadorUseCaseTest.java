package org.traducao.projeto.renomearArquivos.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.renomearArquivos.domain.OperacaoRenomeacao;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.traducao.presentation.web.LogStreamService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RenomeadorUseCaseTest {

    RenomeadorUseCase renomeadorUseCase;
    private Path tempDir;
    private boolean telemetriaChamada = false;

    class MockTelemetriaService extends TelemetriaService {
        @Override
        public void registrarArquivoSanitizado() {
            telemetriaChamada = true;
        }
    }

    class MockLogStream extends LogStreamService {
        @Override
        public void publicarLog(String canal, String mensagem) {
            // Ignora
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("test_renomeador");
        renomeadorUseCase = new RenomeadorUseCase();
        renomeadorUseCase.telemetriaService = new MockTelemetriaService();
        renomeadorUseCase.logStream = new MockLogStream();
        renomeadorUseCase.objectMapper = new ObjectMapper();
        telemetriaChamada = false;
    }

    @AfterEach
    void tearDown() throws IOException {
        if (renomeadorUseCase != null && tempDir != null) {
            Files.deleteIfExists(renomeadorUseCase.resolverArquivoUndo(tempDir));
        }
        Files.walk(tempDir)
            .sorted((a, b) -> b.compareTo(a))
            .forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
    }

    @Test
    void testSimularRenomeacao() throws IOException {
        Path arquivo1 = tempDir.resolve("[SubsPlease] Anime Teste - 01 (1080p).mkv");
        Files.createFile(arquivo1);

        List<OperacaoRenomeacao.ItemRenomeado> simulados = renomeadorUseCase.simularRenomeacao(tempDir, "Anime Top");

        assertEquals(1, simulados.size());
        assertEquals("[SubsPlease] Anime Teste - 01 (1080p).mkv", simulados.get(0).nomeOriginal());
        assertEquals("Anime Top - S01E01.mkv", simulados.get(0).nomeNovo());
        
        // Verifica que o arquivo não foi alterado de verdade na simulação
        assertTrue(Files.exists(arquivo1));
    }

    @Test
    void ignoraArquivosQueNaoSaoVideo() throws IOException {
        Path video = tempDir.resolve("[SubsPlease] Anime Teste - 03 (1080p).mp4");
        Path legenda = tempDir.resolve("[SubsPlease] Anime Teste - 03 (1080p).ass");
        Path texto = tempDir.resolve("Anime Teste - 03.txt");
        Files.createFile(video);
        Files.createFile(legenda);
        Files.createFile(texto);

        List<OperacaoRenomeacao.ItemRenomeado> simulados = renomeadorUseCase.simularRenomeacao(tempDir, "Anime Top");

        assertEquals(1, simulados.size());
        assertEquals("[SubsPlease] Anime Teste - 03 (1080p).mp4", simulados.get(0).nomeOriginal());
        assertEquals("Anime Top - S01E03.mp4", simulados.get(0).nomeNovo());
    }

    @Test
    void simulaNomesDeTrackerComUnderlineSemConfundirTitulo86ComEpisodio() throws IOException {
        for (int episodio = 1; episodio <= 11; episodio++) {
            Files.createFile(tempDir.resolve(
                String.format("[DB]86_-_%02d_(Dual Audio_10bit_BD1080p_x265)_PTBR.mkv", episodio)
            ));
        }

        List<OperacaoRenomeacao.ItemRenomeado> simulados =
            renomeadorUseCase.simularRenomeacao(tempDir, "86 (Eighty-Six) - Temp1");

        assertEquals(11, simulados.size());
        assertEquals("[DB]86_-_01_(Dual Audio_10bit_BD1080p_x265)_PTBR.mkv", simulados.get(0).nomeOriginal());
        assertEquals("86 (Eighty-Six) - Temp1 - S01E01.mkv", simulados.get(0).nomeNovo());
        assertEquals("[DB]86_-_11_(Dual Audio_10bit_BD1080p_x265)_PTBR.mkv", simulados.get(10).nomeOriginal());
        assertEquals("86 (Eighty-Six) - Temp1 - S01E11.mkv", simulados.get(10).nomeNovo());
    }

    @Test
    void renomeiaFilmeUnicoSemEpisodioParaNomePadrao() throws IOException {
        Path filme = tempDir.resolve("[2ndfire]Mobile_Suit_Gundam_Narrative[BD][1080p][AV1][10bit][981A36A1].mkv");
        Files.createFile(filme);

        List<OperacaoRenomeacao.ItemRenomeado> simulados =
            renomeadorUseCase.simularRenomeacao(tempDir, "Mobile Suit Gundam Narrative");

        assertEquals(1, simulados.size());
        assertEquals("[2ndfire]Mobile_Suit_Gundam_Narrative[BD][1080p][AV1][10bit][981A36A1].mkv", simulados.get(0).nomeOriginal());
        assertEquals("Mobile Suit Gundam Narrative.mkv", simulados.get(0).nomeNovo());
    }

    @Test
    void ignoraMultiplosVideosSemEpisodioParaEvitarColisaoDeFilmes() throws IOException {
        Files.createFile(tempDir.resolve("[Grupo] Filme Um [BD 1080p].mkv"));
        Files.createFile(tempDir.resolve("[Grupo] Filme Dois [BD 1080p].mkv"));

        List<OperacaoRenomeacao.ItemRenomeado> simulados =
            renomeadorUseCase.simularRenomeacao(tempDir, "Nome Padrao");

        assertTrue(simulados.isEmpty());
    }

    @Test
    void testAplicarRenomeacaoEBackup() throws IOException {
        Path arquivo1 = tempDir.resolve("[SubsPlease] Anime Teste - 02 (1080p).mkv");
        Files.createFile(arquivo1);

        renomeadorUseCase.aplicarRenomeacao(tempDir, "Anime Top");

        // Verifica que o arquivo antigo não existe e o novo existe
        assertFalse(Files.exists(arquivo1));
        assertTrue(Files.exists(tempDir.resolve("Anime Top - S01E02.mkv")));

        // O manifesto de undo é operacional e deve ficar dentro do projeto,
        // nunca misturado na pasta de mídia que está sendo renomeada.
        Path backupAntigoNaPastaMidia = tempDir.resolve(".kronos_undo_renomeacao.json");
        Path manifestoProjeto = renomeadorUseCase.resolverArquivoUndo(tempDir);
        assertFalse(Files.exists(backupAntigoNaPastaMidia));
        assertTrue(Files.exists(manifestoProjeto));
        assertTrue(manifestoProjeto.startsWith(TelemetriaService.resolverPastaArtefatosOperacionais("renomear-arquivos")));

        assertTrue(telemetriaChamada);
    }

    @Test
    void reverterUsaManifestoSalvoDentroDoProjeto() throws IOException {
        Path arquivoOriginal = tempDir.resolve("[SubsPlease] Anime Teste - 04 (1080p).mkv");
        Path arquivoRenomeado = tempDir.resolve("Anime Top - S01E04.mkv");
        Path manifestoProjeto = renomeadorUseCase.resolverArquivoUndo(tempDir);
        Files.createFile(arquivoOriginal);

        renomeadorUseCase.aplicarRenomeacao(tempDir, "Anime Top");

        assertFalse(Files.exists(arquivoOriginal));
        assertTrue(Files.exists(arquivoRenomeado));
        assertTrue(Files.exists(manifestoProjeto));

        renomeadorUseCase.reverterRenomeacao(tempDir);

        assertTrue(Files.exists(arquivoOriginal));
        assertFalse(Files.exists(arquivoRenomeado));
        assertFalse(Files.exists(manifestoProjeto));
    }
}
