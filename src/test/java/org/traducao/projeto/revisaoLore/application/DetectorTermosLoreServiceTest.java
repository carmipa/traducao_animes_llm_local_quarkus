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

    @Test
    void naoConsideraTagsComoNomesPropriosDivergentes() {
        ResultadoDeteccaoLore resultado = detector.auditar(
            "[[TAG0]]Beans never seem to go down smoothly for me.[[TAG1]]",
            "Os feijões nunca parecem descer suavemente para mim."
        );

        assertFalse(resultado.suspeito());
    }

    @Test
    void naoSinalizaPalavrasComunsCapitalizadasNoInicioDaFala() {
        ResultadoDeteccaoLore resultado = detector.auditar(
            "Passing defense line one! Beginning countdown!",
            "Passando pela primeira linha de defesa! Iniciando a contagem regressiva!"
        );

        assertFalse(resultado.suspeito());
    }

    @Test
    void naoDetectaPalavraInglesaDentroDePalavraPortuguesa() {
        ResultadoDeteccaoLore resultado = detector.auditar(
            "Just a vaporization bomb. What else would it be?",
            "Apenas uma bomba de vaporização. O que mais seria?"
        );

        assertFalse(resultado.suspeito());
    }

    @Test
    void aceitaVariantesPtBrDeEarthFederation() {
        ResultadoDeteccaoLore federacaoTerrestre = detector.auditar(
            "Two Earth Federation units are approaching.",
            "Duas unidades da Federação Terrestre estão se aproximando."
        );
        ResultadoDeteccaoLore federacaoDaTerra = detector.auditar(
            "Two Earth Federation units are approaching.",
            "Duas unidades da Federação da Terra estão se aproximando."
        );

        assertFalse(federacaoTerrestre.suspeito());
        assertFalse(federacaoDaTerra.suspeito());
    }

    @Test
    void sinalizaFederationQuandoFicaEmInglesNaTraducao() {
        ResultadoDeteccaoLore resultado = detector.auditar(
            "Let the Federation have Odessa and the Earth!",
            "Deixe a Federation ficar com Odessa e a Terra!"
        );

        assertTrue(resultado.suspeito());
        assertTrue(resultado.motivos().stream().anyMatch(m -> m.contains("traduzivel permaneceu em ingles")));
    }

    @Test
    void sinalizaTermosRelevantesDeLoreNoInicioDaFalaQuandoDivergentes() {
        ResultadoDeteccaoLore gundamOmitido = detector.auditar(
            "Gundam is approaching!",
            "O robo gigante está se aproximando!"
        );
        ResultadoDeteccaoLore zeonOmitido = detector.auditar(
            "Zeon forces are attacking.",
            "As forças inimigas estão atacando."
        );

        assertTrue(gundamOmitido.suspeito());
        assertTrue(gundamOmitido.motivos().stream().anyMatch(m -> m.contains("inconsistente")));
        assertTrue(zeonOmitido.suspeito());
        assertTrue(zeonOmitido.motivos().stream().anyMatch(m -> m.contains("inconsistente")));
    }

    @Test
    void naoSinalizaQuebraDeFrasesComPalavrasComunsCapitalizadas() {
        ResultadoDeteccaoLore res1 = detector.auditar(
            "Yes. Someone very important to us.",
            "Sim. Alguém muito importante para nós."
        );
        ResultadoDeteccaoLore res2 = detector.auditar(
            "She shall live on inside of us. Forever.",
            "Ela continuará viva dentro de nós. Para sempre."
        );
        ResultadoDeteccaoLore res3 = detector.auditar(
            "How can you all look so calm?!",
            "Como todos vocês podem parecer tão calmos!"
        );
        ResultadoDeteccaoLore res4 = detector.auditar(
            "We're sad, but...",
            "Estamos tristes, mas..."
        );

        assertFalse(res1.suspeito());
        assertFalse(res2.suspeito());
        assertFalse(res3.suspeito());
        assertFalse(res4.suspeito());
    }
}
