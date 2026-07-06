package org.traducao.projeto.apiDadosAnime.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObterMetadataAnimeUseCaseTest {

    private final ObterMetadataAnimeUseCase useCase = new ObterMetadataAnimeUseCase(
        null,
        null,
        new ObjectMapper()
    );

    @Test
    void removeSufixoRevisaoDeLoreDoTermoDeBusca() {
        assertEquals("86", useCase.extrairNomeTermoBusca("86 (Eighty-Six) - Revisao de Lore"));
        assertEquals("DanMachi", useCase.extrairNomeTermoBusca("DanMachi S5 - Revisao de Lore"));
        assertEquals(
            "Mobile Suit Gundam: The 08th MS Team",
            useCase.extrairNomeTermoBusca("Mobile Suit Gundam: The 08th MS Team - Revisao de Lore")
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
