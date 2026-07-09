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

    /** Metadados públicos e sanitizados do ambiente de execução. */
    private Hardware hardware = new Hardware();

    public TelemetriaDatasetProperties() {
    }

    public String repositorioLocal() { return repositorioLocal; }
    public String getRepositorioLocal() { return repositorioLocal; }
    public void setRepositorioLocal(String repositorioLocal) { this.repositorioLocal = repositorioLocal; }

    public String repositorioRemoto() { return repositorioRemoto; }
    public String getRepositorioRemoto() { return repositorioRemoto; }
    public void setRepositorioRemoto(String repositorioRemoto) { this.repositorioRemoto = repositorioRemoto; }

    public Hardware hardware() { return hardware; }
    public Hardware getHardware() { return hardware; }
    public void setHardware(Hardware hardware) { this.hardware = hardware != null ? hardware : new Hardware(); }

    public static class Hardware {
        /** Inclui o bloco ambienteExecucao no JSON público do dataset. */
        private boolean publicarAmbienteExecucao = true;

        /** Usa detecção local por SO quando disponível. */
        private boolean permitirDeteccaoAutomatica = true;

        /** Nome público da GPU quando o driver reporta outro identificador. */
        private String gpuPublica = "";

        public boolean publicarAmbienteExecucao() { return publicarAmbienteExecucao; }
        public boolean isPublicarAmbienteExecucao() { return publicarAmbienteExecucao; }
        public void setPublicarAmbienteExecucao(boolean publicarAmbienteExecucao) {
            this.publicarAmbienteExecucao = publicarAmbienteExecucao;
        }

        public boolean permitirDeteccaoAutomatica() { return permitirDeteccaoAutomatica; }
        public boolean isPermitirDeteccaoAutomatica() { return permitirDeteccaoAutomatica; }
        public void setPermitirDeteccaoAutomatica(boolean permitirDeteccaoAutomatica) {
            this.permitirDeteccaoAutomatica = permitirDeteccaoAutomatica;
        }

        public String gpuPublica() { return gpuPublica; }
        public String getGpuPublica() { return gpuPublica; }
        public void setGpuPublica(String gpuPublica) { this.gpuPublica = gpuPublica; }
    }
}
