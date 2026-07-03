package org.traducao.projeto.revisaoLore.infrastructure;

import org.springframework.stereotype.Component;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Persiste o log linha a linha da sessao de revisao de lore em disco,
 * no mesmo padrao de relatorios das demais operacoes do pipeline.
 */
@Component
public class RevisaoLoreLogPersistencia {

    private static final DateTimeFormatter TIMESTAMP =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public Path salvarLogSessao(Path pastaEntrada, List<String> linhas) throws IOException {
        Path pastaRelatorios = TelemetriaService.resolverPastaRelatorios(pastaEntrada);
        Files.createDirectories(pastaRelatorios);
        String timestamp = TIMESTAMP.format(LocalDateTime.now());
        Path arquivo = pastaRelatorios.resolve("revisao_lore_sessao_" + timestamp + ".log");
        String conteudo = String.join(System.lineSeparator(), linhas);
        Files.writeString(arquivo, conteudo, StandardCharsets.UTF_8);
        return arquivo.toAbsolutePath();
    }
}
