package org.traducao.projeto.correcaoLegendas.domain;

import java.util.List;

public record ResultadoCorrecaoLegendas(
    int curados,
    int corrigidosLlm,
    int semAlteracao,
    int semPar,
    int traducaoAusente,
    int totalErros,
    List<String> erros,
    String relatorioJson
) {
    public boolean teveErros() {
        return totalErros > 0;
    }

    public int totalArquivos() {
        return curados + semAlteracao;
    }

    public int totalArquivosAnalisados() {
        return curados + semAlteracao + semPar + totalErros;
    }
}
