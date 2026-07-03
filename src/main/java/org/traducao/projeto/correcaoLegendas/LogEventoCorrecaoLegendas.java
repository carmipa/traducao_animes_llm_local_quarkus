package org.traducao.projeto.correcaoLegendas;

public record LogEventoCorrecaoLegendas(
    String timestampUtc,
    String nivel,
    String arquivo,
    String mensagem
) {}
