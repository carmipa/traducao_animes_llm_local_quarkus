package org.traducao.projeto.telemetria;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TelemetriaDatasetPropertiesTest {

    @Inject
    TelemetriaDatasetProperties propriedades;

    @Test
    void carregaConfiguracaoDeHardwareDoApplicationYaml() {
        assertTrue(propriedades.hardware().publicarAmbienteExecucao());
        assertTrue(propriedades.hardware().permitirDeteccaoAutomatica());
        assertEquals("NVIDIA RTX 5600", propriedades.hardware().gpuPublica());
    }
}
