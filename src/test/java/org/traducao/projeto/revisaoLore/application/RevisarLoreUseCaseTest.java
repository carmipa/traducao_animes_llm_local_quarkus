package org.traducao.projeto.revisaoLore.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RevisarLoreUseCaseTest {

    @Test
    void corrigeShinTraduzidoComoCanelaSemLlm() {
        var corrigida = RevisarLoreUseCase.corrigirLoreDeterministica(
            "Shin! Shinei Nouzen!",
            "Canela! Shinei Nouzen!"
        );

        assertTrue(corrigida.isPresent());
        assertEquals("Shin! Shinei Nouzen!", corrigida.get());
    }

    @Test
    void naoTrocaCanelaQuandoOriginalNaoTemShin() {
        var corrigida = RevisarLoreUseCase.corrigirLoreDeterministica(
            "My leg hurts.",
            "Minha canela dói."
        );

        assertTrue(corrigida.isEmpty());
    }

    @Test
    void corrigeDudRoundsTraduzidoLiteralmente() {
        var corrigida = RevisarLoreUseCase.corrigirLoreDeterministica(
            "Those are dud rounds.",
            "Essas são rodadas aleatórias."
        );
        var cache = RevisarLoreUseCase.corrigirLoreDeterministica(
            "Those dud rounds landed around there.",
            "Aquelas rodadas fracassadas caíram ali perto."
        );

        assertTrue(corrigida.isPresent());
        assertEquals("Essas são munições falhas.", corrigida.get());
        assertTrue(cache.isPresent());
        assertEquals("Aquelas munições falhas caíram ali perto.", cache.get());
    }
}
