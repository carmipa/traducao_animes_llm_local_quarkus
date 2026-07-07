package org.traducao.projeto.correcaoLegendas.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.correcaoLegendas.domain.CorrecaoLegendasRelatorioJson;
import org.traducao.projeto.correcaoLegendas.infrastructure.CorrecaoLegendasLogPersistencia;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.traducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.traducao.application.ValidadorTraducaoService;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;
import org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto;
import org.traducao.projeto.traducao.infrastructure.legenda.EscritorLegendaAss;
import org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaAss;
import org.traducao.projeto.traducao.infrastructure.legenda.MascaradorTags;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorrigirLegendasUseCaseTest {

    @TempDir
    Path tempDir;

    @Test
    void cacheSemCorrecaoNaoReaplicaTextoOriginalEmIngles() throws IOException {
        Path original = tempDir.resolve("episodio.ass");
        Path traduzido = tempDir.resolve("episodio_PT-BR.ass");
        Files.writeString(original, ass(
            "{\\pos(10,10)}HELLO",
            "{\\pos(20,20)}HELLO"
        ));
        Files.writeString(traduzido, ass(
            "{\\pos(10,10)}Ola",
            "{\\pos(20,20)}Ola"
        ));

        CorretorFake corretor = new CorretorFake();
        CorrigirLegendasUseCase useCase = new CorrigirLegendasUseCase(
            new LeitorLegendaAss(),
            new EscritorLegendaAss(),
            new SanitizadorTagsService(),
            corretor,
            new GerenciadorContexto(List.of(new ContextoFake())),
            new TelemetriaFake(),
            new LogPersistenciaFake(),
            new DetectorEfeitoKaraokeService(),
            new TradutorProperties(),
            new MascaradorTags(),
            new ProtecaoLegendaAssService()
        );

        useCase.corrigirPasta(tempDir, tempDir, "teste");

        String conteudoFinal = Files.readString(traduzido);
        assertTrue(conteudoFinal.contains("{\\pos(10,10)}Ola"));
        assertTrue(conteudoFinal.contains("{\\pos(20,20)}Ola"));
        assertEquals(1, corretor.chamadas);
    }

    private String ass(String primeiraFala, String segundaFala) {
        return """
            [Script Info]
            Title: Teste

            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:01.00,0:00:02.00,Default,,0,0,0,,%s
            Dialogue: 0,0:00:03.00,0:00:04.00,Default,,0,0,0,,%s
            """.formatted(primeiraFala, segundaFala);
    }

    private static class CorretorFake extends CorretorTraducaoLlmService {
        int chamadas;

        CorretorFake() {
            super(null, null, null, null);
        }

        @Override
        public Optional<String> corrigirSeNecessario(String originalEn, String traduzidoAtual) {
            chamadas++;
            return Optional.empty();
        }
    }

    private static class ContextoFake implements ProvedorContexto {
        @Override
        public String getId() {
            return "teste";
        }

        @Override
        public String getNomeExibicao() {
            return "Teste";
        }

        @Override
        public String obterPromptSistema() {
            return "Prompt de teste.";
        }
    }

    private static class TelemetriaFake extends TelemetriaService {
        @Override
        public synchronized void registrarOperacao(OperacaoTelemetria operacao) {
        }

        @Override
        public synchronized Path salvar(Path pastaRelatorios) {
            return pastaRelatorios;
        }
    }

    private static class LogPersistenciaFake extends CorrecaoLegendasLogPersistencia {
        @Override
        public Path salvarRelatorioJson(Path pastaEntrada, CorrecaoLegendasRelatorioJson relatorio) {
            return pastaEntrada.resolve("relatorio-fake.json");
        }
    }
}
