package org.traducao.projeto.raspagemCorrecao.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Traduz texto via API pública do Google Translate (scraping), preservando
 * tags ASS mascaradas e quebras {@code \N}.
 */
@Component
public class GoogleTranslateScraper {

    private static final Logger log = LoggerFactory.getLogger(GoogleTranslateScraper.class);

    // Marcador [Tn]/[B] (com mutilações comuns de espaçamento/parênteses) que
    // sobrou depois da restauração das tags — sinal de resposta corrompida.
    private static final Pattern PADRAO_MARCADOR_RESIDUAL =
        Pattern.compile("(?i)[\\[(]\\s*[tb]\\s*\\d*\\s*[\\])]");

    private final ObjectMapper mapper;
    private final HttpClient httpClient;

    public GoogleTranslateScraper(ObjectMapper mapper) {
        this.mapper = mapper;
        this.httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public String traduzir(String textoOriginal) {
        List<String> tags = new ArrayList<>();
        Pattern patternTags = Pattern.compile("\\{[^}]+\\}");
        Matcher matcher = patternTags.matcher(textoOriginal);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            sb.append(textoOriginal, lastEnd, matcher.start());
            tags.add(matcher.group());
            sb.append(" [T").append(tags.size() - 1).append("] ");
            lastEnd = matcher.end();
        }
        sb.append(textoOriginal, lastEnd, textoOriginal.length());
        String textoMascarado = sb.toString();

        boolean temQuebra = textoMascarado.contains("\\N");
        if (temQuebra) {
            textoMascarado = textoMascarado.replace("\\N", " [B] ");
        }

        textoMascarado = textoMascarado.replaceAll("\\s+", " ").strip();

        if (textoMascarado.isEmpty()) {
            return textoOriginal;
        }

        try {
            String query = URLEncoder.encode(textoMascarado, StandardCharsets.UTF_8);
            String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=pt&dt=t&q="
                + query;

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                log.warn("Erro HTTP na chamada do Google Translate: {}", response.statusCode());
                return textoOriginal;
            }

            JsonNode root = mapper.readTree(response.body());
            JsonNode segments = root.get(0);
            StringBuilder resultadoTraduzido = new StringBuilder();

            if (segments != null && segments.isArray()) {
                for (JsonNode segment : segments) {
                    JsonNode text = segment.get(0);
                    if (text != null && !text.isNull()) {
                        resultadoTraduzido.append(text.asText());
                    }
                }
            }

            String traduzido = resultadoTraduzido.toString();

            if (traduzido.isBlank()) {
                log.warn("Resposta do Google Translate em formato inesperado (sem segmentos traduzíveis); mantendo texto original.");
                return textoOriginal;
            }

            if (temQuebra) {
                traduzido = traduzido.replaceAll("(?i)\\s*\\[b\\]\\s*", "\\\\N");
            }

            for (int i = 0; i < tags.size(); i++) {
                String pattern = "(?i)\\s*\\[t" + i + "\\]\\s*";
                traduzido = traduzido.replaceAll(pattern, Matcher.quoteReplacement(tags.get(i)));
            }

            traduzido = traduzido.replace("\\ N", "\\N").replace("\\ n", "\\N");

            // O Google às vezes mutila os marcadores ("[ T0 ]", "(T0)", "[b ]"...).
            // Nesse caso o replace acima não casa: sobraria marcador VISÍVEL na
            // legenda e/ou a tag ASS original seria perdida. Melhor devolver o
            // texto original intacto (os chamadores tratam igualdade como
            // "sem alteração") do que gravar uma linha corrompida.
            if (PADRAO_MARCADOR_RESIDUAL.matcher(traduzido).find()) {
                log.warn("Google Translate mutilou marcadores de tag/quebra; mantendo texto original: {}", traduzido);
                return textoOriginal;
            }
            for (String tag : tags) {
                if (!traduzido.contains(tag)) {
                    log.warn("Google Translate perdeu a tag ASS {}; mantendo texto original.", tag);
                    return textoOriginal;
                }
            }

            return traduzido;

        } catch (Exception e) {
            log.error("Erro na comunicação com a API do Google Translate: {}", e.getMessage());
            return textoOriginal;
        }
    }
}
