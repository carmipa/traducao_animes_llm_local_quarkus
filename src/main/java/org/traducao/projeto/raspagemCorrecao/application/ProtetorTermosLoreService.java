package org.traducao.projeto.raspagemCorrecao.application;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: impede que a contingência Google traduza literalmente
 * nomes e terminologia que a lore manda manter na forma oficial.
 *
 * <p>INVARIANTES DO DOMÍNIO: termos maiores são mascarados antes dos menores;
 * a grafia encontrada no original é restaurada; marcadores nunca podem sobrar.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: restauração incompleta devolve
 * {@code null}, fazendo o chamador manter a entrada pendente.
 */
@Service
public class ProtetorTermosLoreService {

    private static final Pattern LINHA_MANTER = Pattern.compile(
        "(?im)^\\s*-\\s*Manter sempre[^:]*:\\s*(.+)$");

    /**
     * PROPÓSITO DE NEGÓCIO: transporta o texto seguro para tradução e o mapa
     * necessário para recompor os termos oficiais depois da resposta externa.
     *
     * <p>INVARIANTES DO DOMÍNIO: marcadores são únicos por ocorrência textual.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: record imutável, sem efeitos colaterais.
     */
    public record TextoProtegido(String textoMascarado, Map<String, String> termosPorMarcador) {}

    /**
     * PROPÓSITO DE NEGÓCIO: mascara no texto os termos explícitos do provedor e
     * os declarados na regra “Manter sempre” da lore ativa.
     *
     * <p>INVARIANTES DO DOMÍNIO: comparação ignora caixa, respeita fronteiras
     * alfanuméricas e preserva exatamente a forma encontrada na fala.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto/lore vazios resultam no texto
     * original com mapa vazio.
     */
    public TextoProtegido mascarar(String texto, String lore, Set<String> termosExplicitos) {
        if (texto == null || texto.isBlank()) {
            return new TextoProtegido(texto, Map.of());
        }
        List<String> termos = extrairTermos(lore, termosExplicitos);
        String resultado = texto;
        Map<String, String> mapa = new LinkedHashMap<>();
        int sequencia = 0;
        for (String termo : termos) {
            Pattern ocorrencia = Pattern.compile(
                "(?iu)(?<![\\p{L}\\p{N}])" + Pattern.quote(termo) + "(?![\\p{L}\\p{N}])");
            Matcher matcher = ocorrencia.matcher(resultado);
            StringBuffer substituido = new StringBuffer();
            boolean encontrou = false;
            while (matcher.find()) {
                encontrou = true;
                String marcador = "ZXQLORE" + sequencia++ + "QXZ";
                mapa.put(marcador, matcher.group());
                matcher.appendReplacement(substituido, Matcher.quoteReplacement(marcador));
            }
            if (encontrou) {
                matcher.appendTail(substituido);
                resultado = substituido.toString();
            }
        }
        return new TextoProtegido(resultado, Map.copyOf(mapa));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: restaura os termos canônicos na tradução externa.
     *
     * <p>INVARIANTES DO DOMÍNIO: todos os marcadores precisam sobreviver; nenhum
     * marcador técnico pode aparecer na legenda final.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve {@code null} quando o Google
     * remove ou mutila qualquer marcador.
     */
    public String restaurar(String traduzido, TextoProtegido protegido) {
        if (traduzido == null) return null;
        String restaurado = traduzido;
        for (Map.Entry<String, String> item : protegido.termosPorMarcador().entrySet()) {
            if (!restaurado.contains(item.getKey())) {
                return null;
            }
            restaurado = restaurado.replace(item.getKey(), item.getValue());
        }
        return restaurado.matches("(?i).*ZXQLORE\\d+QXZ.*") ? null : restaurado;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: converte regras textuais da lore em glossário
     * operacional sem exigir listas hardcoded por anime.
     *
     * <p>INVARIANTES DO DOMÍNIO: remove duplicatas sem diferenciar caixa e ordena
     * por tamanho decrescente para evitar mascaramento parcial.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lore sem regra reconhecida usa somente
     * os termos explícitos fornecidos pelo contexto.
     */
    private List<String> extrairTermos(String lore, Set<String> termosExplicitos) {
        Map<String, String> unicos = new LinkedHashMap<>();
        if (termosExplicitos != null) {
            termosExplicitos.stream().filter(t -> t != null && !t.isBlank())
                .forEach(t -> unicos.putIfAbsent(t.toLowerCase(Locale.ROOT), t.strip()));
        }
        if (lore != null) {
            Matcher linhas = LINHA_MANTER.matcher(lore);
            while (linhas.find()) {
                for (String bruto : linhas.group(1).split(",")) {
                    String termo = bruto.strip().replaceFirst("[.;]$", "").strip();
                    if (!termo.isBlank() && termo.length() <= 80) {
                        unicos.putIfAbsent(termo.toLowerCase(Locale.ROOT), termo);
                    }
                }
            }
        }
        List<String> termos = new ArrayList<>(new LinkedHashSet<>(unicos.values()));
        termos.sort(Comparator.comparingInt(String::length).reversed());
        return termos;
    }
}
