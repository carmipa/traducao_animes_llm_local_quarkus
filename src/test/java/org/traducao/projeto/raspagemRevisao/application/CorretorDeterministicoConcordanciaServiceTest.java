package org.traducao.projeto.raspagemRevisao.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: comprova as correções locais que devem preceder o Nemo.
 * <p>INVARIANTES DO DOMÍNIO: somente o trecho objetivo muda; restante da fala e
 * pontuação permanecem intactos.
 * <p>COMPORTAMENTO EM CASO DE FALHA: proposta ausente ou ampla reprova o teste.
 */
class CorretorDeterministicoConcordanciaServiceTest {

    private final CorretorDeterministicoConcordanciaService corretor =
        new CorretorDeterministicoConcordanciaService();

    /**
     * PROPÓSITO DE NEGÓCIO: restaura a expressão brasileira deformada pelo Nemo.
     * <p>INVARIANTES DO DOMÍNIO: itálico ASS permanece byte a byte.
     * <p>COMPORTAMENTO EM CASO DE FALHA: resultado divergente reprova o teste.
     */
    @Test
    void corrigeGracasAoDeus() {
        assertEquals("{\\i1}Graças a Deus.",
            corretor.corrigir("Thank goodness.", "{\\i1}Graças ao Deus.").orElseThrow());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: corrige troca objetiva de pai por mãe.
     * <p>INVARIANTES DO DOMÍNIO: apenas o parentesco incompatível é substituído.
     * <p>COMPORTAMENTO EM CASO DE FALHA: ausência de proposta reprova o teste.
     */
    @Test
    void corrigeParentescoInvertido() {
        assertEquals("Meu pai precisa saber disso!",
            corretor.corrigir("My dad needs to know!", "Minha mãe precisa saber disso!")
                .orElseThrow());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: desfaz a agressividade introduzida no incidente real.
     * <p>INVARIANTES DO DOMÍNIO: a regra exige o eufemismo/erro `son of a hitch`.
     * <p>COMPORTAMENTO EM CASO DE FALHA: palavrão preservado reprova o teste.
     */
    @Test
    void removePalavraoIntroduzidoNoIncidente() {
        assertEquals("Filho da mãe!",
            corretor.corrigir("You son of a hitch!", "Filho da puta!").orElseThrow());
        assertTrue(corretor.corrigir("You son of a bitch!", "Filho da puta!").isEmpty());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: traduz somente o artigo antes de um termo Gundam canônico.
     * <p>INVARIANTES DO DOMÍNIO: `mobile suit` e a tag ASS permanecem intactos.
     * <p>COMPORTAMENTO EM CASO DE FALHA: alteração do termo reprova o teste.
     */
    @Test
    void traduzArtigoAntesDeMobileSuitCanonico() {
        assertEquals("{\\fad(0,1354)}Um mobile suit...",
            corretor.corrigir(
                "{\\fad(0,1354)}A mobile suit...",
                "{\\fad(0,1354)}A mobile suit...").orElseThrow());
    }
}
