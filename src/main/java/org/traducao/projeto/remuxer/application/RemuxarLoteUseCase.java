package org.traducao.projeto.remuxer.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.remuxer.domain.RelatorioRemux;
import org.traducao.projeto.remuxer.domain.RemuxTarefa;
import org.traducao.projeto.remuxer.domain.RemuxerException;
import org.traducao.projeto.remuxer.infrastructure.adapters.MkvmergeAdapter;
import org.traducao.projeto.remuxer.presentation.ui.ConsoleRemuxerLogger;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Service
public class RemuxarLoteUseCase {

    private static final Logger log = LoggerFactory.getLogger(RemuxarLoteUseCase.class);

    private final MkvmergeAdapter mkvmergeAdapter;
    private final MapeadorMidiaService mapeadorMidiaService;
    private final ConsoleRemuxerLogger console;
    private final TelemetriaService telemetriaService;

    public RemuxarLoteUseCase(MkvmergeAdapter mkvmergeAdapter, MapeadorMidiaService mapeadorMidiaService,
            ConsoleRemuxerLogger console, TelemetriaService telemetriaService) {
        this.mkvmergeAdapter = mkvmergeAdapter;
        this.mapeadorMidiaService = mapeadorMidiaService;
        this.console = console;
        this.telemetriaService = telemetriaService;
    }

    public RelatorioRemux executar(Path pastaVideos, Path pastaLegendas) {
        return executar(pastaVideos, pastaLegendas, 0);
    }

    public RelatorioRemux executar(Path pastaVideos, Path pastaLegendas, long sincronismoMs) {
        long inicioMs = System.currentTimeMillis();
        RelatorioRemux relatorio = executarInterno(pastaVideos, pastaLegendas, sincronismoMs);

        // Registra a operação no painel de telemetria como os demais módulos —
        // inclusive nas saídas antecipadas por erro de infraestrutura.
        telemetriaService.registrarOperacao(TelemetriaService.criarOperacao(
            "Remux (mkvmerge)",
            "Videos: " + pastaVideos + " | Legendas: " + pastaLegendas,
            System.currentTimeMillis() - inicioMs,
            relatorio.getMkvDetectados(),
            relatorio.getLegendasPareadas(),
            relatorio.getMkvProcessadosSucesso()
        ));
        return relatorio;
    }

    private RelatorioRemux executarInterno(Path pastaVideos, Path pastaLegendas, long sincronismoMs) {
        RelatorioRemux relatorio = new RelatorioRemux();

        if (sincronismoMs != 0) {
            log.info("Sincronismo manual de legenda aplicado: {}ms", sincronismoMs);
            console.info("Sincronismo manual de legenda aplicado: " + sincronismoMs + "ms");
        }

        try {
            mkvmergeAdapter.validarInfraestrutura();
        } catch (RemuxerException e) {
            log.error("Falha na validação de ambiente: {}", e.getMessage());
            console.erro("Falha na validação de ambiente: " + e.getMessage());
            relatorio.registrarErroInfra();
            relatorio.finalizar();
            return relatorio;
        }

        if (!Files.isDirectory(pastaVideos) || !Files.isDirectory(pastaLegendas)) {
            log.error("Pasta de vídeos ou de legendas não encontrada. videos={}, legendas={}", pastaVideos, pastaLegendas);
            console.erro("Pasta de vídeos ou de legendas não encontrada. videos=" + pastaVideos + ", legendas=" + pastaLegendas);
            relatorio.registrarErroInfra();
            relatorio.finalizar();
            return relatorio;
        }

        Path pastaSaida = pastaVideos.resolve("mkv_final_ptbr");
        try {
            Files.createDirectories(pastaSaida);
        } catch (IOException e) {
            log.error("Não foi possível criar pasta de saída: {}", pastaSaida);
            console.erro("Não foi possível criar pasta de saída: " + pastaSaida);
            relatorio.registrarErroInfra();
            relatorio.finalizar();
            return relatorio;
        }

        List<RemuxTarefa> fila = mapeadorMidiaService.construirFilaProcessamento(pastaVideos, pastaLegendas, pastaSaida);

        // Simular contagem de mkvs totais vs ignorados (simplificado)
        try (Stream<Path> stream = Files.list(pastaVideos)) {
            long totalMkvs = stream.filter(p -> p.toString().toLowerCase().endsWith(".mkv")).count();
            relatorio.registrarDeteccao((int) totalMkvs, fila.size());
            for (int i = 0; i < (totalMkvs - fila.size()); i++) relatorio.registrarIgnorado();
        } catch (IOException e) {
            log.warn("Falha ao contar mkvs totais");
            console.aviso("Falha ao contar mkvs totais");
        }

        console.info("Vídeos detectados: " + relatorio.getMkvDetectados() + " | Pareados com legenda: " + fila.size());

        if (fila.isEmpty()) {
            log.warn("Nenhum arquivo pareado. Fila vazia.");
            console.aviso("Nenhum arquivo pareado. Fila vazia.");
            relatorio.finalizar();
            return relatorio;
        }

        for (RemuxTarefa tarefa : fila) {
            try {
                log.info("Remuxando vídeo: {} (pareado com {})", tarefa.nomeVideo(), tarefa.caminhoLegenda().getFileName());
                console.info("Remuxando vídeo: " + tarefa.nomeVideo() + " (pareado com " + tarefa.caminhoLegenda().getFileName() + ")");

                long tamanhoLegenda = Files.size(tarefa.caminhoLegenda());
                if (tamanhoLegenda == 0) {
                    String msg = "Legenda traduzida vazia/corrompida (0 bytes), pulando remux: " + tarefa.caminhoLegenda()
                        + ". Refaça a tradução ou a cura de tags para regenerar esse arquivo antes de remuxar.";
                    log.error(msg);
                    console.erro(msg);
                    relatorio.registrarErroLegendaInvalida();
                    continue;
                }

                mkvmergeAdapter.executarRemux(tarefa, sincronismoMs);
                long bytes = Files.size(tarefa.caminhoSaida());
                relatorio.registrarSucesso(bytes);
                log.info("MKV finalizado com sucesso: {}", tarefa.caminhoSaida().getFileName());
                console.sucesso("MKV finalizado com sucesso: " + tarefa.caminhoSaida().getFileName());
            } catch (RemuxerException e) {
                log.error("Erro runtime do mkvmerge no arquivo {}: {}", tarefa.nomeVideo(), e.getMessage());
                console.erro("Erro runtime do mkvmerge no arquivo " + tarefa.nomeVideo() + ": " + e.getMessage());
                relatorio.registrarErroRuntime();
            } catch (Exception e) {
                log.error("Erro inesperado no arquivo {}: {}", tarefa.nomeVideo(), e.getMessage());
                console.erro("Erro inesperado no arquivo " + tarefa.nomeVideo() + ": " + e.getMessage());
                relatorio.registrarErroInesperado();
            }
        }

        relatorio.finalizar();
        return relatorio;
    }
}
