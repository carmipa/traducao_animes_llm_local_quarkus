package org.traducao.projeto.raspagemCorrecao.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cobre o contrato tipado e o retry curado sem tocar na rede: substitui o
 * transporte HTTP ({@code executarGet}) por respostas canônicas e anula a espera
 * ({@code dormir}). Verifica o mapeamento de cada desfecho para
 * {@link StatusRaspagem} e que só a falha transitória é retentada.
 */
class GoogleTranslateScraperTest {

    /**
     * Scraper com transporte controlado e sem espera real: conta as chamadas de
     * rede e registra a última espera pedida (para conferir o Retry-After).
     */
    private static final class ScraperFalso extends GoogleTranslateScraper {
        final AtomicInteger chamadas = new AtomicInteger();
        volatile long ultimaEsperaMs = -1;
        private final IntFunction<Object> porTentativa; // GoogleTranslateScraper.RespostaHttp ou IOException (1-based)

        ScraperFalso(IntFunction<Object> porTentativa) {
            super(new ObjectMapper());
            this.porTentativa = porTentativa;
        }

        @Override
        protected GoogleTranslateScraper.RespostaHttp executarGet(String url) throws IOException {
            Object r = porTentativa.apply(chamadas.incrementAndGet());
            if (r instanceof IOException io) {
                throw io;
            }
            return (GoogleTranslateScraper.RespostaHttp) r;
        }

        @Override
        protected void dormir(long ms) {
            this.ultimaEsperaMs = ms; // registra, mas não dorme
        }
    }

    private static ScraperFalso sempre(int status, String corpo) {
        return new ScraperFalso(n -> new GoogleTranslateScraper.RespostaHttp(status, corpo, 0));
    }

    private static ScraperFalso sempreErroRede() {
        return new ScraperFalso(n -> new IOException("timeout simulado"));
    }

    private static ScraperFalso sequencia(Object... porTentativa) {
        return new ScraperFalso(n -> porTentativa[Math.min(n, porTentativa.length) - 1]);
    }

    private static GoogleTranslateScraper.RespostaHttp resp(int status, String corpo) {
        return new GoogleTranslateScraper.RespostaHttp(status, corpo, 0);
    }

    private static GoogleTranslateScraper.RespostaHttp resp(int status, String corpo, long retryAfterMs) {
        return new GoogleTranslateScraper.RespostaHttp(status, corpo, retryAfterMs);
    }

    // ----- mapeamento de desfechos -----

    @Test
    void sucessoQuandoTraducaoDifereDoOriginal() {
        ResultadoRaspagem r = sempre(200, "[[[\"Ola\",\"Hello\"]]]").traduzir("Hello");
        assertEquals(StatusRaspagem.SUCESSO, r.status());
        assertEquals("Ola", r.texto());
    }

    @Test
    void semAlteracaoQuandoGoogleDevolveIgualAoOriginal() {
        ResultadoRaspagem r = sempre(200, "[[[\"Hello\",\"Hello\"]]]").traduzir("Hello");
        assertEquals(StatusRaspagem.SEM_ALTERACAO, r.status());
        assertEquals("Hello", r.texto());
    }

    @Test
    void respostaInvalidaEmHttpNaoTransitorio() {
        assertEquals(StatusRaspagem.RESPOSTA_INVALIDA, sempre(404, "").traduzir("Hello").status());
    }

    @Test
    void falhaTransitoriaEmErroDeRede() {
        assertEquals(StatusRaspagem.FALHA_TRANSITORIA, sempreErroRede().traduzir("Hello").status());
    }

    @Test
    void respostaInvalidaComJsonQuebrado() {
        assertEquals(StatusRaspagem.RESPOSTA_INVALIDA, sempre(200, "isto nao e json").traduzir("Hello").status());
    }

    @Test
    void tagCorrompidaQuandoTagAssSePerde() {
        ResultadoRaspagem r = sempre(200, "[[[\"Ola\",\"x\"]]]").traduzir("{\\i1}Hello");
        assertEquals(StatusRaspagem.TAG_CORROMPIDA, r.status());
        assertEquals("{\\i1}Hello", r.texto());
    }

    @Test
    void sucessoPreservandoTagAss() {
        ResultadoRaspagem r = sempre(200, "[[[\"[T0] Ola\",\"x\"]]]").traduzir("{\\i1}Hello");
        assertEquals(StatusRaspagem.SUCESSO, r.status());
        assertEquals("{\\i1}Ola", r.texto());
    }

    @Test
    void textoVazioNaoChamaTransporteERetornaSemAlteracao() {
        ScraperFalso s = sempre(200, "irrelevante");
        ResultadoRaspagem r = s.traduzir("   ");
        assertEquals(StatusRaspagem.SEM_ALTERACAO, r.status());
        assertEquals("   ", r.texto());
        assertEquals(0, s.chamadas.get());
    }

    // ----- retry curado -----

    @Test
    void retentaAteLimiteEmFalhaTransitoria() {
        ScraperFalso s = sempre(503, "");
        assertEquals(StatusRaspagem.FALHA_TRANSITORIA, s.traduzir("Hello").status());
        assertEquals(5, s.chamadas.get()); // 1 inicial + 4 retries
    }

    @Test
    void naoRetentaEmRespostaInvalida() {
        ScraperFalso s = sempre(404, "");
        s.traduzir("Hello");
        assertEquals(1, s.chamadas.get());
    }

    @Test
    void naoRetentaEmTagCorrompida() {
        ScraperFalso s = sempre(200, "[[[\"Ola\",\"x\"]]]");
        s.traduzir("{\\i1}Hello");
        assertEquals(1, s.chamadas.get());
    }

    @Test
    void recuperaNaSegundaTentativa() {
        ScraperFalso s = sequencia(resp(503, ""), resp(200, "[[[\"Ola\",\"Hello\"]]]"));
        ResultadoRaspagem r = s.traduzir("Hello");
        assertEquals(StatusRaspagem.SUCESSO, r.status());
        assertEquals("Ola", r.texto());
        assertEquals(2, s.chamadas.get());
    }

    @Test
    void honraRetryAfterNaEspera() {
        ScraperFalso s = sequencia(resp(503, "", 5000), resp(200, "[[[\"Ola\",\"Hello\"]]]"));
        ResultadoRaspagem r = s.traduzir("Hello");
        assertEquals(StatusRaspagem.SUCESSO, r.status());
        assertEquals(5000L, s.ultimaEsperaMs); // usou o Retry-After, não o backoff
    }

    // ----- Testes do Parser de Retry-After -----

    @Test
    void parseRetryAfterSegundos() {
        long ms = GoogleTranslateScraper.parseRetryAfter("120");
        assertEquals(120000L, ms);
    }

    @Test
    void parseRetryAfterHttpDateFuturo() {
        ZonedDateTime futuro = ZonedDateTime.now(java.time.ZoneId.of("GMT")).plusMinutes(2);
        String httpDate = futuro.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH));
        
        long ms = GoogleTranslateScraper.parseRetryAfter(httpDate);
        // Deve ser aproximadamente 2 minutos (120000 ms), com margem de erro pequena para o tempo de CPU
        assertTrue(ms > 110000L && ms <= 120000L, "Espera aproximada de 2 minutos. Recebido: " + ms);
    }

    @Test
    void parseRetryAfterHttpDatePassado() {
        ZonedDateTime passado = ZonedDateTime.now(java.time.ZoneId.of("GMT")).minusMinutes(5);
        String httpDate = passado.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH));
        
        long ms = GoogleTranslateScraper.parseRetryAfter(httpDate);
        assertEquals(0L, ms);
    }

    @Test
    void parseRetryAfterInvalido() {
        assertEquals(0L, GoogleTranslateScraper.parseRetryAfter(""));
        assertEquals(0L, GoogleTranslateScraper.parseRetryAfter("   "));
        assertEquals(0L, GoogleTranslateScraper.parseRetryAfter(null));
        assertEquals(0L, GoogleTranslateScraper.parseRetryAfter("texto aleatorio"));
        assertEquals(0L, GoogleTranslateScraper.parseRetryAfter("12a"));
    }
}
