package org.traducao.projeto.mcp;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.traducao.projeto.analisadorMidia.application.AnalisarMidiaUseCase;
import org.traducao.projeto.analisadorMidia.domain.AuditoriaResultado;
import org.traducao.projeto.analisadorMidia.domain.LegendaInfo;
import org.traducao.projeto.analisadorMidia.domain.ResultadoAnaliseLote;
import org.traducao.projeto.core.execucao.FilaExecucaoPipeline;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Ferramentas MCP (Model Context Protocol) expostas pelo KRONOS via transporte
 * SSE em {@code /mcp/sse}. Clientes MCP (ex.: Claude Code) acionam o pipeline
 * enquanto o servidor web ja esta rodando em modo dev.
 * <p>
 * Toda operação pesada passa pela mesma {@link FilaExecucaoPipeline} da UI: o
 * MCP não é uma porta paralela. Isso garante execução sequencial (MCP e UI não
 * disputam GPU/estado global), torna o job visível a {@code ocupada()} e o deixa
 * cancelável pelo "Parar".
 */
@Singleton
public class KronosMcpTools {

    private final AnalisarMidiaUseCase analisarMidiaUseCase;
    private final FilaExecucaoPipeline filaExecucao;

    @Inject
    public KronosMcpTools(AnalisarMidiaUseCase analisarMidiaUseCase, FilaExecucaoPipeline filaExecucao) {
        this.analisarMidiaUseCase = analisarMidiaUseCase;
        this.filaExecucao = filaExecucao;
    }

    @Tool(name = "ping", description = "Verifica se o servidor MCP do KRONOS esta online e responde.")
    public String ping() {
        return "KRONOS CORE MCP online. Pipeline de traducao de animes (Quarkus) pronto.";
    }

    @Tool(name = "analisar_midia",
          description = "Executa a auditoria tecnica (ffprobe) de um arquivo de video ou de uma pasta com videos: "
                      + "container, faixas de video/audio/legenda e veredicto de sincronia das legendas. "
                      + "Grava relatorios .txt/.json em uma subpasta 'relatorios' ao lado da entrada e retorna um resumo.")
    public String analisarMidia(
            @ToolArg(name = "caminho", description = "Caminho absoluto de um arquivo de video (.mkv/.mp4/...) ou de uma pasta contendo videos.")
            String caminho) {

        if (caminho == null || caminho.isBlank()) {
            return "ERRO: informe o caminho de um arquivo ou pasta.";
        }

        Path entrada = Path.of(caminho.trim());
        if (!Files.exists(entrada)) {
            return "ERRO: caminho nao encontrado: " + entrada.toAbsolutePath();
        }

        // Recusa em vez de enfileirar atrás de um job possivelmente longo: o MCP
        // é síncrono e ficaria pendurado até o outro job terminar. Resposta
        // estruturada de ocupação (equivalente MCP ao HTTP 423 Locked).
        if (filaExecucao.ocupada()) {
            return "OCUPADO: o pipeline do KRONOS ja esta executando outro job. "
                 + "Aguarde a conclusao (ou pare pela interface web) e tente novamente.";
        }

        try {
            ResultadoAnaliseLote lote = filaExecucao.executarEAguardar(
                () -> analisarMidiaUseCase.executar(entrada, null));
            return montarResumo(lote);
        } catch (Exception e) {
            return "ERRO ao analisar '" + entrada + "': " + e.getMessage();
        }
    }

    private String montarResumo(ResultadoAnaliseLote lote) {
        StringBuilder sb = new StringBuilder();
        sb.append("Auditoria concluida: ").append(lote.resultados().size()).append(" arquivo(s).\n");
        if (lote.relatorioPrincipal() != null) {
            sb.append("Relatorio: ").append(lote.relatorioPrincipal().toAbsolutePath()).append('\n');
        }
        sb.append('\n');

        for (AuditoriaResultado r : lote.resultados()) {
            sb.append("- ").append(r.nomeArquivo()).append('\n');
            sb.append("    container: ").append(r.container().formato())
              .append(" | duracao: ").append(formatarSegundos(r.container().duracaoSegundos()))
              .append(" | video: ").append(r.videos().size())
              .append(" | audio: ").append(r.audios().size())
              .append(" | legendas: ").append(r.legendas().size())
              .append('\n');
            for (LegendaInfo leg : r.legendas()) {
                sb.append("    legenda [").append(leg.indexRelativo() + 1).append("] ")
                  .append(leg.idioma()).append(" / ").append(leg.tipoCurto());
                if (leg.veredicto() != null && !"N/A".equals(leg.veredicto())) {
                    sb.append(" -> ").append(leg.veredicto());
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private String formatarSegundos(Double seconds) {
        if (seconds == null || seconds <= 0.0) {
            return "N/A";
        }
        long h = (long) (seconds / 3600.0);
        long m = (long) ((seconds % 3600.0) / 60.0);
        long s = (long) (seconds % 60.0);
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}
