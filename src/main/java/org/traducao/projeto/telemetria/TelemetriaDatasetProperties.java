package org.traducao.projeto.telemetria;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do repositório dedicado do dataset público de telemetria
 * (seção {@code telemetria-dataset} do application.yml). O nome segue a
 * convenção da comunidade para datasets de telemetria:
 * {@code [NomeDoSistema]-telemetry-dataset}.
 */
@ConfigurationProperties(prefix = "telemetria-dataset")
public class TelemetriaDatasetProperties {

    /** Pasta local do repositório do dataset (irmã do projeto por padrão). */
    private String repositorioLocal = "../kronos-anime-translation-telemetry-dataset";

    /** Remoto Git para onde o dataset é publicado. */
    private String repositorioRemoto = "https://github.com/carmipa/kronos-anime-translation-telemetry-dataset.git";

    public TelemetriaDatasetProperties() {
    }

    public String repositorioLocal() { return repositorioLocal; }
    public String getRepositorioLocal() { return repositorioLocal; }
    public void setRepositorioLocal(String repositorioLocal) { this.repositorioLocal = repositorioLocal; }

    public String repositorioRemoto() { return repositorioRemoto; }
    public String getRepositorioRemoto() { return repositorioRemoto; }
    public void setRepositorioRemoto(String repositorioRemoto) { this.repositorioRemoto = repositorioRemoto; }
}
