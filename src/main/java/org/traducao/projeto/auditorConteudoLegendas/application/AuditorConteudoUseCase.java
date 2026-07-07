package org.traducao.projeto.auditorConteudoLegendas.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.domain.AuditoriaException;
import org.traducao.projeto.auditorConteudoLegendas.domain.RegraAuditoriaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.domain.RelatorioAuditoriaConteudo;
import org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda;
import org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaAss;

import java.nio.file.Path;
import java.util.List;

@ApplicationScoped
public class AuditorConteudoUseCase {

    private final LeitorLegendaAss leitorLegendaAss;
    private final Instance<RegraAuditoriaConteudo> regras;
    private final TelemetriaAuditoriaService telemetria;

    @Inject
    public AuditorConteudoUseCase(LeitorLegendaAss leitorLegendaAss, 
                                  Instance<RegraAuditoriaConteudo> regras,
                                  TelemetriaAuditoriaService telemetria) {
        this.leitorLegendaAss = leitorLegendaAss;
        this.regras = regras;
        this.telemetria = telemetria;
    }

    public RelatorioAuditoriaConteudo auditar(Path caminhoOriginal, Path caminhoTraduzido) {
        try {
            RelatorioAuditoriaConteudo relatorio = new RelatorioAuditoriaConteudo(
                    caminhoOriginal.getFileName().toString(),
                    caminhoTraduzido.getFileName().toString()
            );

            DocumentoLegenda docOriginal = leitorLegendaAss.ler(caminhoOriginal);
            DocumentoLegenda docTraduzido = leitorLegendaAss.ler(caminhoTraduzido);

            for (RegraAuditoriaConteudo regra : regras) {
                List<AnomaliaConteudo> anomaliasEncontradas = regra.auditar(docOriginal, docTraduzido);
                for (AnomaliaConteudo anomalia : anomaliasEncontradas) {
                    relatorio.adicionarAnomalia(anomalia);
                }
            }

            telemetria.registrar(relatorio);

            return relatorio;
        } catch (Exception e) {
            throw new AuditoriaException("Falha ao auditar os arquivos: " + e.getMessage(), e);
        }
    }
}
