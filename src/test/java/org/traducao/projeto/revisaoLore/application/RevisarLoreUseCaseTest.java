package org.traducao.projeto.revisaoLore.application;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda;
import org.traducao.projeto.traducao.domain.legenda.EventoLegenda;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RevisarLoreUseCaseTest {

    private static final String CABECALHO =
        "[Events]\n"
        + "Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n";

    private static EventoLegenda dialogo(int indice, String inicio, String fim, String texto) {
        String prefixo = "Dialogue: 0," + inicio + "," + fim + ",Default,,0,0,0,,";
        return new EventoLegenda(indice, "Dialogue", "Default", prefixo, texto);
    }

    private static EventoLegenda comentario(int indice, String inicio, String fim, String texto) {
        String prefixo = "Comment: 0," + inicio + "," + fim + ",Default,,0,0,0,,";
        return new EventoLegenda(indice, "Comment", "Default", prefixo, texto);
    }

    private static DocumentoLegenda doc(List<EventoLegenda> eventos) {
        return new DocumentoLegenda(CABECALHO, eventos, "\n", false);
    }

    @Test
    void paresAlinhadosNaoAcusamDivergencia() {
        DocumentoLegenda en = doc(List.of(
            dialogo(0, "0:00:01.00", "0:00:03.00", "Hello Shin."),
            dialogo(1, "0:00:04.00", "0:00:06.00", "The Legion is coming.")));
        DocumentoLegenda pt = doc(List.of(
            dialogo(0, "0:00:01.00", "0:00:03.00", "Ola Shin."),
            dialogo(1, "0:00:04.00", "0:00:06.00", "A Legiao esta chegando.")));

        assertTrue(RevisarLoreUseCase.primeiraDivergenciaEstrutural(en, pt, 500).isEmpty());
    }

    @Test
    void jitterDentroDaToleranciaNaoAcusaDivergencia() {
        DocumentoLegenda en = doc(List.of(dialogo(0, "0:00:01.00", "0:00:03.00", "Hello.")));
        DocumentoLegenda pt = doc(List.of(dialogo(0, "0:00:01.20", "0:00:03.10", "Ola.")));

        assertTrue(RevisarLoreUseCase.primeiraDivergenciaEstrutural(en, pt, 500).isEmpty());
    }

    @Test
    void temposReordenadosAcusamDivergencia() {
        DocumentoLegenda en = doc(List.of(
            dialogo(0, "0:00:01.00", "0:00:03.00", "First line."),
            dialogo(1, "0:00:04.00", "0:00:06.00", "Second line.")));
        // PT com a segunda fala deslocada em segundos (reordenacao/insercao a montante).
        DocumentoLegenda pt = doc(List.of(
            dialogo(0, "0:00:01.00", "0:00:03.00", "Primeira fala."),
            dialogo(1, "0:00:10.00", "0:00:12.00", "Segunda fala.")));

        Optional<String> r = RevisarLoreUseCase.primeiraDivergenciaEstrutural(en, pt, 500);
        assertTrue(r.isPresent());
        assertTrue(r.get().contains("tempos divergentes"));
        assertTrue(r.get().contains("evento 2"));
    }

    @Test
    void tipoDivergenteAcusaDivergencia() {
        DocumentoLegenda en = doc(List.of(dialogo(0, "0:00:01.00", "0:00:03.00", "Line.")));
        DocumentoLegenda pt = doc(List.of(comentario(0, "0:00:01.00", "0:00:03.00", "Linha.")));

        Optional<String> r = RevisarLoreUseCase.primeiraDivergenciaEstrutural(en, pt, 500);
        assertTrue(r.isPresent());
        assertTrue(r.get().contains("tipo divergente"));
    }

    @Test
    void estiloDiferenteNaoAcusaDivergencia() {
        DocumentoLegenda en = doc(List.of(dialogo(0, "0:00:01.00", "0:00:03.00", "Line.")));
        // PT restilizada (Style diferente), mesmo tempo/tipo: legitimo, nao bloqueia.
        EventoLegenda ptEvt = new EventoLegenda(
            0, "Dialogue", "Italico", "Dialogue: 0,0:00:01.00,0:00:03.00,Italico,,0,0,0,,", "Linha.");
        DocumentoLegenda pt = doc(List.of(ptEvt));

        assertFalse(RevisarLoreUseCase.primeiraDivergenciaEstrutural(en, pt, 500).isPresent());
    }

    @Test
    void corrigeShinTraduzidoComoCanelaSemLlm() {
        var corrigida = RevisarLoreUseCase.corrigirLoreDeterministica(
            "Shin! Shinei Nouzen!",
            "Canela! Shinei Nouzen!"
        );

        assertTrue(corrigida.isPresent());
        assertEquals("Shin! Shinei Nouzen!", corrigida.get());
    }

    @Test
    void naoTrocaCanelaQuandoOriginalNaoTemShin() {
        var corrigida = RevisarLoreUseCase.corrigirLoreDeterministica(
            "My leg hurts.",
            "Minha canela dói."
        );

        assertTrue(corrigida.isEmpty());
    }

    @Test
    void corrigeDudRoundsTraduzidoLiteralmente() {
        var corrigida = RevisarLoreUseCase.corrigirLoreDeterministica(
            "Those are dud rounds.",
            "Essas são rodadas aleatórias."
        );
        var cache = RevisarLoreUseCase.corrigirLoreDeterministica(
            "Those dud rounds landed around there.",
            "Aquelas rodadas fracassadas caíram ali perto."
        );

        assertTrue(corrigida.isPresent());
        assertEquals("Essas são munições falhas.", corrigida.get());
        assertTrue(cache.isPresent());
        assertEquals("Aquelas munições falhas caíram ali perto.", cache.get());
    }
}
