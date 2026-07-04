package org.traducao.projeto.traducao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.traducao.domain.exceptions.AlucinacaoDetectadaException;

import java.util.regex.Pattern;

@Service
public class ValidadorTraducaoService {
    
    // Regras robustas importadas do pipeline Python, ampliadas após observar em
    // produção o Mistral Nemo deixar fragmentos como "exactly the same" sem
    // traduzir mesmo traduzindo o resto da fala corretamente.
    //
    // UNICODE_CHARACTER_CLASS e necessario aqui: sem ela, \b no Java so reconhece
    // [a-zA-Z0-9_] como caractere de palavra, entao letras acentuadas (ç, ã, é...)
    // contam como "fronteira", e palavras em portugues como "força" ou "esforço"
    // batem com "\bfor\b" e disparam falso positivo de "resíduo em inglês".
    private static final Pattern PADRAO_RESIDUO = Pattern.compile(
        "\\b(you|they|without|very|where|what|when|why|who|this|that|these|those|"
            + "and|the|is|are|was|were|have|has|with|from|exactly|same|not|but|"
            + "except|because|could|would|should|will|shall|might|must|cannot|"
            + "their|yours|hers|ours|theirs|whom|whose|which|how|however|whenever|wherever|"
            + "about|above|across|after|against|along|among|around|before|behind|below|beneath|"
            + "beside|between|beyond|down|during|into|outside|over|past|since|through|"
            + "throughout|till|toward|under|underneath|until|upon|within|although|unless|"
            + "does|did|been|had|went|gone|got|make|made|take|took|saw|seen|"
            + "always|never|sometimes|often|usually|really|also|even|every|please|thank|thanks|sorry)\\b",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    // Palavras inequívocas de francês (sem colisão com vocabulário PT-BR) que pegam
    // o LLM local "vazando" pra francês em vez de inglês — caso observado em
    // produção: "WITH SHINING BLUE FIRE" foi "corrigido" para "AURA BLEU BRILLANTE"
    // e passou batido pelo PADRAO_RESIDUO, que só cobre inglês. Espanhol não entra
    // aqui: nunca foi observado como idioma de origem/vazamento neste projeto.
    private static final Pattern PADRAO_OUTRO_IDIOMA = Pattern.compile(
        "\\b(bleu|rouge|noir|blanc|jaune|monde|cœur|coeur|amour|bonjour|merci|"
            + "toujours|déjà|deja|être|avoir|avec|chez|sans|vous|nous|elles|"
            + "très|tres|où|quelque|aujourd'hui)\\b",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    // Evita os falsos positivos de "Abaixo a tirania" bloqueando apenas preâmbulos óbvios
    private static final Pattern PADRAO_PREAMBULO = Pattern.compile(
        "^(esta [ée] a tradu|abaixo seguem|aqui está a|tradução solicitada|a tradução seria)",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    // Palavras do PADRAO_RESIDUO que também são nomes próprios comuns em
    // anime/mangá (ex.: o personagem "Will"). Uma ocorrência Capitalizada
    // dessas palavras numa frase PT-BR é quase sempre nome, não resíduo —
    // sem esta exceção a fala inteira era rejeitada e mantida sem tradução.
    private static final java.util.Set<String> RESIDUOS_TAMBEM_NOMES = java.util.Set.of("will");

    public void validarFala(String textoTraduzido) {
        if (textoTraduzido == null || textoTraduzido.trim().isEmpty()) {
            return;
        }

        if (temResiduoRelevante(textoTraduzido)) {
            throw new AlucinacaoDetectadaException("Resíduo gringo detectado: " + textoTraduzido);
        }

        if (PADRAO_OUTRO_IDIOMA.matcher(textoTraduzido).find()) {
            throw new AlucinacaoDetectadaException("Idioma incorreto detectado (não é PT-BR): " + textoTraduzido);
        }

        if (PADRAO_PREAMBULO.matcher(textoTraduzido).find()) {
            throw new AlucinacaoDetectadaException("Preâmbulo detectado: " + textoTraduzido);
        }
    }

    /**
     * true quando há resíduo em inglês de verdade: qualquer palavra do padrão
     * conta, EXCETO quando todas as ocorrências são palavras de
     * {@link #RESIDUOS_TAMBEM_NOMES} escritas Capitalizadas (não CAIXA ALTA),
     * o que indica nome próprio e não fala sem traduzir.
     */
    private boolean temResiduoRelevante(String texto) {
        var matcher = PADRAO_RESIDUO.matcher(texto);
        while (matcher.find()) {
            String palavra = matcher.group(1);
            boolean capitalizada = palavra.length() > 1
                && Character.isUpperCase(palavra.charAt(0))
                && !palavra.equals(palavra.toUpperCase());
            boolean nomeProprioProvavel = capitalizada
                && RESIDUOS_TAMBEM_NOMES.contains(palavra.toLowerCase());
            if (!nomeProprioProvavel) {
                return true;
            }
        }
        return false;
    }
}
