package org.traducao.projeto.telemetria;

/**
 * Fotografia sanitizada do ambiente que gerou o snapshot público.
 * Nao inclui usuario, hostname, IP, serial, MAC, caminhos ou IDs de hardware.
 */
public record AmbienteExecucaoDataset(
    String fabricante,
    String modeloMaquina,
    String cpu,
    String gpuPrincipal,
    String gpuDetectadaSistema,
    Integer ramTotalGb,
    String sistemaOperacional,
    String arquitetura,
    boolean hardwareColetadoAutomaticamente,
    boolean gpuPublicaConfigurada
) {}
