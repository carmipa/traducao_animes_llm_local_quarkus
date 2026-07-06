package org.traducao.projeto.limpaNomes.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.limpaNomes.domain.OperacaoRenomeacao;
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
    void testAplicarRenomeacaoEBackup() throws IOException {
        Path arquivo1 = tempDir.resolve("[SubsPlease] Anime Teste - 02 (1080p).mkv");
        Files.createFile(arquivo1);

        renomeadorUseCase.aplicarRenomeacao(tempDir, "Anime Top");

        // Verifica que o arquivo antigo não existe e o novo existe
        assertFalse(Files.exists(arquivo1));
        assertTrue(Files.exists(tempDir.resolve("Anime Top - S01E02.mkv")));

        // Verifica o backup JSON
        Path backupFile = tempDir.resolve(".kronos_undo_renomeacao.json");
        assertTrue(Files.exists(backupFile));

        assertTrue(telemetriaChamada);
    }
}
