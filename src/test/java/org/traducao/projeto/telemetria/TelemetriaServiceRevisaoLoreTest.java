package org.traducao.projeto.telemetria;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetriaServiceRevisaoLoreTest {

    @Test
    void ehRevisaoLoreReconheceTipoOficial() {
        assertTrue(TelemetriaService.ehRevisaoLore("Revisao de Lore (.ass LLM)"));
        assertFalse(TelemetriaService.ehRevisaoLore("Revisão de Legendas"));
    }
}
