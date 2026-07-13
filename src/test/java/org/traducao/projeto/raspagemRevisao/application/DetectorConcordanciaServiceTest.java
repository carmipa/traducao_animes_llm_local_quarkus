package org.traducao.projeto.raspagemRevisao.application;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoDeteccaoConcordancia;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cobre as heurísticas de concordância de gênero (calques do inglês): o núcleo
 * algorítmico da revisão de legendas. Serviço de lógica pura — sem I/O nem LLM.
 */
class DetectorConcordanciaServiceTest {

    private final DetectorConcordanciaService detector = new DetectorConcordanciaService();

    @Test
    void textoLimpoNaoEhSuspeito() {
        ResultadoDeteccaoConcordancia r = detector.analisar("She said hi.", "ela disse oi.");
        assertFalse(r.suspeito());
        assertTrue(r.motivos().isEmpty());
    }

    @Test
    void traducaoNulaOuVaziaEhLimpo() {
        assertFalse(detector.analisar("She said", null).suspeito());
        assertFalse(detector.analisar("She said", "   ").suspeito());
    }

    @Test
    void artigoMasculinoComSubstantivoFemininoEhSuspeito() {
        ResultadoDeteccaoConcordancia r = detector.analisar(null, "aquele garota apareceu.");
        assertTrue(r.suspeito());
        assertFalse(r.motivos().isEmpty());
    }

    @Test
    void sujeitoElaComPredicadoMasculinoEhSuspeito() {
        assertTrue(detector.analisar(null, "ela está cansado.").suspeito());
    }

    @Test
    void sheNoOriginalMasEleNaTraducaoEhSuspeito() {
        assertTrue(detector.analisar("She smiled at me.", "ele sorriu para mim.").suspeito());
    }

    @Test
    void originalSemGeneroComMasculinoMarcadoEhSuspeito() {
        assertTrue(detector.analisar("I am so tired.", "estou cansado.").suspeito());
    }

    @Test
    void objetoPronominalCorretoNaoGeraFalsoPositivo() {
        // "She told him" -> "Ela disse a ele": 'a ele' é objeto correto; não deve
        // marcar pelo 'ele' isolado (regra removerObjetoPronominal).
        ResultadoDeteccaoConcordancia r = detector.analisar("She told him.", "ela disse a ele.");
        assertFalse(r.suspeito(), () -> "não deveria marcar; motivos=" + r.motivos());
    }

    @Test
    void tagsAssNaoAtrapalhamADeteccao() {
        assertTrue(detector.analisar(null, "{\\i1}ela está cansado{\\i0}").suspeito());
    }
}
