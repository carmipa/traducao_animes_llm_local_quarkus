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

    /**
     * PROPÓSITO DE NEGÓCIO: confirma que uma auditoria ASS limpa expõe formato
     * e gera o dataset JSON esperado.
     * <p>INVARIANTES DO DOMÍNIO: os dois arquivos são ASS válidos e equivalentes.
     * <p>COMPORTAMENTO EM CASO DE FALHA: metadado ausente ou persistência
     * inexistente reprova o teste.
     */
    @Test
    void auditarArquivoLimpoGeraRelatorioJson(@TempDir Path tempDir) throws IOException {
        Path original = tempDir.resolve("ep01_eng.ass");
        Path traduzido = tempDir.resolve("ep01_pt.ass");
        AssAuditoriaFixtures.escreverParLimpo(original, traduzido);

        RelatorioAuditoriaConteudo relatorio = useCase.auditar(original, traduzido);

        assertTrue(relatorio.isLimpo());
        assertEquals("ASS", relatorio.getFormatoOriginal());
        assertEquals("ASS", relatorio.getFormatoTraduzido());
        assertEquals(5, relatorio.getRegrasExecutadas());
        assertTrue(relatorio.getDuracaoMs() >= 0);
        assertNotNull(relatorio.getCaminhoRelatorioJson());
        assertTrue(Files.exists(Path.of(relatorio.getCaminhoRelatorioJson())));
        String json = Files.readString(Path.of(relatorio.getCaminhoRelatorioJson()));
        assertTrue(json.contains("\"formatoOriginal\" : \"ASS\""));
        assertTrue(json.contains("\"formatoTraduzido\" : \"ASS\""));
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

    /**
     * PROPÓSITO DE NEGÓCIO: comprova que a Opção 3 aceita SubRip e informa o
     * formato no relatório em vez de apresentar apenas o nome do arquivo.
     * <p>INVARIANTES DO DOMÍNIO: os dois artefatos são SRT válidos e o parser
     * preserva a correspondência dos blocos.
     * <p>COMPORTAMENTO EM CASO DE FALHA: formato ausente, rejeição do SRT ou
     * anomalia artificial reprova o teste.
     */
    @Test
    void auditarSrtInformaFormatoNoRelatorio(@TempDir Path tempDir) throws IOException {
        Path original = tempDir.resolve("ep04_eng.srt");
        Path traduzido = tempDir.resolve("ep04_pt.srt");
        Files.writeString(original, "1\n00:00:01,000 --> 00:00:03,000\nHello.\n");
        Files.writeString(traduzido, "1\n00:00:01,000 --> 00:00:03,000\nOlá.\n");

        RelatorioAuditoriaConteudo relatorio = useCase.auditar(original, traduzido);

        assertTrue(relatorio.isLimpo());
        assertEquals("SRT", relatorio.getFormatoOriginal());
        assertEquals("SRT", relatorio.getFormatoTraduzido());
        assertNotNull(relatorio.getCaminhoRelatorioJson());
        String json = Files.readString(Path.of(relatorio.getCaminhoRelatorioJson()));
        assertTrue(json.contains("\"formatoOriginal\" : \"SRT\""));
        assertTrue(json.contains("\"formatoTraduzido\" : \"SRT\""));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: evita que um arquivo desconhecido seja classificado
     * silenciosamente como legenda válida no relatório.
     * <p>INVARIANTES DO DOMÍNIO: apenas ASS, SSA e SRT são formatos suportados.
     * <p>COMPORTAMENTO EM CASO DE FALHA: o caso de uso retorna erro didático
     * contendo os formatos aceitos.
     */
    @Test
    void rejeitaFormatoDesconhecidoComMensagemDidatica(@TempDir Path tempDir) throws IOException {
        Path original = tempDir.resolve("ep05_eng.txt");
        Path traduzido = tempDir.resolve("ep05_pt.srt");
        Files.writeString(original, "texto");
        Files.writeString(traduzido, "1\n00:00:01,000 --> 00:00:03,000\nTexto.\n");

        AuditoriaException ex = assertThrows(AuditoriaException.class,
            () -> useCase.auditar(original, traduzido));

        assertTrue(ex.getMessage().contains("ASS, SSA ou SRT"));
    }
}
