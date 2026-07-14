package org.traducao.projeto.auditorConteudoLegendas.domain;

import org.traducao.projeto.traducao.domain.legenda.EventoLegenda;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: extrai os instantes de início e fim de um evento de
 * legenda para as regras de arquivo único que dependem do tempo (timestamp
 * inválido e sobreposição), unificando os dois formatos suportados.
 *
 * <p>INVARIANTES DO DOMÍNIO: o tempo é lido do campo {@code prefixo} preservado
 * pelos leitores — ASS guarda {@code Dialogue: Layer,Início,Fim,...} e SRT
 * guarda a linha {@code hh:mm:ss,mmm --> hh:mm:ss,mmm}. Todos os valores são
 * normalizados para milissegundos absolutos desde 0.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer prefixo ilegível ou incompleto
 * resulta em {@code null} (não lança), sinalizando à regra que aquele evento não
 * pode ser avaliado no eixo do tempo.
 */
public final class TempoEventoUtil {

    // hh:mm:ss seguido de separador decimal (',' no SRT, '.' no ASS) + frações.
    private static final Pattern TEMPO = Pattern.compile("(\\d{1,2}):(\\d{2}):(\\d{2})[.,](\\d{1,3})");

    private TempoEventoUtil() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve {início, fim} em ms do evento informado.
     * <p>INVARIANTES DO DOMÍNIO: SRT é detectado pela seta {@code -->}; caso
     * contrário assume-se ASS e são usados o 2º e o 3º campos do prefixo.
     * <p>COMPORTAMENTO EM CASO DE FALHA: retorna {@code null} quando falta o
     * prefixo, o formato não bate ou algum instante não pôde ser lido.
     */
    public static long[] extrairInicioFimMs(EventoLegenda evento) {
        if (evento == null || evento.prefixo() == null) {
            return null;
        }
        String prefixo = evento.prefixo();

        if (prefixo.contains("-->")) {
            String[] lados = prefixo.split("-->", 2);
            Long inicio = paraMs(lados[0]);
            Long fim = paraMs(lados.length > 1 ? lados[1] : "");
            return (inicio == null || fim == null) ? null : new long[]{inicio, fim};
        }

        // ASS/SSA: "Dialogue: Layer,Início,Fim,Style,...," — o 2º e o 3º campos são o tempo.
        int idxColon = prefixo.indexOf(':');
        String corpo = idxColon >= 0 ? prefixo.substring(idxColon + 1) : prefixo;
        String[] campos = corpo.split(",");
        if (campos.length < 3) {
            return null;
        }
        Long inicio = paraMs(campos[1]);
        Long fim = paraMs(campos[2]);
        return (inicio == null || fim == null) ? null : new long[]{inicio, fim};
    }

    /**
     * PROPÓSITO DE NEGÓCIO: converte o primeiro carimbo hh:mm:ss(.|,)frac de um
     * trecho em milissegundos.
     * <p>INVARIANTES DO DOMÍNIO: as frações são interpretadas em centésimos (ASS,
     * 2 dígitos) ou milésimos (SRT, 3 dígitos) conforme a quantidade de dígitos.
     * <p>COMPORTAMENTO EM CASO DE FALHA: trecho sem carimbo válido devolve
     * {@code null}.
     */
    private static Long paraMs(String trecho) {
        if (trecho == null) {
            return null;
        }
        Matcher m = TEMPO.matcher(trecho);
        if (!m.find()) {
            return null;
        }
        long horas = Long.parseLong(m.group(1));
        long minutos = Long.parseLong(m.group(2));
        long segundos = Long.parseLong(m.group(3));
        String fracao = m.group(4);
        long fracaoMs = fracao.length() == 2
            ? Long.parseLong(fracao) * 10L      // centésimos (ASS)
            : Long.parseLong(fracao.length() == 1 ? fracao + "00" : fracao); // milésimos (SRT)
        return ((horas * 60 + minutos) * 60 + segundos) * 1000L + fracaoMs;
    }
}
