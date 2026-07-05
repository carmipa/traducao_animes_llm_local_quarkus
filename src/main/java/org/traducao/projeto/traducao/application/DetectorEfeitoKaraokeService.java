package org.traducao.projeto.traducao.application;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Reconhece eventos que são efeito de karaokê/música, e não fala de diálogo.
 * Cobre as duas formas em que o karaokê aparece nos arquivos .ass:
 * <ul>
 *   <li>Karaokê "cru": tags de timing {@code \k}, {@code \kf}, {@code \ko}
 *       por sílaba, como sai do fansub antes de aplicar template.</li>
 *   <li>Saída do Kara Templater do Aegisub: as tags {@code \k} são consumidas
 *       na aplicação do template e viram uma linha por sílaba/letra com
 *       transformações animadas ({@code \t(...)}, {@code \frx}, {@code \fad},
 *       {@code \pos}) e quase nenhum texto visível.</li>
 * </ul>
 * Regra única compartilhada pelos módulos de tradução, revisão e correção —
 * nenhum deles deve mexer em música; isso é responsabilidade do módulo de
 * karaokê.
 */
@Service
public class DetectorEfeitoKaraokeService {

    // Tags de timing de karaoke ASS (\k, \kf, \ko, etc.)
    private static final Pattern TAG_KARAOKE_PATTERN = Pattern.compile("\\\\[kK][fo]?\\d");
    // Transformação animada \t(...): diálogo comum praticamente nunca usa;
    // templates de karaokê e letreiros animados sempre usam.
    private static final Pattern TAG_TRANSFORMACAO_PATTERN = Pattern.compile("\\\\t\\(");
    private static final Pattern TAG_POSICIONAMENTO_COMPLEXO_PATTERN = Pattern.compile("\\\\(pos|move|i?clip|org)\\(");
    private static final Pattern PADRAO_REMOVE_TAGS_ASS = Pattern.compile("\\{[^}]*\\}");
    private static final Pattern ESCRITA_JAPONESA_PATTERN = Pattern.compile("[\\p{IsHiragana}\\p{IsKatakana}\\p{IsHan}]");
    private static final Pattern TEXTO_ALFANUMERICO_PATTERN = Pattern.compile("[\\p{L}\\d]");
    private static final Pattern ESTILO_MUSICA_PATTERN = Pattern.compile(
        "(?i)\\b(song|music|karaoke|opening|ending|theme|insert|op|ed|sing|lyrics?)\\b");
    private static final Pattern ESTILO_JAPONES_ROMAJI_PATTERN = Pattern.compile(
        "(?i)\\b(romaji|jp|jpn|japanese|japones|japon[eê]s|kana|kanji)\\b");
    private static final int MIN_CHARS_TAGS_POSICIONAMENTO_COMPLEXO = 45;

    /**
     * Karaokê cru: só as tags de timing {@code \k}. Usado onde ignorar demais
     * tem custo alto (tradução: letreiros/títulos com {@code \t} e texto curto
     * DEVEM ser traduzidos e têm a mesma assinatura do template de karaokê).
     */
    public boolean temTagKaraoke(String texto) {
        return texto != null && TAG_KARAOKE_PATTERN.matcher(texto).find();
    }

    /**
     * Karaokê em qualquer forma (cru ou pós-template). Usado nos fluxos de
     * revisão/correção, onde ignorar um letreiro já traduzido é inofensivo e o
     * risco real é mexer em música.
     */
    public boolean eEfeitoKaraoke(String texto) {
        if (texto == null || texto.isBlank()) {
            return false;
        }
        if (TAG_KARAOKE_PATTERN.matcher(texto).find()) {
            return true;
        }
        return eSaidaDeTemplateKaraoke(texto);
    }

    /**
     * Karaoke/música que deve ficar intacto: letra em japonês (kana/kanji) ou
     * estilos explicitamente marcados como japonês/romaji. Karaoke em inglês,
     * francês etc. não entra aqui e pode seguir para tradução/revisão.
     */
    public boolean devePreservarKaraokeOriginal(String estilo, String texto) {
        if (!temIndicadorDeMusica(estilo, texto)) {
            return false;
        }
        if (estilo != null && ESTILO_JAPONES_ROMAJI_PATTERN.matcher(estilo).find()) {
            return true;
        }
        return ESCRITA_JAPONESA_PATTERN.matcher(extrairTextoVisivel(texto)).find();
    }

    /**
     * Indica letra de música/karaokê com texto visível traduzível e sem sinal
     * claro de japonês/romaji original. Usado para não bloquear OP/ED em inglês
     * ou outros idiomas latinos só porque o estilo contém "song/karaoke".
     */
    public boolean eKaraokeOuMusicaTraduzivel(String estilo, String texto) {
        if (!temIndicadorDeMusica(estilo, texto) || devePreservarKaraokeOriginal(estilo, texto)) {
            return false;
        }
        return TEXTO_ALFANUMERICO_PATTERN.matcher(extrairTextoVisivel(texto)).find();
    }

    /**
     * Sílaba/letra de música pós-template: há transformação animada e o texto
     * visível é ínfimo perto do bloco de tags (ex.: {@code {\r\pos(369,23)
     * \t(1160,1450,\frx-50...)}I}). Uma fala real com efeito pontual tem
     * proporção inversa — mais texto do que tag.
     */
    private boolean eSaidaDeTemplateKaraoke(String texto) {
        boolean temTransformacao = TAG_TRANSFORMACAO_PATTERN.matcher(texto).find();
        boolean temPosicionamentoComplexo = TAG_POSICIONAMENTO_COMPLEXO_PATTERN.matcher(texto).find();
        if (!temTransformacao && !temPosicionamentoComplexo) {
            return false;
        }
        String visivel = PADRAO_REMOVE_TAGS_ASS.matcher(texto).replaceAll("")
            .replace("\\N", " ")
            .strip();
        boolean altaDensidadeTags = visivel.length() * 3 < texto.length();
        if (temTransformacao) {
            return altaDensidadeTags;
        }

        int tamanhoTags = texto.length() - visivel.length();
        return altaDensidadeTags && tamanhoTags >= MIN_CHARS_TAGS_POSICIONAMENTO_COMPLEXO;
    }

    private boolean temIndicadorDeMusica(String estilo, String texto) {
        return temTagKaraoke(texto)
            || (estilo != null && ESTILO_MUSICA_PATTERN.matcher(estilo).find());
    }

    private String extrairTextoVisivel(String texto) {
        if (texto == null) {
            return "";
        }
        return PADRAO_REMOVE_TAGS_ASS.matcher(texto)
            .replaceAll("")
            .replace("\\N", " ")
            .replace("\\n", " ")
            .replace("\\h", " ")
            .strip();
    }
}
