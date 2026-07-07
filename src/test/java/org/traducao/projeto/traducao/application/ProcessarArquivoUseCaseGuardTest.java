package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessarArquivoUseCaseGuardTest {

    @Test
    void detectaSaidaDeLlmEmLinhaAssPesadaCurta() {
        String original = "{=68}{\\pos(1192,40)\\1c&H00B2BA&\\3c&HB2BABB&"
            + "\\t(0,253,\\blur4\\bord4)\\t(253,758,\\blur0.6\\bord0)}Sa";
        String traduzido = "Saída: {=68}{\\pos(1192,40)\\1c&H00B2BA&\\3c&HB2BABB&"
            + "\\t(0,253,\\blur4\\bord4)\\t(253,758,\\blur0.6\\bord0)}";

        assertTrue(ProcessarArquivoUseCase.respostaAssPesadaSuspeita(original, traduzido));
    }

    @Test
    void detectaSilabaDeEfeitoInfladaParaFrase() {
        String original = "{\\pos(1214,40)\\1c&H00B2BA&\\3c&HB2BABB&"
            + "\\t(0,30,\\blur4\\bord4)\\t(30,90,\\blur0.6\\bord0)}mi";
        String traduzido = "{\\pos(1214,40)\\1c&H00B2BA&\\3c&HB2BABB&"
            + "\\t(0,30,\\blur4\\bord4)\\t(30,90,\\blur0.6\\bord0)}[ininteligível]";

        assertTrue(ProcessarArquivoUseCase.respostaAssPesadaSuspeita(original, traduzido));
    }

    @Test
    void naoBloqueiaDialogoNormalComTagSimples() {
        String original = "{\\i1}I will protect everyone.{\\i0}";
        String traduzido = "{\\i1}Eu vou proteger todos.{\\i0}";

        assertFalse(ProcessarArquivoUseCase.respostaAssPesadaSuspeita(original, traduzido));
    }

    @Test
    void bloqueiaLetraSoltaComClipGiganteAntesDoLlm() {
        String linha = "{\\bord0\\shad0\\fs75\\an5\\pos(885.38,350.217)\\alpha&H80&"
            + "\\clip(m 928 368 b 928 370 929 373 929 376 931 376 934 377 937 377 "
            + "945 377 949 373 949 367 949 362 946 360 940 356 936 353 935 352 "
            + "935 349 935 346 936 345 939 345 943 345 944 346 945 351 l 948 351 "
            + "947 348 947 345 947 343 945 343 942 342 939 342 931 342 928 347 "
            + "928 351 928 356 931 358 936 361 941 365 942 366 942 369 942 373 "
            + "940 374 937 374 934 374 932 373 931 367)}S";

        assertTrue(ProcessarArquivoUseCase.deveBloquearAntesDoLlm("Default", linha, 1));
    }

    @Test
    void bloqueiaTituloVetorialDuplicadoAntesDoLlm() {
        String linha = "{\\an2\\iclip(m 1411 799 l 1408 799 b 1407 803 1407 804 "
            + "1405 804 l 1400 804 b 1400 803 1399 802 l 1399 793 1402 793 "
            + "1404 793 1404 796 l 1407 796 1407 787 1404 787 b 1404 790 "
            + "1402 791 1399 791 1399 782 1401 781 1404 781 1406 781 1407 785 "
            + "1410 785 1408 779 1391 779 1391 780 1394 780 1394 783 1394 801 "
            + "1394 803 1393 804 1391 804 l 1391 806 1409 806 c)\\pos(1475.5,819.5)"
            + "\\blur0.5\\t(3899,0,1,\\alpha&HFF&)}Einherjar";

        assertTrue(ProcessarArquivoUseCase.deveBloquearAntesDoLlm("Ep Title", linha, 2));
    }

    @Test
    void naoBloqueiaTituloSimplesComPosETransformacao() {
        String linha = "{\\pos(1565.5,822.5)\\c&H000000&\\blur0.7\\t(4188,0,1,\\1a&HFF&)}Prologue";

        assertFalse(ProcessarArquivoUseCase.deveBloquearAntesDoLlm("Ep Title", linha, 1));
    }

    @Test
    void detectaCaminhoDeLegendaJaTraduzida() {
        assertTrue(ProcessarArquivoUseCase.caminhoPareceLegendaTraduzida(
            Path.of("C:\\animes\\DanMachi\\legendas_ptbr\\episodio_PT-BR.ass")));
        assertTrue(ProcessarArquivoUseCase.caminhoPareceLegendaTraduzida(
            Path.of("C:\\animes\\DanMachi\\traducao_ptbr\\episodio.ass")));
        assertFalse(ProcessarArquivoUseCase.caminhoPareceLegendaTraduzida(
            Path.of("C:\\animes\\DanMachi\\legendas_eng\\episodio_ENG.ass")));
    }
}
