package org.traducao.projeto.analisadorMidia.application;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.analisadorMidia.domain.LegendaInfo;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cobre o dado VITAL da análise: a classificação do tipo de legenda (codec →
 * tipo) e o veredicto de traduzibilidade (texto = traduzível; bitmap = OCR;
 * nenhuma = RAW/hardsub). Decide se um episódio segue no pipeline de tradução.
 */
class AnalisarMidiaClassificacaoTest {

    @Test
    void classificaOsCodecsDeLegendaConhecidos() {
        assertEquals("ASS", tipoCurto("ass", "ASS"));
        assertEquals("SRT", tipoCurto("subrip", "SUBRIP"));
        assertEquals("PGS", tipoCurto("hdmv_pgs_subtitle", "HDMV_PGS_SUBTITLE"));
        assertEquals("VOBSUB", tipoCurto("dvd_subtitle", "DVD_SUBTITLE"));
        assertEquals("WEBVTT", tipoCurto("webvtt", "WEBVTT"));
        assertEquals("MOV_TEXT", tipoCurto("mov_text", "MOV_TEXT"));
    }

    @Test
    void codecDesconhecidoCaiEmDesconhecido() {
        assertEquals("DESCONHECIDO", tipoCurto("algo_estranho", "XYZ"));
    }

    @Test
    void veredictoDeTextoEhTraduzivel() {
        assertTrue(AnalisarMidiaUseCase.verdictTraducao(List.of(leg("ASS"))).startsWith("SIM"));
        assertTrue(AnalisarMidiaUseCase.verdictTraducao(List.of(leg("SRT"))).startsWith("SIM"));
    }

    @Test
    void veredictoDeBitmapNaoEhTraduzivel() {
        assertTrue(AnalisarMidiaUseCase.verdictTraducao(List.of(leg("PGS"))).startsWith("NAO"));
        assertTrue(AnalisarMidiaUseCase.verdictTraducao(List.of(leg("VOBSUB"))).startsWith("NAO"));
    }

    @Test
    void veredictoSemLegendaEhNaoAplicavel() {
        assertTrue(AnalisarMidiaUseCase.verdictTraducao(List.of()).startsWith("N/A"));
    }

    @Test
    void faixaDeTextoTemPrioridadeSobreBitmap() {
        // Um arquivo com PGS + ASS ainda é traduzível pela faixa de texto.
        String veredicto = AnalisarMidiaUseCase.verdictTraducao(List.of(leg("PGS"), leg("ASS")));
        assertTrue(veredicto.startsWith("SIM"), veredicto);
    }

    private static String tipoCurto(String codecId, String formato) {
        return AnalisarMidiaUseCase.classificarLegenda(codecId, formato)[1];
    }

    private static LegendaInfo leg(String tipoCurto) {
        return new LegendaInfo(0, 0, "eng", "ASS", "ass", "(Sem titulo)",
            null, tipoCurto, null, false, false, false, false, false, false, null, null);
    }
}
