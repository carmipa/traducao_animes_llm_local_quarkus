package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectorEfeitoKaraokeServiceTest {

    private final DetectorEfeitoKaraokeService detector = new DetectorEfeitoKaraokeService();

    @Test
    void detectaKaraokeCruComTagsDeTiming() {
        assertTrue(detector.eEfeitoKaraoke("{\\k50}Ka {\\k30}ra {\\k42}o {\\k28}ke"));
        assertTrue(detector.eEfeitoKaraoke("{\\kf20}sora {\\ko35}wo"));
    }

    @Test
    void detectaSaidaDeTemplateKaraokePorLetra() {
        // Linha real que escapou da revisão: letra "I" afogada em transformações.
        assertTrue(detector.eEfeitoKaraoke(
            "{\\r\\pos(369,23)\\t(1160,1450,\\frx-50\\fry50\\bord6\\blur5\\3c&HFFE7C7&"
                + "\\fad(50,50))\\t(1450,1450,\\frx0\\fry0\\bord3\\blur0\\3c&HFEA32F&)}I"));
    }
 
     @Test
     void detectaLetreiroFrameAFramePorDensidadeTags() {
         // Linha com \pos e fscx/fscy onde o texto visível é curto em relação às tags.
         String letreiroFrame = "{\\fscx100\\fscy100\\blur0.8\\fs60\\c&H010101&\\pos(452,444)}THE 08TH MS TEAM";
         assertTrue(detector.eEfeitoKaraoke(letreiroFrame));
     }

     @Test
    void naoSinalizaDialogoComum() {
        assertFalse(detector.eEfeitoKaraoke("What are you doing here?!"));
        assertFalse(detector.eEfeitoKaraoke("{\\i1}Bell, cuidado!{\\i0}"));
    }

    @Test
    void naoSinalizaDialogoComEfeitoPontualETextoLongo() {
        // \t presente, mas o texto visível domina a linha: é fala, não karaokê.
        assertFalse(detector.eEfeitoKaraoke(
            "{\\fad(200,200)\\t(0,300,\\fscx110)}Eu nunca vou desistir deste sonho, aconteça o que acontecer!"));
    }

    @Test
    void naoSinalizaNuloOuVazio() {
        assertFalse(detector.eEfeitoKaraoke(null));
        assertFalse(detector.eEfeitoKaraoke("   "));
    }

    @Test
    void temTagKaraokeSoDetectaTagsDeTimingCruas() {
        assertTrue(detector.temTagKaraoke("{\\k50}Ka {\\k30}ra"));
        // Letreiro/título com \t e texto curto (caso real DanMachi: "Prólogo"):
        // eEfeitoKaraoke sinaliza (revisão pula), temTagKaraoke não (tradução traduz).
        String tituloDeTela = "{\\pos(1565.5,822.5)\\c&H000000&\\blur0.7\\t(4188,0,1,\\1a&HFF&)}Prologue";
        assertTrue(detector.eEfeitoKaraoke(tituloDeTela));
        assertFalse(detector.temTagKaraoke(tituloDeTela));
    }
}
