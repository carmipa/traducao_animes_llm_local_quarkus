package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.traducao.domain.exceptions.AlucinacaoDetectadaException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidadorTraducaoServiceTest {

    private final ValidadorTraducaoService validador = new ValidadorTraducaoService();

    @Test
    void rejeitaRotuloTraducaoNoInicio() {
        // Caso real (Gundam Narrative): LLM rotulou a resposta em vez de só traduzir.
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("Tradução: {\\r\\pos(488,23)}ep"));
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("Traducao : Ele nunca vai desistir."));
    }

    @Test
    void rejeitaMarcadorErroTraducaoLegado() {
        // Caso real (G-Reconguista): marcador do pipeline Python antigo na legenda final.
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("[ERRO_TRADUCAO: The Garanden!]"));
    }

    @Test
    void aceitaFalaComPalavraTraducaoNoMeio() {
        assertDoesNotThrow(() ->
            validador.validarFala("A tradução deste documento levou anos."));
    }

    @Test
    void rejeitaResiduoInglesEmFalaMista() {
        // Caso real (86): linha metade PT, metade EN.
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("Se você terminou sua missão, it's seu dever me dar um relatório."));
    }

    @Test
    void aceitaFalaLimpaEmPortugues() {
        assertDoesNotThrow(() ->
            validador.validarFala("Com força e esforço, vamos vencer esta batalha."));
    }

    @Test
    void aceitaComentarioAssEmInglesDentroDeChaves() {
        // Caso real (DanMachi): comentários de fansub no original são preservados
        // e não são texto visível — não podem disparar resíduo.
        assertDoesNotThrow(() ->
            validador.validarFala("Melhor levar-me com você. {Yes, ma'am}"));
        assertDoesNotThrow(() ->
            validador.validarFala("Vamos embora daqui. {it's a pun with the previous line}"));
    }

    @Test
    void rejeitaPreambuloDepoisDeTagAss() {
        // A âncora ^ do padrão de preâmbulo deve valer para o texto VISÍVEL,
        // não para a string crua começando com {\i1}.
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("{\\i1}Tradução: Ele nunca vai desistir.{\\i0}"));
    }
}
