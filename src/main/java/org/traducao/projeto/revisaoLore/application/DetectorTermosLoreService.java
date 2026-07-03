package org.traducao.projeto.revisaoLore.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.revisaoLore.domain.ResultadoDeteccaoLore;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristica leve para priorizar falas com possivel erro de lore/terminologia
 * antes de chamar o LLM (nomes em ingles remanescentes, grafias suspeitas, etc.).
 */
@Service
public class DetectorTermosLoreService {

    private static final Pattern NOME_PROPRIO = Pattern.compile("\\b[A-Z][a-z]{2,}(?:\\s+[A-Z][a-z]{2,})*\\b");
    private static final Pattern PALAVRA_LATINA = Pattern.compile("\\b[A-Za-z]{3,}\\b");
    private static final Set<String> PALAVRAS_IGNORADAS = Set.of(
        "the", "and", "you", "your", "that", "this", "with", "from", "what", "when", "where",
        "why", "how", "are", "was", "were", "have", "has", "had", "not", "but", "for", "all",
        "out", "our", "his", "her", "him", "she", "they", "them", "will", "can", "just", "like",
        "get", "got", "one", "two", "now", "yes", "hey", "sir", "miss", "lord", "lady", "man",
        "boy", "girl", "god", "damn", "hell", "okay", "ok", "well", "come", "here", "there"
    );

    public ResultadoDeteccaoLore auditar(String originalIngles, String traducaoPt) {
        if (originalIngles == null || originalIngles.isBlank()
            || traducaoPt == null || traducaoPt.isBlank()) {
            return ResultadoDeteccaoLore.limpo();
        }

        List<String> motivos = new ArrayList<>();
        String en = originalIngles.trim();
        String pt = traducaoPt.trim();

        detectarNomesInglesRemanescentes(en, pt, motivos);
        detectarNomesPropriosDivergentes(en, pt, motivos);
        detectarTermosMaiusculosSuspeitos(pt, motivos);

        if (motivos.isEmpty()) {
            return ResultadoDeteccaoLore.limpo();
        }
        return new ResultadoDeteccaoLore(true, List.copyOf(motivos));
    }

    private void detectarNomesInglesRemanescentes(String en, String pt, List<String> motivos) {
        Matcher matcher = PALAVRA_LATINA.matcher(en);
        Set<String> candidatos = new LinkedHashSet<>();
        while (matcher.find()) {
            String palavra = matcher.group().toLowerCase(Locale.ROOT);
            if (palavra.length() >= 4 && !PALAVRAS_IGNORADAS.contains(palavra)) {
                candidatos.add(palavra);
            }
        }

        String ptLower = pt.toLowerCase(Locale.ROOT);
        for (String candidato : candidatos) {
            if (ptLower.contains(candidato)) {
                motivos.add("Possivel nome/termo em ingles remanescente na traducao: \"" + candidato + "\"");
            }
        }
    }

    private void detectarNomesPropriosDivergentes(String en, String pt, List<String> motivos) {
        Matcher matcherEn = NOME_PROPRIO.matcher(en);
        while (matcherEn.find()) {
            String nome = matcherEn.group();
            if (nome.length() < 4) {
                continue;
            }
            if (!pt.contains(nome) && !contemVarianteAproximada(pt, nome)) {
                motivos.add("Nome proprio do original pode estar inconsistente na traducao: \"" + nome + "\"");
            }
        }
    }

    private void detectarTermosMaiusculosSuspeitos(String pt, List<String> motivos) {
        Matcher matcher = Pattern.compile("\\b[A-Z]{2,}\\b").matcher(pt);
        while (matcher.find()) {
            String token = matcher.group();
            if (token.length() >= 3 && !token.equals("ASS") && !token.equals("SSA")) {
                motivos.add("Sigla ou termo todo em maiusculas pode indicar lore fora do padrao: \"" + token + "\"");
                break;
            }
        }
    }

    private boolean contemVarianteAproximada(String pt, String nome) {
        String[] partes = nome.split("\\s+");
        if (partes.length == 1) {
            return false;
        }
        int encontrados = 0;
        for (String parte : partes) {
            if (parte.length() >= 3 && pt.toLowerCase(Locale.ROOT).contains(parte.toLowerCase(Locale.ROOT))) {
                encontrados++;
            }
        }
        return encontrados >= Math.max(1, partes.length - 1);
    }
}
