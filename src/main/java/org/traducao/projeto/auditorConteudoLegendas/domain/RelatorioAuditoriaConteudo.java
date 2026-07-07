package org.traducao.projeto.auditorConteudoLegendas.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class RelatorioAuditoriaConteudo {
    private final String arquivoOriginal;
    private final String arquivoTraduzido;
    private final List<AnomaliaConteudo> anomalias = new ArrayList<>();
    private long duracaoMs;
    private String caminhoRelatorioJson;
    private int regrasExecutadas;

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

    @JsonProperty("limpo")
    public boolean isLimpo() {
        return anomalias.isEmpty();
    }

    public long getDuracaoMs() {
        return duracaoMs;
    }

    public String getCaminhoRelatorioJson() {
        return caminhoRelatorioJson;
    }

    public int getRegrasExecutadas() {
        return regrasExecutadas;
    }

    public void definirMetadados(long duracaoMs, String caminhoRelatorioJson, int regrasExecutadas) {
        this.duracaoMs = duracaoMs;
        this.caminhoRelatorioJson = caminhoRelatorioJson;
        this.regrasExecutadas = regrasExecutadas;
    }
}
