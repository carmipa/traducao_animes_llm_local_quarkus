package org.traducao.projeto.raspagemCorrecao.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.raspagemCorrecao.infrastructure.GoogleTranslateScraper;
import org.traducao.projeto.raspagemCorrecao.infrastructure.ResultadoRaspagem;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.traducao.application.DetectorTraducaoIdenticaService;
import org.traducao.projeto.traducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.traducao.application.ValidadorTraducaoService;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;
import org.traducao.projeto.traducao.infrastructure.cache.CacheManutencaoService;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;
import org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto;
import org.traducao.projeto.traducaoCorrige.application.ClassificadorEntradaCacheService;
import org.traducao.projeto.traducaoCorrige.application.ContextoManutencaoCacheService;
import org.traducao.projeto.traducaoCorrige.domain.EntradaAuditoriaCorrecaoCache;
import org.traducao.projeto.traducaoCorrige.domain.ResultadoManutencaoCache;
import org.traducao.projeto.traducaoCorrige.infrastructure.CorrecaoCacheAuditoria;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: prova a regressão central do menu — uma entrada vazia
 * produzida pela limpeza precisa ser preenchida pela contingência Google.
 *
 * <p>INVARIANTES DO DOMÍNIO: teste não acessa a internet nem grava telemetria no
 * projeto; cache versionado e proveniência permanecem intactos.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer ausência de tradução aplicada ou
 * alteração do envelope falha o teste.
 */
class CorrigirComGoogleUseCaseTest {

    @TempDir
    Path temp;

    @Test
    void preencheEntradaVaziaDeixadaPelaLimpeza() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        GerenciadorContexto contexto = new GerenciadorContexto(List.of(new ContextoTeste()));
        ClassificadorEntradaCacheService classificador = new ClassificadorEntradaCacheService(
            new DetectorTraducaoIdenticaService(contexto), new ValidadorTraducaoService(),
            new TradutorProperties(), new DetectorEfeitoKaraokeService(), new ProtecaoLegendaAssService());
        CacheManutencaoService cacheService = new CacheServiceTeste(mapper, temp.resolve("backups"));
        CorrigirComGoogleUseCase useCase = new CorrigirComGoogleUseCase(
            cacheService, classificador, new ContextoManutencaoCacheService(contexto),
            new GoogleStub(mapper), new AuditoriaStub(mapper), new TelemetriaStub());

        Path cache = temp.resolve("cache");
        Path arquivo = cache.resolve("ep.cache.json");
        Files.createDirectories(cache);
        Files.writeString(arquivo, """
            {"proveniencia":{"schemaVersion":1,"contextoId":"danmachi","contextoHash":"abc","modeloLlm":"gemma","idiomaOrigem":"en","idiomaDestino":"pt-br"},
             "entradas":[{"indice":1,"estilo":"Default","original":"Help!","traduzido":""}]}
            """);

        ResultadoManutencaoCache resultado = useCase.executar(cache, null);
        var salvo = mapper.readTree(arquivo.toFile());

        assertEquals("Ajude!", salvo.path("entradas").get(0).path("traduzido").asText());
        assertEquals("danmachi", salvo.path("proveniencia").path("contextoId").asText());
        assertEquals(1, resultado.itensCorrigidos());
        assertEquals("CONCLUIDO", resultado.status());
    }

    private static final class GoogleStub extends GoogleTranslateScraper {
        GoogleStub(ObjectMapper mapper) { super(mapper); }
        @Override public ResultadoRaspagem traduzir(String textoOriginal) { return ResultadoRaspagem.sucesso("Ajude!"); }
    }

    private static final class CacheServiceTeste extends CacheManutencaoService {
        private final Path backup;
        CacheServiceTeste(ObjectMapper mapper, Path backup) { super(mapper); this.backup = backup; }
        @Override public Sessao iniciarSessao(Path raizCache, String operacao) {
            return new Sessao(raizCache.toAbsolutePath().normalize(), backup.toAbsolutePath().normalize(), operacao);
        }
    }

    private static final class AuditoriaStub extends CorrecaoCacheAuditoria {
        AuditoriaStub(ObjectMapper mapper) { super(mapper); }
        @Override public synchronized void registrar(EntradaAuditoriaCorrecaoCache entrada) { }
    }

    private static final class TelemetriaStub extends TelemetriaService {
        @Override public synchronized void finalizarOperacao(
            OperacaoTelemetria operacao, Path pastaEntrada, String prefixoRelatorio, String conteudoRelatorio) { }
    }

    private static final class ContextoTeste implements ProvedorContexto {
        private static final String PROMPT = ContextoPrompt.montar("Teste", "Principais nomes: Bell Cranel.");
        @Override public String getId() { return "danmachi"; }
        @Override public String getNomeExibicao() { return "Teste"; }
        @Override public String obterPromptSistema() { return PROMPT; }
    }
}
