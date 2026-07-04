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
}
