package org.traducao.projeto.raspagemCorrecao.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Cobre o contrato tipado da raspagem sem tocar na rede: substitui o transporte
 * HTTP ({@code executarGet}) por respostas canônicas e verifica o mapeamento de
 * cada desfecho para {@link StatusRaspagem}.
 */
class GoogleTranslateScraperTest {

    private static GoogleTranslateScraper comResposta(int status, String corpo) {
        return new GoogleTranslateScraper(new ObjectMapper()) {
            @Override
            protected RespostaHttp executarGet(String url) {
                return new RespostaHttp(status, corpo);
            }
        };
    }

    private static GoogleTranslateScraper comIOException() {
        return new GoogleTranslateScraper(new ObjectMapper()) {
            @Override
            protected RespostaHttp executarGet(String url) throws IOException {
                throw new IOException("timeout simulado");
            }
        };
    }

    @Test
    void sucessoQuandoTraducaoDifereDoOriginal() {
        ResultadoRaspagem r = comResposta(200, "[[[\"Ola\",\"Hello\"]]]").traduzir("Hello");
        assertEquals(StatusRaspagem.SUCESSO, r.status());
        assertEquals("Ola", r.texto());
    }

    @Test
    void semAlteracaoQuandoGoogleDevolveIgualAoOriginal() {
        ResultadoRaspagem r = comResposta(200, "[[[\"Hello\",\"Hello\"]]]").traduzir("Hello");
        assertEquals(StatusRaspagem.SEM_ALTERACAO, r.status());
        assertEquals("Hello", r.texto());
    }

    @Test
    void falhaTransitoriaEm429() {
        assertEquals(StatusRaspagem.FALHA_TRANSITORIA, comResposta(429, "").traduzir("Hello").status());
    }

    @Test
    void falhaTransitoriaEm503() {
        assertEquals(StatusRaspagem.FALHA_TRANSITORIA, comResposta(503, "").traduzir("Hello").status());
    }

    @Test
    void respostaInvalidaEmHttpNaoTransitorio() {
        assertEquals(StatusRaspagem.RESPOSTA_INVALIDA, comResposta(404, "").traduzir("Hello").status());
    }

    @Test
    void falhaTransitoriaEmErroDeRede() {
        assertEquals(StatusRaspagem.FALHA_TRANSITORIA, comIOException().traduzir("Hello").status());
    }

    @Test
    void respostaInvalidaComJsonQuebrado() {
        assertEquals(StatusRaspagem.RESPOSTA_INVALIDA, comResposta(200, "isto nao e json").traduzir("Hello").status());
    }

    @Test
    void tagCorrompidaQuandoTagAssSePerde() {
        ResultadoRaspagem r = comResposta(200, "[[[\"Ola\",\"x\"]]]").traduzir("{\\i1}Hello");
        assertEquals(StatusRaspagem.TAG_CORROMPIDA, r.status());
        assertEquals("{\\i1}Hello", r.texto());
    }

    @Test
    void sucessoPreservandoTagAss() {
        ResultadoRaspagem r = comResposta(200, "[[[\"[T0] Ola\",\"x\"]]]").traduzir("{\\i1}Hello");
        assertEquals(StatusRaspagem.SUCESSO, r.status());
        assertEquals("{\\i1}Ola", r.texto());
    }

    @Test
    void textoVazioNaoChamaTransporteERetornaSemAlteracao() {
        ResultadoRaspagem r = comResposta(200, "irrelevante").traduzir("   ");
        assertEquals(StatusRaspagem.SEM_ALTERACAO, r.status());
        assertEquals("   ", r.texto());
    }
}
