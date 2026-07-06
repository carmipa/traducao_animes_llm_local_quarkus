package org.traducao.projeto.revisaoLore.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.revisaoLore.domain.ResultadoDeteccaoLore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristica leve para priorizar falas com possivel erro de lore/terminologia
 * antes de chamar o LLM (nomes em ingles remanescentes, grafias suspeitas, etc.).
 */
@Service
public class DetectorTermosLoreService {

    private static final Pattern NOME_PROPRIO = Pattern.compile(
        "\\b(?:[A-Z][A-Za-z0-9'’.-]{2,}|[A-Z]{2,}(?:-[A-Z0-9]+)?)(?:\\s+(?:[A-Z][A-Za-z0-9'’.-]{2,}|[A-Z]{2,}(?:-[A-Z0-9]+)?))*\\b"
    );
    private static final Pattern PALAVRA_LATINA = Pattern.compile("\\b[A-Za-z]{3,}\\b");
    private static final Map<String, List<String>> TRADUCOES_LITERAIS_SUSPEITAS = criarTraducoesLiteraisSuspeitas();
    private static final Map<String, List<String>> TERMOS_TRADUZIVEIS_ACEITOS = criarTermosTraduziveisAceitos();
    private static final Set<String> TERMOS_LORE_SOLTEIROS_RELEVANTES = Set.of(
        "aeug", "titans", "anaheim", "apsalus", "sahalin", "sakhalin", "char", "amuro",
        "londo", "phenex", "unicorn", "narrative", "banshee", "legion", "handler",
        "processor", "juggernaut", "valkyrie", "macross",
        "zeon", "gundam", "zaku", "gouf", "gelgoog", "dom", "gm", "kampfer", "sazabi",
        "alex", "sinanju", "axis", "jaburo", "odessa", "libot", "albion", "shiro",
        "aina", "karen", "eledore", "michel", "kiki", "ginias", "norris", "packard",
        "kojima", "kou", "gato", "nina", "cima", "bernie", "christina", "chris",
        "bright", "sayla", "lalah", "hathaway", "quess", "gyunei", "nanai", "jona",
        "michelle", "rita", "zoltan"
    );
    private static final Set<String> PALAVRAS_IGNORADAS = Set.of(
        "the", "and", "you", "your", "that", "this", "with", "from", "what", "when", "where",
        "why", "how", "are", "was", "were", "have", "has", "had", "not", "but", "for", "all",
        "out", "our", "his", "her", "him", "she", "they", "them", "will", "can", "just", "like",
        "get", "got", "one", "two", "now", "yes", "hey", "sir", "miss", "lord", "lady", "man",
        "boy", "girl", "god", "damn", "hell", "okay", "ok", "well", "come", "here", "there",
        "even", "passing", "beginning", "fall", "leave", "these", "if", "let", "yeah",
        "youre", "im", "dont", "cant", "wont", "ill", "ive", "thats", "whats", "base",
        "someone", "anyone", "something", "anything", "forever", "indeed", "maybe", "please",
        "thanks", "thank", "hello", "goodbye", "always", "never", "every"
    );

    public ResultadoDeteccaoLore auditar(String originalIngles, String traducaoPt) {
        if (originalIngles == null || originalIngles.isBlank()
            || traducaoPt == null || traducaoPt.isBlank()) {
            return ResultadoDeteccaoLore.limpo();
        }

        List<String> motivos = new ArrayList<>();
        String en = originalIngles.trim();
        String pt = traducaoPt.trim();

        detectarTraducoesLiteraisSuspeitas(en, pt, motivos);
        detectarTermosTraduziveisEmIngles(en, pt, motivos);
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
        Set<String> tokensNomeProprio = tokensDeNomesProprios(en);
        while (matcher.find()) {
            String palavra = matcher.group().toLowerCase(Locale.ROOT);
            if (palavra.matches("tag\\d+")) {
                continue;
            }
            if (palavra.length() >= 4
                && !PALAVRAS_IGNORADAS.contains(palavra)
                && !tokensNomeProprio.contains(palavra)) {
                candidatos.add(palavra);
            }
        }

        String ptLower = pt.toLowerCase(Locale.ROOT);
        for (String candidato : candidatos) {
            if (contemPalavraInteira(ptLower, candidato)) {
                motivos.add("Possivel nome/termo em ingles remanescente na traducao: \"" + candidato + "\"");
            }
        }
    }

    private void detectarTermosTraduziveisEmIngles(String en, String pt, List<String> motivos) {
        String enLower = en.toLowerCase(Locale.ROOT);
        String ptLower = pt.toLowerCase(Locale.ROOT);

        for (Map.Entry<String, List<String>> entrada : TERMOS_TRADUZIVEIS_ACEITOS.entrySet()) {
            String termoIngles = entrada.getKey();
            if (!contemExpressaoInteira(enLower, termoIngles) || contemAlgumaExpressao(ptLower, entrada.getValue())) {
                continue;
            }
            if (contemExpressaoInteira(ptLower, termoIngles)) {
                motivos.add("Termo de faccao/organizacao traduzivel permaneceu em ingles: \""
                    + termoIngles + "\"");
            }
        }
    }

    private void detectarTraducoesLiteraisSuspeitas(String en, String pt, List<String> motivos) {
        String enLower = en.toLowerCase(Locale.ROOT);
        String ptLower = pt.toLowerCase(Locale.ROOT);

        for (Map.Entry<String, List<String>> entrada : TRADUCOES_LITERAIS_SUSPEITAS.entrySet()) {
            String termoCanonico = entrada.getKey();
            if (!enLower.contains(termoCanonico)) {
                continue;
            }
            for (String traducaoSuspeita : entrada.getValue()) {
                if (ptLower.contains(traducaoSuspeita)) {
                    motivos.add("Possivel traducao literal de termo/nome canonico: \""
                        + traducaoSuspeita + "\" deveria preservar \"" + termoCanonico + "\" quando for lore");
                    break;
                }
            }
        }
    }

    private void detectarNomesPropriosDivergentes(String en, String pt, List<String> motivos) {
        Matcher matcherEn = NOME_PROPRIO.matcher(en);
        while (matcherEn.find()) {
            String grupo = matcherEn.group();
            // Divide o grupo por quebras de frase reais (. ! ? seguido de espaço),
            // ignorando abreviações comuns (Dr., Lt., U.C., etc.)
            String[] subNomes = grupo.split("(?<!\\bDr|\\bLt|\\bCol|\\bCapt|\\bGen|\\bMr|\\bMrs|\\bMs|\\bSt|\\bU\\.C)(?<=[.!?])\\s+");
            for (String subNomeRaw : subNomes) {
                String nome = limparCandidatoNomeProprio(subNomeRaw);
                int indexNoOriginal = en.indexOf(subNomeRaw);
                if (deveIgnorarNomeProprio(nome, inicioEfetivoDaFala(en, indexNoOriginal >= 0 ? indexNoOriginal : matcherEn.start()))
                    || traducaoAceitaParaTermo(nome, pt)) {
                    continue;
                }
                if (pt.contains(nome)) {
                    continue;
                }
                if (contemNomeCompostoParcial(pt, nome)) {
                    motivos.add("Nome proprio composto foi preservado apenas em parte; conferir se algum trecho foi traduzido: \"" + nome + "\"");
                } else if (!contemVarianteAproximada(pt, nome)) {
                    motivos.add("Nome proprio do original pode estar inconsistente na traducao: \"" + nome + "\"");
                }
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

    private Set<String> tokensDeNomesProprios(String en) {
        Set<String> tokens = new LinkedHashSet<>();
        Matcher matcher = NOME_PROPRIO.matcher(en);
        while (matcher.find()) {
            for (String parte : limparCandidatoNomeProprio(matcher.group()).split("\\s+")) {
                String normalizada = normalizarTokenNome(parte);
                if (normalizada.length() >= 3 && !normalizada.matches("tag\\d+")) {
                    tokens.add(normalizada);
                }
            }
        }
        return tokens;
    }

    private String removerArtigoInicialIngles(String nome) {
        return nome.replaceFirst("(?i)^(the|a|an)\\s+", "");
    }

    private String removerPossessivoIngles(String nome) {
        return nome.replaceAll("(?i)['’]s\\b", "");
    }

    private String limparCandidatoNomeProprio(String nome) {
        return removerPrefixoComumIngles(removerArtigoInicialIngles(removerPossessivoIngles(nome))).strip();
    }

    private String removerPrefixoComumIngles(String nome) {
        return nome.replaceFirst("(?i)^(one|two|three|four|five|six|seven|eight|nine|ten|all|these|those|this|that|some|any)\\s+", "");
    }

    private boolean deveIgnorarNomeProprio(String nome, boolean inicioEfetivoDaFala) {
        if (nome.length() < 4 || nome.matches("(?i)TAG\\d+")) {
            return true;
        }

        String[] partes = nome.split("\\s+");
        if (partes.length == 1) {
            String normalizada = normalizarTokenNome(partes[0]);
            if (PALAVRAS_IGNORADAS.contains(normalizada)) {
                return true;
            }
            return inicioEfetivoDaFala && !temIndicadorLoreSolteiro(partes[0], normalizada);
        }
        return false;
    }

    private boolean inicioEfetivoDaFala(String texto, int inicioCandidato) {
        if (inicioCandidato <= 0) {
            return true;
        }
        String prefixo = texto.substring(0, Math.max(0, inicioCandidato))
            .replaceAll("(?i)\\[\\[TAG\\d+\\]\\]", "")
            .trim();
        if (prefixo.isEmpty()) {
            return true;
        }
        char ultimo = prefixo.charAt(prefixo.length() - 1);
        return ultimo == '.' || ultimo == '!' || ultimo == '?' || ultimo == '"' || ultimo == '”' || ultimo == '\'' || ultimo == '’';
    }

    private boolean temIndicadorLoreSolteiro(String original, String normalizada) {
        return TERMOS_LORE_SOLTEIROS_RELEVANTES.contains(normalizada)
            || original.matches(".*\\d.*")
            || original.matches("[A-Z0-9.-]{2,}");
    }

    private boolean traducaoAceitaParaTermo(String nome, String pt) {
        List<String> aceitas = TERMOS_TRADUZIVEIS_ACEITOS.get(nome.toLowerCase(Locale.ROOT));
        return aceitas != null && contemAlgumaExpressao(pt.toLowerCase(Locale.ROOT), aceitas);
    }

    private String normalizarTokenNome(String token) {
        return removerPossessivoIngles(token)
            .replaceAll("[^A-Za-z0-9]", "")
            .toLowerCase(Locale.ROOT);
    }

    private boolean contemNomeCompostoParcial(String pt, String nome) {
        String[] partes = nome.split("\\s+");
        if (partes.length < 2) {
            return false;
        }

        String ptLower = pt.toLowerCase(Locale.ROOT);
        int encontrados = 0;
        int relevantes = 0;
        for (String parte : partes) {
            String normalizada = parte.replaceAll("[^A-Za-z0-9]", "");
            if (normalizada.length() < 3) {
                continue;
            }
            relevantes++;
            if (ptLower.contains(normalizada.toLowerCase(Locale.ROOT))) {
                encontrados++;
            }
        }
        return relevantes >= 2 && encontrados > 0 && encontrados < relevantes;
    }

    private boolean contemAlgumaExpressao(String textoLower, List<String> expressoes) {
        return expressoes.stream().anyMatch(expressao -> contemExpressaoInteira(textoLower, expressao));
    }

    private boolean contemPalavraInteira(String textoLower, String palavraLower) {
        return contemExpressaoInteira(textoLower, palavraLower);
    }

    private boolean contemExpressaoInteira(String textoLower, String expressaoLower) {
        return Pattern
            .compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(expressaoLower) + "(?![\\p{L}\\p{N}])",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
            .matcher(textoLower)
            .find();
    }

    private static Map<String, List<String>> criarTraducoesLiteraisSuspeitas() {
        Map<String, List<String>> termos = new LinkedHashMap<>();
        termos.put("narrative", List.of("narrativo", "narrativa"));
        termos.put("unicorn", List.of("unicórnio", "unicornio"));
        termos.put("phenex", List.of("fênix", "fenix"));
        termos.put("freedom", List.of("liberdade"));
        termos.put("justice", List.of("justiça", "justica"));
        termos.put("destiny", List.of("destino"));
        termos.put("stargazer", List.of("observador de estrelas", "observadora de estrelas"));
        termos.put("war in the pocket", List.of("guerra no bolso", "guerra de bolso"));
        termos.put("mobile suit", List.of("traje móvel", "traje movel", "roupa móvel", "roupa movel"));
        termos.put("mobile armor", List.of("armadura móvel", "armadura movel"));
        termos.put("newtype", List.of("novo tipo", "nova tipo"));
        termos.put("handler", List.of("manipulador", "manipuladora"));
        termos.put("processor", List.of("processador", "processadora"));
        termos.put("juggernaut", List.of("rolo compressor"));
        return Map.copyOf(termos);
    }

    private static Map<String, List<String>> criarTermosTraduziveisAceitos() {
        Map<String, List<String>> termos = new LinkedHashMap<>();
        termos.put("earth federation", List.of(
            "federacao terrestre",
            "federação terrestre",
            "federacao da terra",
            "federação da terra"
        ));
        termos.put("federation", List.of("federacao", "federação"));
        termos.put("principality of zeon", List.of("principado de zeon"));
        return Map.copyOf(termos);
    }
}
