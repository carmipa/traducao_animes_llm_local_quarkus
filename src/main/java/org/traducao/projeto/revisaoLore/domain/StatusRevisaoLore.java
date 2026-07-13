package org.traducao.projeto.revisaoLore.domain;

/**
 * PROPÓSITO DE NEGÓCIO: distingue o desfecho real de uma execução de revisão de
 * lore, substituindo o antigo "[SUCESSO]" incondicional. Permite ao operador
 * saber, num relance no console/relatório, se o job realmente concluiu, se
 * concluiu deixando pendências, se foi cancelado, se falhou ou se nem havia
 * arquivos para processar.
 *
 * <p>INVARIANTES DO DOMÍNIO: exatamente um status descreve cada execução. Só
 * {@link #FALHOU} pode acompanhar uma exceção propagada; os demais representam
 * retornos normais do use case. {@link #CONCLUIDO} exige zero erros e zero falas
 * pendentes.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: é um enum imutável; não dispara exceções.
 * O rótulo textual nunca é nulo.
 */
public enum StatusRevisaoLore {
    CONCLUIDO("Concluído"),
    CONCLUIDO_COM_PENDENCIAS("Concluído com pendências"),
    FALHOU("Falhou"),
    CANCELADO("Cancelado"),
    SEM_ARQUIVOS("Sem arquivos");

    private final String rotulo;

    StatusRevisaoLore(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
