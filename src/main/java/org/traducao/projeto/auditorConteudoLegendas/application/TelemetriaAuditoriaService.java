package org.traducao.projeto.auditorConteudoLegendas.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.domain.AuditoriaConteudoRelatorioJson;
import org.traducao.projeto.auditorConteudoLegendas.domain.RelatorioAuditoriaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.infrastructure.AuditoriaConteudoPersistencia;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: transforma cada Análise de Legenda em telemetria e
 * dataset JSON pesquisável, incluindo os formatos efetivamente processados.
 * <p>INVARIANTES DO DOMÍNIO: métricas e relatório persistido descrevem a mesma
 * execução e os mesmos arquivos.
 * <p>COMPORTAMENTO EM CASO DE FALHA: falha de persistência é registrada, mas
 * não invalida o resultado em memória da auditoria.
 */
@ApplicationScoped
public class TelemetriaAuditoriaService {

    public static final String TIPO_OPERACAO = "Auditoria de Conteudo de Legendas";

    private static final Logger log = LoggerFactory.getLogger(TelemetriaAuditoriaService.class);

    private final TelemetriaService telemetriaService;
    private final AuditoriaConteudoPersistencia persistencia;

    @Inject
    public TelemetriaAuditoriaService(
        TelemetriaService telemetriaService,
        AuditoriaConteudoPersistencia persistencia
    ) {
        this.telemetriaService = telemetriaService;
        this.persistencia = persistencia;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: registra resultado, formatos e anomalias para
     * acompanhamento operacional e melhoria futura das regras.
     * <p>INVARIANTES DO DOMÍNIO: o JSON e a operação agregada compartilham os
     * mesmos contadores e formatos.
     * <p>COMPORTAMENTO EM CASO DE FALHA: retorna {@code null} se o JSON não
     * puder ser salvo, mantendo a telemetria em memória quando possível.
     */
    public String registrar(
        RelatorioAuditoriaConteudo relatorio,
        Path caminhoOriginal,
        Path caminhoTraduzido,
        long duracaoMs
    ) {
        int totalAnomalias = relatorio.getAnomalias().size();
        long criticas = relatorio.getAnomalias().stream()
            .filter(a -> a.severidade() == AnomaliaConteudo.TipoSeveridade.CRITICAL)
            .count();

        String detalhe = caminhoTraduzido.toAbsolutePath()
            + " | formatoOriginal=" + relatorio.getFormatoOriginal()
            + " | formatoTraduzido=" + relatorio.getFormatoTraduzido()
            + " | anomalias=" + totalAnomalias
            + " | criticas=" + criticas;

        OperacaoTelemetria operacao = TelemetriaService.criarOperacao(
            TIPO_OPERACAO,
            detalhe,
            duracaoMs,
            1,
            totalAnomalias,
            0
        );

        AuditoriaConteudoRelatorioJson json = new AuditoriaConteudoRelatorioJson(
            "auditoria_conteudo",
            operacao,
            relatorio.getArquivoOriginal(),
            relatorio.getArquivoTraduzido(),
            relatorio.getFormatoOriginal(),
            relatorio.getFormatoTraduzido(),
            relatorio.isLimpo(),
            totalAnomalias,
            duracaoMs,
            List.copyOf(relatorio.getAnomalias())
        );

        Path pastaRelatorios = resolverPastaRelatorios(caminhoTraduzido);
        String caminhoJson = null;

        try {
            Path arquivo = persistencia.salvarRelatorioJson(pastaRelatorios, json);
            caminhoJson = arquivo.toString();
            telemetriaService.registrarOperacao(operacao);
            telemetriaService.salvar(TelemetriaService.resolverPastaRelatorios(pastaRelatorios));
        } catch (IOException e) {
            log.warn("Falha ao salvar relatorio JSON da auditoria de conteudo: {}", e.getMessage());
            telemetriaService.registrarOperacao(operacao);
        }

        if (relatorio.isLimpo()) {
            log.info("Auditoria de conteudo limpa: {} ({} ms)", relatorio.getArquivoTraduzido(), duracaoMs);
            System.out.println("[Auditoria Conteudo] Arquivo limpo: " + relatorio.getArquivoTraduzido());
        } else {
            log.warn("Auditoria de conteudo detectou {} anomalia(s) em {} ({} ms)",
                totalAnomalias, relatorio.getArquivoTraduzido(), duracaoMs);
            System.out.println("[Auditoria Conteudo] " + totalAnomalias + " anomalia(s) em "
                + relatorio.getArquivoTraduzido());
            for (AnomaliaConteudo anomalia : relatorio.getAnomalias()) {
                System.out.println("  [" + anomalia.severidade() + "] " + anomalia.regra()
                    + " — " + anomalia.descricao());
            }
        }

        if (caminhoJson != null) {
            System.out.println("[Auditoria Conteudo] Relatorio JSON: " + caminhoJson);
        }

        return caminhoJson;
    }

    static Path resolverPastaRelatorios(Path caminhoTraduzido) {
        Path pai = caminhoTraduzido.getParent();
        return pai != null ? pai : Path.of("auditoria_conteudo");
    }
}
