package org.traducao.projeto.auditorConteudoLegendas.application.regras.arquivounico;

import jakarta.enterprise.context.ApplicationScoped;
import org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.domain.RegraAuditoriaArquivoUnico;
import org.traducao.projeto.auditorConteudoLegendas.domain.TempoEventoUtil;
import org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda;
import org.traducao.projeto.traducao.domain.legenda.EventoLegenda;

import java.util.ArrayList;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: sinaliza eventos cujo instante de fim é anterior ou igual
 * ao de início. Uma linha com duração zero ou negativa não aparece na tela e
 * costuma indicar corrupção de timestamps na legenda.
 *
 * <p>INVARIANTES DO DOMÍNIO: só eventos {@code Dialogue} com tempo legível são
 * avaliados; a comparação usa milissegundos absolutos.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: evento sem tempo interpretável é ignorado
 * (a regra {@link RegraTagOverrideNaoFechada} e as demais cobrem outros danos).
 */
@ApplicationScoped
public class RegraTimestampInvalido implements RegraAuditoriaArquivoUnico {

    @Override
    public String getNome() {
        return "Timestamp Inválido (fim ≤ início)";
    }

    @Override
    public List<AnomaliaConteudo> auditar(DocumentoLegenda documento) {
        List<AnomaliaConteudo> anomalias = new ArrayList<>();
        for (EventoLegenda evento : documento.eventos()) {
            if (!evento.isDialogo()) {
                continue;
            }
            long[] tempo = TempoEventoUtil.extrairInicioFimMs(evento);
            if (tempo == null) {
                continue;
            }
            if (tempo[1] <= tempo[0]) {
                anomalias.add(new AnomaliaConteudo(
                    AnomaliaConteudo.TipoSeveridade.ERROR,
                    getNome(),
                    "Fim (" + tempo[1] + " ms) menor ou igual ao início (" + tempo[0] + " ms). A linha tem duração inválida e não será exibida.",
                    evento,
                    null,
                    "Corrigir o timestamp de fim para depois do início."
                ));
            }
        }
        return anomalias;
    }
}
