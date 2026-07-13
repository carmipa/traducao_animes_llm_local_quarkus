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
import org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaSrt;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: compara uma legenda original com sua versão traduzida
 * e produz um relatório didático com formato, integridade e anomalias.
 * <p>INVARIANTES DO DOMÍNIO: somente arquivos regulares ASS, SSA ou SRT entram
 * na auditoria; cada arquivo é lido pelo parser correspondente ao seu formato.
 * <p>COMPORTAMENTO EM CASO DE FALHA: arquivo ausente, formato não suportado ou
 * erro de leitura gera {@link AuditoriaException} sem relatório parcial.
 */
@ApplicationScoped
public class AuditorConteudoUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuditorConteudoUseCase.class);

    private final LeitorLegendaAss leitorLegendaAss;
    private final LeitorLegendaSrt leitorLegendaSrt;
    private final Instance<RegraAuditoriaConteudo> regras;
    private final TelemetriaAuditoriaService telemetria;

    /**
     * PROPÓSITO DE NEGÓCIO: reúne leitores, regras e telemetria necessários
     * para analisar os formatos suportados por uma única operação.
     * <p>INVARIANTES DO DOMÍNIO: dependências são obrigatórias e os leitores
     * ASS/SSA e SRT permanecem separados.
     * <p>COMPORTAMENTO EM CASO DE FALHA: o contêiner de injeção interrompe a
     * inicialização se alguma dependência não estiver disponível.
     */
    @Inject
    public AuditorConteudoUseCase(
        LeitorLegendaAss leitorLegendaAss,
        LeitorLegendaSrt leitorLegendaSrt,
        Instance<RegraAuditoriaConteudo> regras,
        TelemetriaAuditoriaService telemetria
    ) {
        this.leitorLegendaAss = leitorLegendaAss;
        this.leitorLegendaSrt = leitorLegendaSrt;
        this.regras = regras;
        this.telemetria = telemetria;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: executa a Análise de Legenda e devolve todos os
     * dados necessários para tela, exportação e telemetria.
     * <p>INVARIANTES DO DOMÍNIO: o formato informado no relatório é obtido do
     * próprio caminho validado e determina o leitor usado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: encapsula falhas inesperadas em
     * {@link AuditoriaException}; falhas de domínio preservam sua mensagem.
     */
    public RelatorioAuditoriaConteudo auditar(Path caminhoOriginal, Path caminhoTraduzido) {
        String formatoOriginal = validarArquivo(caminhoOriginal, "original");
        String formatoTraduzido = validarArquivo(caminhoTraduzido, "traduzido");

        long inicioMs = System.currentTimeMillis();
        log.info("Iniciando auditoria de conteudo: {} vs {}", caminhoOriginal, caminhoTraduzido);
        System.out.println("\n=== Auditoria de Conteudo de Legendas ===");
        System.out.println("Original : " + caminhoOriginal.toAbsolutePath());
        System.out.println("Formato original: " + formatoOriginal);
        System.out.println("Traduzido: " + caminhoTraduzido.toAbsolutePath());
        System.out.println("Formato traduzido: " + formatoTraduzido);

        try {
            RelatorioAuditoriaConteudo relatorio = new RelatorioAuditoriaConteudo(
                caminhoOriginal.getFileName().toString(),
                caminhoTraduzido.getFileName().toString(),
                formatoOriginal,
                formatoTraduzido
            );

            DocumentoLegenda docOriginal = lerLegenda(caminhoOriginal, formatoOriginal);
            DocumentoLegenda docTraduzido = lerLegenda(caminhoTraduzido, formatoTraduzido);

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

    /**
     * PROPÓSITO DE NEGÓCIO: valida a entrada e devolve o rótulo de formato que
     * aparecerá no relatório.
     * <p>INVARIANTES DO DOMÍNIO: somente ASS, SSA e SRT são aceitos; o rótulo é
     * sempre retornado em maiúsculas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: lança {@link AuditoriaException} com o
     * papel do arquivo e a lista dos formatos suportados.
     */
    private String validarArquivo(Path caminho, String papel) {
        if (!Files.isRegularFile(caminho)) {
            throw new AuditoriaException("Arquivo " + papel + " nao encontrado: " + caminho);
        }
        String nome = caminho.getFileName().toString().toLowerCase();
        if (nome.endsWith(".ass")) return "ASS";
        if (nome.endsWith(".ssa")) return "SSA";
        if (nome.endsWith(".srt")) return "SRT";
        throw new AuditoriaException("Formato do arquivo " + papel
            + " nao suportado. Use ASS, SSA ou SRT: " + caminho);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: direciona cada artefato ao parser compatível para
     * que a auditoria use o mesmo modelo de documento interno.
     * <p>INVARIANTES DO DOMÍNIO: SRT usa o leitor SubRip; ASS e SSA usam o
     * leitor Advanced SubStation Alpha.
     * <p>COMPORTAMENTO EM CASO DE FALHA: formato fora do conjunto validado gera
     * {@link AuditoriaException} e nenhuma regra é executada.
     */
    private DocumentoLegenda lerLegenda(Path caminho, String formato) {
        return switch (formato) {
            case "SRT" -> leitorLegendaSrt.ler(caminho);
            case "ASS", "SSA" -> leitorLegendaAss.ler(caminho);
            default -> throw new AuditoriaException("Formato nao suportado: " + formato);
        };
    }
}
