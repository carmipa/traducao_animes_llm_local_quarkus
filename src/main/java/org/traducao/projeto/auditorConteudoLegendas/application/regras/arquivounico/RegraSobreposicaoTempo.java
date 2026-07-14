package org.traducao.projeto.auditorConteudoLegendas.application.regras.arquivounico;

import jakarta.enterprise.context.ApplicationScoped;
import org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.domain.RegraAuditoriaArquivoUnico;
import org.traducao.projeto.auditorConteudoLegendas.domain.TempoEventoUtil;
import org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda;
import org.traducao.projeto.traducao.domain.legenda.EventoLegenda;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: detecta diálogos que se sobrepõem no tempo — uma fala que
 * começa antes de a anterior terminar. Legendas de diálogo sobrepostas piscam ou
 * se empilham na tela, dano perceptível que independe de arquivo de referência.
 *
 * <p>INVARIANTES DO DOMÍNIO: só eventos {@code Dialogue} com tempo legível entram;
 * a verificação é feita sobre a lista ordenada por início, comparando cada evento
 * com o de maior fim já visto (cobre eventos fora de ordem no arquivo).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: eventos sem tempo interpretável são
 * ignorados; documento com menos de dois diálogos legíveis não gera anomalia.
 */
@ApplicationScoped
public class RegraSobreposicaoTempo implements RegraAuditoriaArquivoUnico {

    private record EventoTempo(EventoLegenda evento, long inicio, long fim) {}

    @Override
    public String getNome() {
        return "Sobreposição de Tempo Entre Diálogos";
    }

    @Override
    public List<AnomaliaConteudo> auditar(DocumentoLegenda documento) {
        List<EventoTempo> temporizados = new ArrayList<>();
        for (EventoLegenda evento : documento.eventos()) {
            if (!evento.isDialogo()) {
                continue;
            }
            long[] tempo = TempoEventoUtil.extrairInicioFimMs(evento);
            if (tempo == null || tempo[1] <= tempo[0]) {
                continue; // duração inválida é tratada pela RegraTimestampInvalido
            }
            temporizados.add(new EventoTempo(evento, tempo[0], tempo[1]));
        }
        temporizados.sort(Comparator.comparingLong(EventoTempo::inicio));

        List<AnomaliaConteudo> anomalias = new ArrayList<>();
        long maiorFimAnterior = Long.MIN_VALUE;
        EventoTempo anterior = null;
        for (EventoTempo atual : temporizados) {
            if (anterior != null && atual.inicio() < maiorFimAnterior) {
                anomalias.add(new AnomaliaConteudo(
                    AnomaliaConteudo.TipoSeveridade.WARNING,
                    getNome(),
                    "Este diálogo começa em " + atual.inicio() + " ms, antes de a fala anterior terminar em "
                        + maiorFimAnterior + " ms. As linhas se sobrepõem na tela.",
                    atual.evento(),
                    null,
                    "Ajustar início/fim para que as falas de diálogo não se sobreponham."
                ));
            }
            if (atual.fim() > maiorFimAnterior) {
                maiorFimAnterior = atual.fim();
            }
            anterior = atual;
        }
        return anomalias;
    }
}
