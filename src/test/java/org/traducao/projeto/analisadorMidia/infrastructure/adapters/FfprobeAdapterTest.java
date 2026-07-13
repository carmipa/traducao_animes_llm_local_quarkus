package org.traducao.projeto.analisadorMidia.infrastructure.adapters;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.analisadorMidia.domain.AnalisadorException;
import org.traducao.projeto.analisadorMidia.domain.AudioInfo;
import org.traducao.projeto.analisadorMidia.domain.AuditoriaResultado;
import org.traducao.projeto.analisadorMidia.domain.LegendaInfo;
import org.traducao.projeto.analisadorMidia.domain.VideoInfo;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cobre o parsing ffprobe-JSON → domínio sem executar ffprobe real: substitui o
 * seam de processo externo ({@code executarFfprobeJson}) por JSON canônico e
 * verifica container, faixas de vídeo/áudio/legenda e casos-limite.
 */
class FfprobeAdapterTest {

    private static FfprobeAdapter comJson(String json) {
        return new FfprobeAdapter() {
            @Override
            protected String executarFfprobeJson(Path caminhoVideo) {
                return json;
            }
        };
    }

    @Test
    void parseiaContainerVideoAudioELegendaAss() {
        String json = """
            {"streams":[
              {"index":0,"codec_type":"video","codec_name":"hevc","codec_long_name":"H.265 / HEVC",
               "width":1920,"height":1080,"pix_fmt":"yuv420p10le","r_frame_rate":"24000/1001",
               "display_aspect_ratio":"16:9","bit_rate":"5000000"},
              {"index":1,"codec_type":"audio","codec_name":"flac","channels":2,"sample_rate":"48000",
               "bit_rate":"900000","tags":{"language":"jpn","title":"Japanese"}},
              {"index":2,"codec_type":"subtitle","codec_name":"ass","tags":{"language":"eng","title":"English"}}
            ],
            "format":{"format_name":"matroska,webm","size":"734003200","duration":"1440.5",
                      "bit_rate":"4000000","tags":{"encoder":"libebml"}}}
            """;

        AuditoriaResultado r = comJson(json).analisarMidia(Path.of("Exemplo.mkv"));

        assertEquals("Exemplo.mkv", r.nomeArquivo());
        assertEquals("matroska,webm", r.container().formato());
        assertEquals(734003200L, r.container().tamanhoBytes());
        assertEquals(1440.5, r.container().duracaoSegundos(), 0.001);

        assertEquals(1, r.videos().size());
        VideoInfo v = r.videos().get(0);
        assertEquals("HEVC", v.codecId());
        assertEquals(1920, v.width());
        assertEquals(1080, v.height());
        assertEquals(10, v.bitDepth());
        assertEquals(23.976, v.fps(), 0.001);

        assertEquals(1, r.audios().size());
        AudioInfo a = r.audios().get(0);
        assertEquals("jpn", a.idioma());
        assertEquals("FLAC", a.format());
        assertEquals(2, a.channels());
        assertEquals(48.0, a.sampleRateKHz(), 0.001);

        assertEquals(1, r.legendas().size());
        LegendaInfo leg = r.legendas().get(0);
        assertEquals("eng", leg.idioma());
        assertEquals("ASS", leg.formato());
        assertEquals("ass", leg.codecId());
    }

    @Test
    void semLegendaResultaEmListaVaziaE8BitsEmPixFmt() {
        String json = """
            {"streams":[
              {"index":0,"codec_type":"video","codec_name":"h264","width":1280,"height":720,
               "pix_fmt":"yuv420p","r_frame_rate":"24/1"}
            ],
            "format":{"format_name":"mov,mp4","size":"100","duration":"60"}}
            """;

        AuditoriaResultado r = comJson(json).analisarMidia(Path.of("raw.mp4"));

        assertEquals(0, r.legendas().size());
        assertEquals(1, r.videos().size());
        assertEquals(8, r.videos().get(0).bitDepth());
    }

    @Test
    void legendaPgsSemIdiomaCaiEmDesconhecido() {
        String json = """
            {"streams":[
              {"index":0,"codec_type":"subtitle","codec_name":"hdmv_pgs_subtitle"}
            ],
            "format":{"format_name":"matroska","size":"1","duration":"1"}}
            """;

        AuditoriaResultado r = comJson(json).analisarMidia(Path.of("bd.mkv"));

        assertEquals(1, r.legendas().size());
        LegendaInfo leg = r.legendas().get(0);
        assertEquals("HDMV_PGS_SUBTITLE", leg.formato());
        assertEquals("Desconhecido", leg.idioma());
    }

    @Test
    void leCapitulosAnexosEFlagsDeDisposition() {
        String json = """
            {"streams":[
              {"index":0,"codec_type":"video","codec_name":"h264","width":1280,"height":720,
               "pix_fmt":"yuv420p","r_frame_rate":"24/1"},
              {"index":1,"codec_type":"subtitle","codec_name":"ass","tags":{"language":"eng"},
               "disposition":{"default":1,"forced":1,"hearing_impaired":1}},
              {"index":2,"codec_type":"attachment","codec_name":"ttf","extradata_size":12345,
               "tags":{"filename":"font.ttf","mimetype":"application/x-truetype-font"}}
            ],
            "chapters":[
              {"start_time":"0.000","end_time":"60.000","tags":{"title":"Abertura"}},
              {"start_time":"60.000","end_time":"120.000","tags":{"title":"Parte A"}}
            ],
            "format":{"format_name":"matroska","size":"1","duration":"120"}}
            """;

        AuditoriaResultado r = comJson(json).analisarMidia(Path.of("ep.mkv"));

        assertEquals(1, r.legendas().size());
        LegendaInfo leg = r.legendas().get(0);
        assertTrue(leg.isDefault());
        assertTrue(leg.isForced());
        assertTrue(leg.acessibilidade());

        assertEquals(2, r.capitulos().size());
        assertEquals(1, r.capitulos().get(0).numero());
        assertEquals("Abertura", r.capitulos().get(0).titulo());

        assertEquals(1, r.anexos().size());
        assertEquals("font.ttf", r.anexos().get(0).nomeArquivo());
        assertEquals("application/x-truetype-font", r.anexos().get(0).mimeType());
    }

    @Test
    void falhaDoFfprobeViraAnalisadorException() {
        FfprobeAdapter adapter = new FfprobeAdapter() {
            @Override
            protected String executarFfprobeJson(Path caminhoVideo) {
                throw new AnalisadorException("ffprobe falhou com código 1");
            }
        };

        assertThrows(AnalisadorException.class, () -> adapter.analisarMidia(Path.of("x.mkv")));
    }
}
