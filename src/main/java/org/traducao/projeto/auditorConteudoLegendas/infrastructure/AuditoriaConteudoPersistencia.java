package org.traducao.projeto.auditorConteudoLegendas.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.enterprise.context.ApplicationScoped;
import org.traducao.projeto.auditorConteudoLegendas.domain.AuditoriaConteudoRelatorioJson;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@ApplicationScoped
public class AuditoriaConteudoPersistencia {

    private static final DateTimeFormatter TIMESTAMP =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ObjectMapper objectMapper;

    public AuditoriaConteudoPersistencia() {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public Path salvarRelatorioJson(Path pastaEntrada, AuditoriaConteudoRelatorioJson relatorio) throws IOException {
        Path pastaRelatorios = TelemetriaService.resolverPastaRelatorios(pastaEntrada);
        Files.createDirectories(pastaRelatorios);
        String timestamp = TIMESTAMP.format(LocalDateTime.now());
        Path arquivo = pastaRelatorios.resolve("auditoria_conteudo_" + timestamp + ".json");
        objectMapper.writeValue(arquivo.toFile(), relatorio);
        return arquivo.toAbsolutePath();
    }
}
