package org.traducao.projeto.revisaoLore.application;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.revisaoLore.domain.ResultadoDeteccaoLore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectorTermosLoreServiceTest {

    private final DetectorTermosLoreService detector = new DetectorTermosLoreService();

    @Test
    void detectaNarrativeTraduzidoLiteralmenteEmNomeCanonico() {
        ResultadoDeteccaoLore resultado = detector.auditar(
            "The RX-9 Narrative Gundam is launching.",
            "O RX-9 Gundam Narrativo vai decolar."
        );

        assertTrue(resultado.suspeito());
        assertTrue(resultado.motivos().stream().anyMatch(m -> m.contains("narrativo")));
    }

    @Test
    void detectaNomeCompostoPreservadoApenasEmParte() {
        ResultadoDeteccaoLore resultado = detector.auditar(
            "Mobile Suit Gundam Narrative is connected to Unicorn.",
            "Mobile Suit Gundam Narrativo esta ligado ao Unicorn."
        );

        assertTrue(resultado.suspeito());
        assertTrue(resultado.motivos().stream().anyMatch(m -> m.contains("preservado apenas em parte")));
    }

    @Test
    void naoSinalizaNomeCanonicoPreservado() {
        ResultadoDeteccaoLore resultado = detector.auditar(
            "The RX-9 Narrative Gundam is launching.",
            "O RX-9 Narrative Gundam vai decolar."
        );

        assertFalse(resultado.suspeito());
    }
}
