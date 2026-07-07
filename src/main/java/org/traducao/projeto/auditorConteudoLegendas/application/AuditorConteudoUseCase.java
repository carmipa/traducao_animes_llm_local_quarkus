package org.traducao.projeto.auditorConteudoLegendas.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.domain.AuditoriaException;
import org.traducao.projeto.auditorConteudoLegendas.domain.RegraAuditoriaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.domain.RelatorioAuditoriaConteudo;
import org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda;
import org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaAss;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@ApplicationScoped
public class AuditorConteudoUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuditorConteudoUseCase.class);

    private final LeitorLegendaAss leitorLegendaAss;
    private final Instance<RegraAuditoriaConteudo> regras;
    private final TelemetriaAuditoriaService telemetria;

    @Inject
    public AuditorConteudoUseCase(
        LeitorLegendaAss leitorLegendaAss,
        Instance<RegraAuditoriaConteudo> regras,
        TelemetriaAuditoriaService telemetria
    ) {
        this.leitorLegendaAss = leitorLegendaAss;
        this.regras = regras;
        this.telemetria = telemetria;
    }

    public RelatorioAuditoriaConteudo auditar(Path caminhoOriginal, Path caminhoTraduzido) {
        validarArquivos(caminhoOriginal, caminhoTraduzido);

        long inicioMs = System.currentTimeMillis();
        log.info("Iniciando auditoria de conteudo: {} vs {}", caminhoOriginal, caminhoTraduzido);
        System.out.println("\n=== Auditoria de Conteudo de Legendas ===");
        System.out.println("Original : " + caminhoOriginal.toAbsolutePath());
        System.out.println("Traduzido: " + caminhoTraduzido.toAbsolutePath());

        try {
            RelatorioAuditoriaConteudo relatorio = new RelatorioAuditoriaConteudo(
                caminhoOriginal.getFileName().toString(),
                caminhoTraduzido.getFileName().toString()
            );

            DocumentoLegenda docOriginal = leitorLegendaAss.ler(caminhoOriginal);
            DocumentoLegenda docTraduzido = leitorLegendaAss.ler(caminhoTraduzido);

            int regrasExecutadas = 0;
            for (RegraAuditoriaConteudo regra : regras) {
                regrasExecutadas++;
                List<AnomaliaConteudo> anomaliasEncontradas = regra.auditar(docOriginal, docTraduzido);
                log.debug("Regra '{}' encontrou {} anomalia(s)", regra.getNome(), anomaliasEncontradas.size());
                for (AnomaliaConteudo anomalia : anomaliasEncontradas) {
                    relatorio.adicionarAnomalia(anomalia);
                }
            }

            long duracaoMs = System.currentTimeMillis() - inicioMs;
            String caminhoJson = telemetria.registrar(relatorio, caminhoOriginal, caminhoTraduzido, duracaoMs);
            relatorio.definirMetadados(duracaoMs, caminhoJson, regrasExecutadas);

            log.info("Auditoria de conteudo concluida em {} ms — {} anomalia(s), {} regra(s)",
                duracaoMs, relatorio.getAnomalias().size(), regrasExecutadas);
            System.out.println("Regras executadas: " + regrasExecutadas);
            System.out.println("Anomalias: " + relatorio.getAnomalias().size());
            System.out.println("Duracao: " + duracaoMs + " ms");

            return relatorio;
        } catch (AuditoriaException e) {
            throw e;
        } catch (Exception e) {
            log.error("Falha na auditoria de conteudo ({} vs {}): {}",
                caminhoOriginal, caminhoTraduzido, e.getMessage(), e);
            throw new AuditoriaException("Falha ao auditar os arquivos: " + e.getMessage(), e);
        }
    }

    private void validarArquivos(Path caminhoOriginal, Path caminhoTraduzido) {
        if (!Files.isRegularFile(caminhoOriginal)) {
            throw new AuditoriaException("Arquivo original nao encontrado: " + caminhoOriginal);
        }
        if (!Files.isRegularFile(caminhoTraduzido)) {
            throw new AuditoriaException("Arquivo traduzido nao encontrado: " + caminhoTraduzido);
        }
        String nomeOrig = caminhoOriginal.getFileName().toString().toLowerCase();
        String nomeTrad = caminhoTraduzido.getFileName().toString().toLowerCase();
        if (!nomeOrig.endsWith(".ass") && !nomeOrig.endsWith(".ssa")) {
            throw new AuditoriaException("Arquivo original deve ser .ass ou .ssa: " + caminhoOriginal);
        }
        if (!nomeTrad.endsWith(".ass") && !nomeTrad.endsWith(".ssa")) {
            throw new AuditoriaException("Arquivo traduzido deve ser .ass ou .ssa: " + caminhoTraduzido);
        }
    }
}
