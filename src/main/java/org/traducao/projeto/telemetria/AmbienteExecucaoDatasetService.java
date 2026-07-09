package org.traducao.projeto.telemetria;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traducao.projeto.core.util.ProcessoExternoUtil;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * Detecta apenas metadados publicáveis do ambiente de execução.
 */
@ApplicationScoped
public class AmbienteExecucaoDatasetService {

    private static final Logger log = LoggerFactory.getLogger(AmbienteExecucaoDatasetService.class);
    private static final Duration TIMEOUT_DETECCAO = Duration.ofSeconds(5);

    private final ObjectMapper mapper = new ObjectMapper();

    public AmbienteExecucaoDataset detectar(TelemetriaDatasetProperties.Hardware config) {
        if (config != null && !config.publicarAmbienteExecucao()) {
            return null;
        }

        AmbienteExecucaoDataset detectado = null;
        if (config == null || config.permitirDeteccaoAutomatica()) {
            detectado = detectarWindows();
        }
        if (detectado == null) {
            detectado = detectarFallbackJava();
        }

        String gpuPublica = textoSeguro(config != null ? config.gpuPublica() : null);
        if (gpuPublica == null) {
            return detectado;
        }

        return new AmbienteExecucaoDataset(
            detectado.fabricante(),
            detectado.modeloMaquina(),
            detectado.cpu(),
            gpuPublica,
            detectado.gpuDetectadaSistema(),
            detectado.ramTotalGb(),
            detectado.sistemaOperacional(),
            detectado.arquitetura(),
            detectado.hardwareColetadoAutomaticamente(),
            true
        );
    }

    private AmbienteExecucaoDataset detectarWindows() {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            return null;
        }
        String script = """
            $cs = Get-CimInstance Win32_ComputerSystem
            $cpu = Get-CimInstance Win32_Processor | Select-Object -First 1
            $gpus = @(Get-CimInstance Win32_VideoController |
                Where-Object { $_.Name -and $_.Name -notmatch 'Microsoft|Basic Display' } |
                Select-Object -ExpandProperty Name)
            [pscustomobject]@{
                fabricante = $cs.Manufacturer
                modeloMaquina = $cs.Model
                ramBytes = [int64]$cs.TotalPhysicalMemory
                cpu = $cpu.Name
                gpus = $gpus
            } | ConvertTo-Json -Compress -Depth 4
            """;
        try {
            var resultado = ProcessoExternoUtil.executar(List.of(
                "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", script
            ), TIMEOUT_DETECCAO, true);
            if (resultado.codigoSaida() != 0) {
                log.debug("Deteccao de hardware via PowerShell falhou: {}", saida(resultado));
                return null;
            }
            JsonNode json = mapper.readTree(saida(resultado));
            return new AmbienteExecucaoDataset(
                textoSeguro(json.path("fabricante").asText(null)),
                textoSeguro(json.path("modeloMaquina").asText(null)),
                textoSeguro(json.path("cpu").asText(null)),
                primeiraGpu(json.path("gpus")),
                primeiraGpu(json.path("gpus")),
                arredondarGb(json.path("ramBytes").asLong(0L)),
                textoSeguro(System.getProperty("os.name")),
                textoSeguro(System.getProperty("os.arch")),
                true,
                false
            );
        } catch (Exception e) {
            log.debug("Nao foi possivel detectar hardware via PowerShell.", e);
            return null;
        }
    }

    private AmbienteExecucaoDataset detectarFallbackJava() {
        long memoriaBytes = Runtime.getRuntime().maxMemory();
        try {
            memoriaBytes = ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean())
                .getTotalMemorySize();
        } catch (RuntimeException ignored) {
            // Mantem o fallback do Runtime quando o MXBean nao estiver disponivel.
        }
        return new AmbienteExecucaoDataset(
            null,
            null,
            textoSeguro(System.getenv("PROCESSOR_IDENTIFIER")),
            null,
            null,
            arredondarGb(memoriaBytes),
            textoSeguro(System.getProperty("os.name")),
            textoSeguro(System.getProperty("os.arch")),
            false,
            false
        );
    }

    private static String primeiraGpu(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                String valor = textoSeguro(item.asText(null));
                if (valor != null) {
                    return valor;
                }
            }
            return null;
        }
        return textoSeguro(node.asText(null));
    }

    private static Integer arredondarGb(long bytes) {
        if (bytes <= 0L) {
            return null;
        }
        return Math.toIntExact(Math.round(bytes / 1024.0 / 1024.0 / 1024.0));
    }

    private static String textoSeguro(String valor) {
        if (valor == null) {
            return null;
        }
        String limpo = valor.replaceAll("[\\r\\n\\t]+", " ").replaceAll("\\s+", " ").trim();
        if (limpo.isBlank() || limpo.length() > 120) {
            return null;
        }
        return limpo;
    }

    private static String saida(ProcessoExternoUtil.Resultado resultado) {
        return new String(resultado.stdout(), StandardCharsets.UTF_8);
    }
}
