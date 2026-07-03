package org.traducao.projeto.revisaoLore.domain;

import org.traducao.projeto.telemetria.OperacaoTelemetria;

import java.util.List;

/**
 * Relatorio completo da revisao de lore em JSON: telemetria, metricas, contexto e log da sessao.
 */
public record RevisaoLoreRelatorioJson(
    String tipo,
    OperacaoTelemetria operacao,
    ContextoObra contexto,
    PastasOperacao pastas,
    String modo,
    MetricasRevisaoLore metricas,
    List<String> erros,
    List<LogEventoRevisaoLore> eventos
) {
    public record ContextoObra(String id, String nome) {}

    public record PastasOperacao(String originalEn, String traduzidaPtBr) {}

    public record MetricasRevisaoLore(
        long duracaoMs,
        String duracaoFormatada,
        int arquivosAnalisados,
        int arquivosAlterados,
        int falasAuditadas,
        int falasSinalizadas,
        int falasCorrigidas,
        int falasSemAlteracao,
        int totalErros
    ) {}
}
