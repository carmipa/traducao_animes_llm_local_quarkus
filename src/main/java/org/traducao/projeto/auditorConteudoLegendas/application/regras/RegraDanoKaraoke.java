package org.traducao.projeto.auditorConteudoLegendas.application.regras;

import jakarta.enterprise.context.ApplicationScoped;
import org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.domain.RegraAuditoriaConteudo;
import org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda;
import org.traducao.projeto.traducao.domain.legenda.EventoLegenda;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Detecta dano de tradução em karaokê/música comparando cada evento traduzido
 * com o original. Usa o {@link DetectorEfeitoKaraokeService} como fonte única
 * de verdade — a mesma régua da tradução e da Correção de Karaokê, para a
 * auditoria acusar exatamente o que aquelas etapas deveriam ter protegido:
 * <ul>
 *   <li>CRITICAL: karaokê japonês/romaji alterado (caso real do 86 T1, onde
 *       romaji com tags leves virava alucinação do LLM);</li>
 *   <li>WARNING: música traduzível com expansão anormal de texto;</li>
 *   <li>WARNING: tags de timing de karaokê ({@code \k}) perdidas.</li>
 * </ul>
 */
@ApplicationScoped
public class RegraDanoKaraoke implements RegraAuditoriaConteudo {

    private final DetectorEfeitoKaraokeService detectorKaraoke;

    public RegraDanoKaraoke(DetectorEfeitoKaraokeService detectorKaraoke) {
        this.detectorKaraoke = detectorKaraoke;
    }

    @Override
    public String getNome() {
        return "Dano Estrutural em Karaokê/Música";
    }

    @Override
    public List<AnomaliaConteudo> auditar(DocumentoLegenda original, DocumentoLegenda traduzido) {
        List<AnomaliaConteudo> anomalias = new ArrayList<>();
        Map<Integer, EventoLegenda> mapOriginal = original.eventos().stream()
                .collect(Collectors.toMap(EventoLegenda::indice, Function.identity()));

        for (EventoLegenda eventoTrad : traduzido.eventos()) {
            if (!eventoTrad.isDialogo() || !eventoTrad.temTexto()) continue;

            EventoLegenda eventoOrig = mapOriginal.get(eventoTrad.indice());
            if (eventoOrig == null || !eventoOrig.temTexto()) continue;

            String textoOrig = eventoOrig.texto();
            boolean protegido = detectorKaraoke.devePreservarKaraokeOriginal(eventoOrig.estilo(), textoOrig);
            boolean musicaTraduzivel = detectorKaraoke.eKaraokeOuMusicaTraduzivel(eventoOrig.estilo(), textoOrig);
            if (!protegido && !musicaTraduzivel) continue;

            String visivelOriginal = extrairTextoVisivelAss(textoOrig);
            String visivelTraduzido = extrairTextoVisivelAss(eventoTrad.texto());

            if (protegido && !visivelOriginal.equals(visivelTraduzido)) {
                anomalias.add(new AnomaliaConteudo(
                        AnomaliaConteudo.TipoSeveridade.CRITICAL,
                        getNome(),
                        "Karaokê japonês/romaji foi alterado na tradução — deveria permanecer intacto.",
                        eventoOrig,
                        eventoTrad,
                        "Rode a Correção de Karaokê: ela restaura a linha original automaticamente."
                ));
                continue;
            }

            if (musicaTraduzivel && visivelOriginal.length() > 5
                    && visivelTraduzido.length() > visivelOriginal.length() * 2.5) {
                anomalias.add(new AnomaliaConteudo(
                        AnomaliaConteudo.TipoSeveridade.WARNING,
                        getNome(),
                        "O texto da música sofreu expansão anormal na tradução (possível alucinação do LLM).",
                        eventoOrig,
                        eventoTrad,
                        "Revise a linha; se for alucinação, rode a Correção de Karaokê."
                ));
            }

            if (detectorKaraoke.temTagKaraoke(textoOrig) && !detectorKaraoke.temTagKaraoke(eventoTrad.texto())) {
                anomalias.add(new AnomaliaConteudo(
                        AnomaliaConteudo.TipoSeveridade.WARNING,
                        getNome(),
                        "As tags de timing de karaokê (\\k) sumiram na tradução.",
                        eventoOrig,
                        eventoTrad,
                        "Restaure as tags originais (a Correção de Karaokê recupera o prefixo de tags)."
                ));
            }
        }
        return anomalias;
    }

    private String extrairTextoVisivelAss(String texto) {
        if (texto == null) return "";
        return texto.replaceAll("\\{[^}]+\\}", "")
                    .replace("\\N", " ")
                    .replace("\\n", " ")
                    .replace("\\h", " ")
                    .strip();
    }
}
