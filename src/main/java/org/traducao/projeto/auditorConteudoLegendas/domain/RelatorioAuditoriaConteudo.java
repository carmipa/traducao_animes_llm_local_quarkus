package org.traducao.projeto.auditorConteudoLegendas.domain;

import java.util.ArrayList;
import java.util.List;

public class RelatorioAuditoriaConteudo {
    private final String arquivoOriginal;
    private final String arquivoTraduzido;
    private final List<AnomaliaConteudo> anomalias = new ArrayList<>();

    public RelatorioAuditoriaConteudo(String arquivoOriginal, String arquivoTraduzido) {
        this.arquivoOriginal = arquivoOriginal;
        this.arquivoTraduzido = arquivoTraduzido;
    }

    public void adicionarAnomalia(AnomaliaConteudo anomalia) {
        anomalias.add(anomalia);
    }

    public List<AnomaliaConteudo> getAnomalias() {
        return anomalias;
    }

    public String getArquivoOriginal() {
        return arquivoOriginal;
    }

    public String getArquivoTraduzido() {
        return arquivoTraduzido;
    }

    public boolean isLimpo() {
        return anomalias.isEmpty();
    }
}
