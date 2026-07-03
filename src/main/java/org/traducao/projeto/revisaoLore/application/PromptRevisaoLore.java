package org.traducao.projeto.revisaoLore.application;

import java.util.List;

/**
 * Monta os prompts de sistema e usuario para revisao de terminologia/lore
 * (nomes proprios, locais, faccoes, mechas) com base na lore da obra ativa.
 */
public final class PromptRevisaoLore {

    private PromptRevisaoLore() {
    }

    public static String montarPromptSistema(String loreObra) {
        String lore = loreObra != null && !loreObra.isBlank() ? loreObra.strip() : "(sem lore adicional)";
        return """
            Voce e revisor especializado em legendas de anime/filme, focado em TERMINOLOGIA E LORE.
            Corrija APENAS nomes proprios, locais, organizacoes, mechas, titulos, apelidos e termos de mundo
            que estejam fora do padrao oficial da obra. NAO reescreva a fala inteira nem mude concordancia de genero
            a menos que um nome proprio exija artigo/pronome coerente.

            Use a lore abaixo como fonte canonica de grafia e padrao:
            %s

            Regras:
            - Preserve marcadores [[TAGn]] literalmente (nao traduza nem remova).
            - Mantenha termos tecnicos UC/Gundam/obra em ingles quando a lore assim indicar (mobile suit, Newtype, etc.).
            - Corrija transliteracoes erradas, nomes anglicizados indevidos e localizacoes fora do padrao.
            - Se a traducao ja estiver correta segundo a lore, devolva-a sem alteracoes.
            - Nao adicione explicacoes, aspas ou comentarios.

            Responda APENAS com uma unica linha: a fala revisada em portugues do Brasil.
            """.formatted(lore);
    }

    public static String montarPromptUsuario(
        String originalIngles,
        String traducaoPt,
        List<String> problemasDetectados
    ) {
        String listaProblemas = problemasDetectados == null || problemasDetectados.isEmpty()
            ? "(revisao preventiva de nomes/locais/termos)"
            : String.join("\n- ", problemasDetectados);

        return """
            Revise a traducao em portugues comparando com o original em ingles.
            Foque em nomes de personagens, lugares, faccoes, mechas e termos de lore.

            Original (ingles):
            %s

            Traducao atual (portugues):
            %s

            Indicios de problema (heuristica automatica):
            - %s

            Responda com uma unica linha: a traducao revisada conforme a lore oficial.
            """.formatted(originalIngles, traducaoPt, listaProblemas);
    }
}
