package org.traducao.projeto.traducao.application;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Decide se uma fala pode legitimamente permanecer idêntica ao original (nomes
 * próprios, números, siglas, termos de lore) ou se a igualdade é sinal de que o
 * LLM simplesmente devolveu a fala sem traduzir.
 */
@Service
public class DetectorTraducaoIdenticaService {

    private static final Pattern PADRAO_REMOVE_TAGS_ASS = Pattern.compile("\\{[^}]+}");

    private static final Set<String> PALAVRAS_INGLES_COMUNS = Set.of(
        "hello", "hi", "hey", "goodbye", "bye", "yes", "no", "yeah", "yep", "nope",
        "thanks", "thank", "sorry", "please", "wait", "stop", "go", "come", "run",
        "what", "why", "who", "where", "when", "how", "right", "okay", "ok", "fine"
    );

    private static final Set<String> TERMOS_DE_LORE = Set.of(
        "bell", "hestia", "ais", "orario", "dungeon", "falna", "familia",
        "zeon", "gundam", "zaku", "alex", "kampfer", "axis", "aeug", "titans",
        "macross", "zentradi", "meltrandi", "valkyrie", "marduk",
        "gauna", "sidonia", "legion", "juggernaut"
    );

    private static final List<String> TERMOS_IGNORADOS_MULTIPALAVRA = List.of(
        "fire bolt", "argo vesta", "caelus hildr", "hildrsleif", "dios aedes vesta",
        "vana freya", "vana seith", "vana seith.", "zeo gullveig", "hildis vini",
        "agallis arvesynth", "remiste felis", "uchide no kozuchi", "feles cruz",
        "dubh daol", "zekka", "gralineze fromel", "gokoh", "astrea record"
    );

    public boolean deveManterIdentico(String texto) {
        if (texto == null) {
            return true;
        }
        String textoLimpo = PADRAO_REMOVE_TAGS_ASS.matcher(texto).replaceAll("").strip();
        textoLimpo = textoLimpo.replaceAll("[^\\w\\s\\d]", "").strip();

        if (textoLimpo.isEmpty()) {
            return true;
        }

        String[] palavras = textoLimpo.split("\\s+");
        if (palavras.length == 1) {
            return deveManterPalavraUnicaIdentica(textoLimpo);
        }

        if (palavras.length == 2
            && Character.isUpperCase(palavras[0].charAt(0))
            && Character.isUpperCase(palavras[1].charAt(0))) {
            return true;
        }

        return TERMOS_IGNORADOS_MULTIPALAVRA.contains(textoLimpo.toLowerCase());
    }

    private boolean deveManterPalavraUnicaIdentica(String textoLimpo) {
        if (textoLimpo.matches("\\d+")) {
            return true;
        }
        if (textoLimpo.length() > 1 && textoLimpo.equals(textoLimpo.toUpperCase())) {
            return true;
        }

        String minusculo = textoLimpo.toLowerCase();
        if (PALAVRAS_INGLES_COMUNS.contains(minusculo)) {
            return false;
        }

        return TERMOS_DE_LORE.contains(minusculo);
    }

    /**
     * true quando a "tradução" só repete o original em inglês (ignorando tags ASS
     * e quebras de linha) e isso não é um caso legítimo de nome/número/termo de
     * lore — ou seja, a fala provavelmente nunca foi traduzida de fato.
     */
    public boolean pareceNaoTraduzida(String original, String traduzido) {
        if (original == null || traduzido == null) {
            return false;
        }
        String o = normalizar(original);
        String t = normalizar(traduzido);
        if (o.isEmpty() || !o.equals(t)) {
            return false;
        }
        return !deveManterIdentico(original);
    }

    private String normalizar(String texto) {
        return PADRAO_REMOVE_TAGS_ASS.matcher(texto).replaceAll("")
            .replace("\\N", " ")
            .replace("\\n", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }
}
