package org.traducao.projeto.auditorConteudoLegendas.application;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.auditorConteudoLegendas.application.TelemetriaAuditoriaService;
import org.traducao.projeto.auditorConteudoLegendas.domain.AuditoriaException;
import org.traducao.projeto.auditorConteudoLegendas.domain.RelatorioAuditoriaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.support.AssAuditoriaFixtures;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class AuditorConteudoUseCaseTest {

    @Inject
    AuditorConteudoUseCase useCase;

    @Test
    void auditarArquivoLimpoGeraRelatorioJson(@TempDir Path tempDir) throws IOException {
        Path original = tempDir.resolve("ep01_eng.ass");
        Path traduzido = tempDir.resolve("ep01_pt.ass");
        AssAuditoriaFixtures.escreverParLimpo(original, traduzido);

        RelatorioAuditoriaConteudo relatorio = useCase.auditar(original, traduzido);

        assertTrue(relatorio.isLimpo());
        assertEquals(4, relatorio.getRegrasExecutadas());
        assertTrue(relatorio.getDuracaoMs() >= 0);
        assertNotNull(relatorio.getCaminhoRelatorioJson());
        assertTrue(Files.exists(Path.of(relatorio.getCaminhoRelatorioJson())));
    }

    @Test
    void auditarDetectaAnomaliasCriticas(@TempDir Path tempDir) throws IOException {
        Path original = tempDir.resolve("ep02_eng.ass");
        Path traduzido = tempDir.resolve("ep02_pt.ass");
        AssAuditoriaFixtures.escreverParComQuebraExcessiva(original, traduzido);

        RelatorioAuditoriaConteudo relatorio = useCase.auditar(original, traduzido);

        assertFalse(relatorio.isLimpo());
        assertTrue(relatorio.getAnomalias().size() >= 1);
    }

    @Test
    void auditarArquivoInexistenteLancaAuditoriaException(@TempDir Path tempDir) throws IOException {
        Path original = tempDir.resolve("ausente.ass");
        Path traduzido = tempDir.resolve("ep03_pt.ass");
        AssAuditoriaFixtures.escreverParLimpo(tempDir.resolve("ref_eng.ass"), traduzido);

        AuditoriaException ex = assertThrows(AuditoriaException.class,
            () -> useCase.auditar(original, traduzido));
        assertTrue(ex.getMessage().contains("nao encontrado"));
    }
}
