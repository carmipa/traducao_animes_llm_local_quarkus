package org.traducao.projeto.trocaTipoLegenda.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda;
import org.traducao.projeto.traducao.infrastructure.legenda.EscritorLegendaAss;
import org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaAss;
import org.traducao.projeto.trocaTipoLegenda.domain.AuditoriaLegendaResultado;
import org.traducao.projeto.trocaTipoLegenda.domain.ResultadoGeralAuditoria;
import org.traducao.projeto.trocaTipoLegenda.domain.ResultadoTrocaFonte;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrocaTipoLegendaUseCaseTest {

    private TrocaTipoLegendaUseCase useCase;
    private Path tempDirBackups;

    // Stub simples do TelemetriaService para evitar NPEs decorrentes de SSE broadcast nos testes unitários
    private static class TelemetriaServiceStub extends TelemetriaService {
        boolean operacaoRegistrada = false;

        @Override
        public void init() {
            // override post-construct para evitar carregar telemetria compartilhada real do disco nos testes
        }

        @Override
        public synchronized void registrarOperacao(OperacaoTelemetria operacao) {
            this.operacaoRegistrada = true;
        }
    }

    private final TelemetriaServiceStub telemetriaStub = new TelemetriaServiceStub();

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        LeitorLegendaAss leitor = new LeitorLegendaAss();
        EscritorLegendaAss escritor = new EscritorLegendaAss();
        AuditoriaFontesService auditoriaService = new AuditoriaFontesService();
        
        useCase = new TrocaTipoLegendaUseCase(leitor, escritor, auditoriaService, telemetriaStub);
        
        // Mover o diretório de backups do teste para a pasta temporária
        tempDirBackups = tempDir.resolve("backups");
        Files.createDirectories(tempDirBackups);
    }

    @AfterEach
    void tearDown() throws IOException {
        // Limpar backups criados se necessário
        deleteDirectoryRecursively(Path.of("backups"));
        deleteDirectoryRecursively(Path.of("relatorios"));
    }

    private void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.exists(path)) {
            try (var walk = Files.walk(path)) {
                walk.sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            // Ignora falhas de exclusão nos testes
                        }
                    });
            }
        }
    }

    @Test
    void escanearEIdentificarProblemasNoLote(@TempDir Path tempDir) throws IOException {
        // Criar legendas de teste no diretório temporário
        criarLegendaDeTeste(tempDir, "legenda1.ass", ".VnBook-Antiqua");
        criarLegendaDeTeste(tempDir, "legenda2.ass", "Arial");

        ResultadoGeralAuditoria resultado = useCase.escanear(tempDir);

        assertEquals(2, resultado.totalArquivosAnalisados());
        assertEquals(1, resultado.totalComProblemas());
        
        List<AuditoriaLegendaResultado> arquivos = resultado.arquivos();
        assertEquals(2, arquivos.size());

        AuditoriaLegendaResultado arq1 = arquivos.stream()
            .filter(a -> a.arquivo().equals("legenda1.ass"))
            .findFirst().orElseThrow();
        assertTrue(arq1.temProblemas());
        assertEquals(".VnBook-Antiqua", arq1.fontes().get(1).fonteAtual());

        AuditoriaLegendaResultado arq2 = arquivos.stream()
            .filter(a -> a.arquivo().equals("legenda2.ass"))
            .findFirst().orElseThrow();
        assertFalse(arq2.temProblemas());
    }

    @Test
    void aplicarSubstituicaoComBackupERelatorio(@TempDir Path tempDir) throws IOException {
        criarLegendaDeTeste(tempDir, "legenda1.ass", ".VnBook-Antiqua");
        criarLegendaDeTeste(tempDir, "legenda2.ass", "Arial");

        ResultadoTrocaFonte resultado = useCase.aplicar(tempDir);

        assertEquals(2, resultado.totalAnalisados());
        assertEquals(1, resultado.totalAlterados());
        assertEquals(1, resultado.totalSubstituicoes());
        assertNotNull(resultado.pastaBackup());
        assertNotNull(resultado.caminhoRelatorioJson());

        // Verificar que o arquivo corrigido agora contém a nova fonte
        String conteudoCorrigido = Files.readString(tempDir.resolve("legenda1.ass"), StandardCharsets.UTF_8);
        assertTrue(conteudoCorrigido.contains("Book Antiqua"));
        assertFalse(conteudoCorrigido.contains(".VnBook-Antiqua"));

        // Verificar que o backup existe na pasta de backup
        Path pastaBackup = Path.of(resultado.pastaBackup());
        assertTrue(Files.exists(pastaBackup.resolve("legenda1.ass")));
        String conteudoBackup = Files.readString(pastaBackup.resolve("legenda1.ass"), StandardCharsets.UTF_8);
        assertTrue(conteudoBackup.contains(".VnBook-Antiqua")); // deve ter a fonte original problemática

        // Verificar telemetria
        assertTrue(telemetriaStub.operacaoRegistrada);
    }

    private void criarLegendaDeTeste(Path pasta, String nome, String fonte) throws IOException {
        String cabecalho = "[Script Info]\n" +
            "Title: Test Legend\n" +
            "Script Type: v4.00+\n\n" +
            "[V4+ Styles]\n" +
            "Format: Name, Fontname\n" +
            "Style: Default,Arial\n" +
            "Style: Dialogue," + fonte + "\n\n" +
            "[Events]\n" +
            "Format: Layer, Start, End, Style, Text\n" +
            "Dialogue: 0,0:00:01.00,0:00:03.00,Dialogue,Olá Mundo\n";

        Files.writeString(pasta.resolve(nome), cabecalho, StandardCharsets.UTF_8);
    }
}
