package org.traducao.projeto.raspagemCorrecao.application;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * PROPÓSITO DE NEGÓCIO: prova que a contingência online preserva terminologia
 * oficial declarada na lore em vez de produzir traduções literais destrutivas.
 *
 * <p>INVARIANTES DO DOMÍNIO: termos explícitos e regra “Manter sempre” são
 * protegidos; marcador perdido invalida a resposta.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer termo alterado ou marcador aceito
 * indevidamente reprova o teste.
 */
class ProtetorTermosLoreServiceTest {

    private final ProtetorTermosLoreService service = new ProtetorTermosLoreService();

    @Test
    void preservaTermosDaRegraManterSempre() {
        String lore = "- Manter sempre em inglês ou forma oficial: Sleeves, Psycho-Frame, Phenex.";
        var protegido = service.mascarar(
            "These guys are Sleeves and use a psycho-frame.", lore, Set.of());

        assertFalse(protegido.textoMascarado().contains("Sleeves"));
        String resposta = protegido.textoMascarado()
            .replace("These guys are", "Esses caras são")
            .replace("and use a", "e usam um");
        assertEquals(
            "Esses caras são Sleeves e usam um psycho-frame.",
            service.restaurar(resposta, protegido));
    }

    @Test
    void rejeitaRespostaQuePerdeMarcadorDeLore() {
        var protegido = service.mascarar("Protect Phenex!", "", Set.of("Phenex"));
        assertNull(service.restaurar("Proteja a Fênix!", protegido));
    }
}
