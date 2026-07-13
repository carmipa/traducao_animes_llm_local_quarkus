package org.traducao.projeto.revisaoLore.domain;

import java.util.List;

public record ResultadoRevisaoLore(
    StatusRevisaoLore status,
    int arquivosAnalisados,
    int arquivosAlterados,
    int falasAuditadas,
    int falasSinalizadas,
    int falasCorrigidas,
    int falasSemAlteracao,
    int falasSemResposta,
    int falasDescartadas,
    int falasPendentes,
    int totalErros,
    List<String> erros,
    String caminhoRelatorioJson
) {
    public boolean teveErros() {
        return totalErros > 0;
    }
}
