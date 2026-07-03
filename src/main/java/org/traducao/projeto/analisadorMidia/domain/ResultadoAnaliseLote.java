package org.traducao.projeto.analisadorMidia.domain;

import java.nio.file.Path;
import java.util.List;

/**
 * Resultado de uma execução de auditoria sobre um lote de vídeos, incluindo o
 * caminho do relatório de texto efetivamente gravado em disco (individual,
 * se um único arquivo foi analisado, ou consolidado, se foram vários).
 * {@code relatorioPrincipal} é {@code null} se nada foi gravado (ex.: falha de IO).
 */
public record ResultadoAnaliseLote(
    List<AuditoriaResultado> resultados,
    Path relatorioPrincipal
) {}
