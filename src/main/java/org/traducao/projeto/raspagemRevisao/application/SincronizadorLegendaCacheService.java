package org.traducao.projeto.raspagemRevisao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda;
import org.traducao.projeto.traducao.domain.legenda.EventoLegenda;
import org.traducao.projeto.traducao.infrastructure.cache.EntradaCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: materializa no ASS/SSA as correções confirmadas pela
 * Opção 5 antes de a Opção 6 iniciar sua auditoria linguística.
 *
 * <p>INVARIANTES DO DOMÍNIO: sincroniza somente por índice existente, somente
 * tradução não vazia e nunca modifica cabeçalho, tempos, estilos ou linhas não
 * dialogadas. Uma fala que regrediu exatamente ao original EN pode ser
 * recuperada mesmo quando o timestamp do cache é anterior ao ASS.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: cache vazio devolve o documento original;
 * sem autorização temporal, somente regressões exatas ao original são reparadas.
 */
@Service
public class SincronizadorLegendaCacheService {

    /**
     * PROPÓSITO DE NEGÓCIO: transporta o documento sincronizado e os índices
     * alterados para log, métricas e decisão de persistência.
     * <p>INVARIANTES DO DOMÍNIO: índices são imutáveis e não contêm duplicatas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: record não executa efeitos colaterais.
     */
    public record Resultado(
        DocumentoLegenda documento,
        List<Integer> indicesSincronizados,
        List<Integer> indicesRecuperadosDoOriginal
    ) {
        /**
         * PROPÓSITO DE NEGÓCIO: informa o total materializado pela ponte 5→6.
         * <p>INVARIANTES DO DOMÍNIO: equivale ao tamanho da lista de índices.
         * <p>COMPORTAMENTO EM CASO DE FALHA: lista nula é proibida pela fábrica do serviço.
         */
        public int total() { return indicesSincronizados.size(); }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aplica ao documento as traduções mais recentes do
     * cache quando a comparação temporal autorizou a ponte entre módulos ou
     * quando o ASS regrediu exatamente ao original inglês registrado no cache.
     *
     * <p>INVARIANTES DO DOMÍNIO: valor vazio nunca apaga fala; valor idêntico não
     * conta como mudança; primeira entrada de um índice é a autoridade; cache
     * antigo só vence uma fala já traduzida quando ela voltou ao original EN.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: argumentos ausentes devolvem o
     * documento intacto; autorização temporal falsa restringe a operação à
     * recuperação comprovável de regressões ao original.
     */
    public Resultado sincronizar(
        DocumentoLegenda documento,
        List<EntradaCache> entradas,
        boolean autorizado
    ) {
        if (documento == null || entradas == null || entradas.isEmpty()) {
            return new Resultado(documento, List.of(), List.of());
        }
        Map<Integer, EntradaCache> porIndice = new HashMap<>();
        for (EntradaCache entrada : entradas) porIndice.putIfAbsent(entrada.indice(), entrada);

        List<EventoLegenda> atualizados = new ArrayList<>(documento.eventos().size());
        List<Integer> indices = new ArrayList<>();
        List<Integer> recuperadosDoOriginal = new ArrayList<>();
        for (EventoLegenda evento : documento.eventos()) {
            EntradaCache entrada = porIndice.get(evento.indice());
            boolean regrediuAoOriginal = entrada != null && entrada.original() != null
                && entrada.original().equals(evento.texto());
            boolean podeAplicar = autorizado || regrediuAoOriginal;
            if (podeAplicar && evento.isDialogo() && entrada != null && entrada.traduzido() != null
                && !entrada.traduzido().isBlank() && !entrada.traduzido().equals(evento.texto())) {
                atualizados.add(evento.comTexto(entrada.traduzido()));
                indices.add(evento.indice());
                if (regrediuAoOriginal) recuperadosDoOriginal.add(evento.indice());
            } else {
                atualizados.add(evento);
            }
        }
        if (indices.isEmpty()) return new Resultado(documento, List.of(), List.of());
        return new Resultado(new DocumentoLegenda(
            documento.cabecalho(), atualizados, documento.quebraDeLinha(), documento.comBom()),
            List.copyOf(indices), List.copyOf(recuperadosDoOriginal));
    }
}
