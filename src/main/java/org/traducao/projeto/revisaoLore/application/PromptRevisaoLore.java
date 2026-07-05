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
            - Trate nomes canonicos como texto protegido: personagens, sobrenomes, apelidos, lugares,
              naves, mechas, armas, operacoes e titulos de obra NAO devem ser traduzidos.
            - Faccoes e organizacoes consagradas que possuem traducao padrao estabelecida para o portugues devem ser traduzidas
              quando o texto em portugues ja estiver nessa convencao. Aceite variantes naturais e corretas em PT-BR:
              "Federation" pode ser "Federacao"; "Earth Federation" pode ser "Federacao Terrestre" ou
              "Federacao da Terra"; "Principality of Zeon" pode ser "Principado de Zeon".
              NAO force uma unica variante se a traducao atual ja estiver natural, consistente e correta.
              Nomes de faccoes especificas sem traducao consagrada (ex.: "08th MS Team", "Londo Bell") devem ser mantidos no original.
            - Quando uma palavra comum fizer parte de um nome oficial protegido, mantenha a palavra no idioma original
              (ex.: Narrative Gundam, Unicorn Gundam, Freedom Gundam, War in the Pocket, The 08th MS Team).
            - Mantenha termos tecnicos da obra em ingles quando a lore assim indicar (mobile suit, Newtype, Handler, etc.).
            - Corrija transliteracoes erradas, nomes anglicizados indevidos, traducao literal de nomes oficiais e localizacoes fora do padrao.
            - Se apenas uma parte do nome foi traduzida, restaure o nome oficial completo conforme a lore.
            - NAO altere verbos, adjetivos, metaforas ou expressoes comuns de dialogo que ja estejam bem traduzidas para o portugues
              (ex.: mantenha termos fluidos e naturais como "salvacao", nao mude para traducoes literais e engessadas como "linha de vida" baseando-se no ingles "life line").
              O foco da revisao e estritamente a terminologia da lore.
            - Nao use o original em ingles para retraduzir, melhorar estilo, trocar sinonimos ou ajustar fluidez geral.
              Use o original apenas para identificar nomes/termos de lore que estejam errados na traducao atual.
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
            Audite SOMENTE a terminologia de lore da fala em portugues.
            Use o original em ingles apenas como referencia para localizar nomes de personagens, lugares,
            faccoes, mechas, patentes, armas e termos protegidos.
            Nao retraduza a fala, nao melhore estilo, nao troque sinonimos e nao corrija expressoes comuns.
            Se a traducao atual ja estiver aceitavel em PT-BR, devolva exatamente a mesma linha.

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
