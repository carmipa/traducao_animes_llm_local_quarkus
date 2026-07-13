package org.traducao.projeto.apiDadosAnime.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObterMetadataAnimeUseCaseTest {

    private final ObterMetadataAnimeUseCase useCase = new ObterMetadataAnimeUseCase(
        null,
        null,
        null,
        new ObjectMapper()
    );

    @Test
    void removeSufixoRevisaoDeLoreDoTermoDeBusca() {
        assertEquals("86 Eighty Six", useCase.extrairNomeTermoBusca("86 (Eighty-Six) - Revisao de Lore"));
        assertEquals("DanMachi", useCase.extrairNomeTermoBusca("DanMachi S5 - Revisao de Lore"));
        assertEquals(
            "Mobile Suit Gundam: The 08th MS Team",
            useCase.extrairNomeTermoBusca("Mobile Suit Gundam: The 08th MS Team - Revisao de Lore")
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: garante que o contexto Gundam Narrative mantenha o
     * alias usado pelas APIs de capa tanto na tradução quanto na revisão de lore.
     * <p>
     * INVARIANTES DO DOMÍNIO: parênteses são apenas delimitadores; a palavra
     * {@code Narrative} nunca pode ser descartada do termo de busca.
     * <p>
     * COMPORTAMENTO EM CASO DE FALHA: qualquer regressão reprova a suíte com a
     * diferença entre o termo esperado e o termo sanitizado.
     */
    @Test
    void preservaAliasNarrativeEntreParentesesParaBuscaDeCapa() {
        assertEquals(
            "Mobile Suit Gundam NT Narrative",
            useCase.extrairNomeTermoBusca("Mobile Suit Gundam NT (Narrative)")
        );
        assertEquals(
            "Mobile Suit Gundam NT Narrative",
            useCase.extrairNomeTermoBusca("Mobile Suit Gundam NT (Narrative) - Revisao de Lore")
        );
    }

    @Test
    void mantemLimpezaPadraoDeCaminhoDeArquivo() {
        assertEquals(
            "Mobile Suit Gundam The 08th MS Team",
            useCase.extrairNomeTermoBusca("C:\\animes\\[Joseki] Mobile Suit Gundam The 08th MS Team COMPLETE (1996)(BD AV1 1080p)\\Mobile.Suit.Gundam.The.08th.MS.Team.S01E02_Track3_PT-BR.ass")
        );
    }
}
