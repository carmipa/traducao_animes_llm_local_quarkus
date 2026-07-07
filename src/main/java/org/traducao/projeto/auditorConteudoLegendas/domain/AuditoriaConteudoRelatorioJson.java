package org.traducao.projeto.auditorConteudoLegendas.domain;

import org.traducao.projeto.telemetria.OperacaoTelemetria;

import java.util.List;

/**
 * Relatório persistido em JSON da auditoria de conteúdo de legendas.
 */
public record AuditoriaConteudoRelatorioJson(
    String tipo,
    OperacaoTelemetria operacao,
    String arquivoOriginal,
    String arquivoTraduzido,
    boolean limpo,
    int totalAnomalias,
    long duracaoMs,
    List<AnomaliaConteudo> anomalias
) {}
