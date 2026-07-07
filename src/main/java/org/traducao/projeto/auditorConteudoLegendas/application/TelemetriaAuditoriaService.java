package org.traducao.projeto.auditorConteudoLegendas.application;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traducao.projeto.auditorConteudoLegendas.domain.RelatorioAuditoriaConteudo;

@ApplicationScoped
public class TelemetriaAuditoriaService {
    
    private static final Logger log = LoggerFactory.getLogger(TelemetriaAuditoriaService.class);

    public void registrar(RelatorioAuditoriaConteudo relatorio) {
        if (relatorio.isLimpo()) {
            log.info("TELEMETRIA: Auditoria Limpa para {}", relatorio.getArquivoTraduzido());
        } else {
            log.warn("TELEMETRIA: Auditoria Detectou {} anomalias em {}", 
                relatorio.getAnomalias().size(), relatorio.getArquivoTraduzido());
            // Aqui poderíamos salvar em um banco SQLite ou um arquivo JSON acumulativo
        }
    }
}
