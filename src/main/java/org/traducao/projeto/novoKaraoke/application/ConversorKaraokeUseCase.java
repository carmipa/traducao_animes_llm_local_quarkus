package org.traducao.projeto.novoKaraoke.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traducao.projeto.novoKaraoke.domain.EventoAss;
import org.traducao.projeto.novoKaraoke.domain.LinhaSimplesKaraoke;
import org.traducao.projeto.novoKaraoke.domain.NovoKaraokeException;
import org.traducao.projeto.novoKaraoke.domain.ResultadoConversaoKaraoke;
import org.traducao.projeto.novoKaraoke.infrastructure.NovoKaraokePersistencia;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.traducao.presentation.web.LogStreamService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Converte legendas .ass com karaokê KFX (milhares de eventos por sílaba/frame)
 * em legendas simples: uma linha limpa por frase da música, no MESMO tempo do
 * efeito original (início = menor início do bloco, fim = maior fim).
 * <p>
 * Garantias de segurança:
 * <ul>
 *   <li>O arquivo original NUNCA é alterado — a saída vai para a pasta que o
 *       usuário escolher.</li>
 *   <li>Diálogo, placas e Comment são reemitidos byte a byte (linha crua).</li>
 *   <li>Bloco musical que não puder ser reconstruído com confiança é mantido
 *       intacto (viés de preservação, mesmo princípio do
 *       {@link DetectorEfeitoKaraokeService}).</li>
 * </ul>
 */
@ApplicationScoped
public class ConversorKaraokeUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConversorKaraokeUseCase.class);

    public static final String CANAL_LOG = "novo-karaoke";
    static final String NOME_ESTILO_SIMPLES = "Karaoke Simples";

    /** Gap acima deste valor separa duas ocorrências da mesma frase (refrão repetido). */
    private static final long GAP_MESMA_FRASE_CS = 1000; // 10s
    /** Gap usado quando só há fragmentos silábicos/frame-a-frame, sem linha inteira. */
    private static final long GAP_FRAGMENTOS_KFX_CS = 200; // 2s
    private static final long GAP_REPETICAO_FRAGMENTO_CS = 50;
    /** Margem pequena só para cobrir arredondamento/frame na remoção dos eventos KFX. */
    private static final long MARGEM_COBERTURA_CS = 25;
    /** Janelas com sobreposição >= 60% e texto parecido são variantes divergentes da mesma frase. */
    private static final double SOBREPOSICAO_MINIMA_VARIANTE = 0.6;
    private static final double SIMILARIDADE_MINIMA_TOKENS = 0.3;

    private static final Pattern PADRAO_PLAY_RES_Y = Pattern.compile("(?i)^PlayResY\\s*:\\s*(\\d+)");
    private static final Pattern PADRAO_ESTILO = Pattern.compile("(?i)^Style\\s*:");
    private static final Pattern PADRAO_SECAO = Pattern.compile("^\\s*\\[[^\\]]+\\]");
    private static final Pattern PADRAO_ARQUIVO_AUXILIAR = Pattern.compile(
        "(?i).*(?:NCOP\\d*|NCED\\d*|Special Edition|[_\\s-]SP[_\\s-]).*");
    private static final Pattern PADRAO_ARTEFATO_TAG_VISIVEL = Pattern.compile("(?i)!?\\[?TAG\\d+\\]?");
    private static final Pattern PADRAO_JAPONES = Pattern.compile("[\\p{IsHiragana}\\p{IsKatakana}\\p{IsHan}]");
    private static final Pattern PADRAO_COORDENADA_PRINCIPAL = Pattern.compile(
        "\\\\(?:pos|move)\\((-?\\d+(?:\\.\\d+)?),(-?\\d+(?:\\.\\d+)?)");
    private static final Pattern PALAVRA_ROMAJI_PATTERN = Pattern.compile(
        "^(?:n|(?:([kgsztdnhbpmyrwfjv])\\1?|sh|ch|ts|ky|gy|ny|hy|my|ry|by|py)?[aeiou])+$");
    private static final Pattern NAO_ASCII_PATTERN = Pattern.compile("[^\\x00-\\x7F]");

    @Inject
    DetectorEfeitoKaraokeService detectorKaraoke;

    @Inject
    LogStreamService logStream;

    @Inject
    TelemetriaService telemetriaService;

    @Inject
    NovoKaraokePersistencia persistencia;

    public List<ResultadoConversaoKaraoke> simular(Path pastaOrigem, Path pastaDestino) {
        return executar(pastaOrigem, pastaDestino, false);
    }

    public List<ResultadoConversaoKaraoke> aplicar(Path pastaOrigem, Path pastaDestino) {
        return executar(pastaOrigem, pastaDestino, true);
    }

    private List<ResultadoConversaoKaraoke> executar(Path pastaOrigem, Path pastaDestino, boolean gravar) {
        long inicioMs = System.currentTimeMillis();
        String modo = gravar ? "Aplicação" : "Simulação (Dry-Run)";
        logStream.publicarLog(CANAL_LOG, "==== Karaokê Simples — " + modo + " ====");
        logStream.publicarLog(CANAL_LOG, "Origem: " + pastaOrigem);
        logStream.publicarLog(CANAL_LOG, "Destino: " + pastaDestino);

        validarPastas(pastaOrigem, pastaDestino, gravar);

        List<Path> arquivos = listarLegendas(pastaOrigem);
        if (arquivos.isEmpty()) {
            logStream.publicarLog(CANAL_LOG, "Nenhum arquivo .ass encontrado na pasta de origem.");
            return List.of();
        }
        logStream.publicarLog(CANAL_LOG, "Arquivos .ass encontrados: " + arquivos.size());

        List<ResultadoConversaoKaraoke> resultados = new ArrayList<>();
        int falhas = 0;
        for (Path arquivo : arquivos) {
            if (Thread.currentThread().isInterrupted()) {
                logStream.publicarLog(CANAL_LOG, "[INTERROMPIDO] Conversão cancelada; arquivos já gravados foram preservados.");
                break;
            }
            try {
                resultados.add(converterArquivo(arquivo, pastaDestino, gravar));
            } catch (Exception e) {
                falhas++;
                log.error("Falha ao converter {}", arquivo, e);
                logStream.publicarLog(CANAL_LOG, "[ERRO] " + arquivo.getFileName() + ": " + e.getMessage());
            }
        }

        long duracaoMs = System.currentTimeMillis() - inicioMs;
        publicarResumo(resultados, falhas, duracaoMs, gravar);

        if (gravar && !resultados.isEmpty()) {
            registrarTelemetriaEManifesto(pastaOrigem, pastaDestino, resultados, duracaoMs);
        }
        return resultados;
    }

    private void validarPastas(Path pastaOrigem, Path pastaDestino, boolean gravar) {
        if (!Files.isDirectory(pastaOrigem)) {
            throw new NovoKaraokeException("A pasta de origem não existe ou não é um diretório: " + pastaOrigem);
        }
        if (pastaDestino == null) {
            throw new NovoKaraokeException("Informe a pasta de destino para as legendas convertidas.");
        }
        Path origemNorm = pastaOrigem.toAbsolutePath().normalize();
        Path destinoNorm = pastaDestino.toAbsolutePath().normalize();
        if (origemNorm.equals(destinoNorm)) {
            throw new NovoKaraokeException(
                "A pasta de destino deve ser DIFERENTE da origem: os arquivos originais são preservados para auditoria.");
        }
        if (gravar) {
            try {
                Files.createDirectories(destinoNorm);
            } catch (IOException e) {
                throw new NovoKaraokeException("Não foi possível criar a pasta de destino: " + destinoNorm, e);
            }
        }
    }

    private List<Path> listarLegendas(Path pasta) {
        try (Stream<Path> stream = Files.list(pasta)) {
            List<Path> arquivos = stream.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ass"))
                .sorted()
                .toList();
            List<Path> principais = arquivos.stream()
                .filter(p -> !ehArquivoAuxiliar(p))
                .toList();
            if (!principais.isEmpty() && principais.size() < arquivos.size()) {
                int ignorados = arquivos.size() - principais.size();
                logStream.publicarLog(CANAL_LOG,
                    "Arquivos auxiliares NCOP/NCED/SP ignorados nesta execução: " + ignorados);
                return principais;
            }
            return arquivos;
        } catch (IOException e) {
            throw new NovoKaraokeException("Falha ao listar a pasta de origem: " + pasta, e);
        }
    }

    private static boolean ehArquivoAuxiliar(Path arquivo) {
        return PADRAO_ARQUIVO_AUXILIAR.matcher(arquivo.getFileName().toString()).matches();
    }

    ResultadoConversaoKaraoke converterArquivo(Path arquivo, Path pastaDestino, boolean gravar) throws IOException {
        String nome = arquivo.getFileName().toString();
        logStream.publicarLog(CANAL_LOG, "");
        logStream.publicarLog(CANAL_LOG, ">> Processando: " + nome);

        byte[] bytesOriginais = Files.readAllBytes(arquivo);
        boolean temBom = bytesOriginais.length >= 3
            && (bytesOriginais[0] & 0xFF) == 0xEF && (bytesOriginais[1] & 0xFF) == 0xBB && (bytesOriginais[2] & 0xFF) == 0xBF;
        String conteudo = new String(bytesOriginais, temBom ? 3 : 0, bytesOriginais.length - (temBom ? 3 : 0), StandardCharsets.UTF_8);
        boolean crlf = conteudo.contains("\r\n");
        List<String> linhas = conteudo.lines().toList();

        ResultadoConversaoKaraoke resultado = new ResultadoConversaoKaraoke(nome);
        resultado.setTamanhoOriginalBytes(bytesOriginais.length);

        // --- separa cabeçalho (tudo que não é Dialogue) e eventos ---
        List<String> cabecalho = new ArrayList<>();
        List<EventoAss> eventos = new ArrayList<>();
        int playResY = 0;
        int idxUltimoEstilo = -1;
        for (String linha : linhas) {
            EventoAss evento = EventoAss.interpretar(linha);
            if (evento != null) {
                eventos.add(evento);
                continue;
            }
            Matcher mRes = PADRAO_PLAY_RES_Y.matcher(linha.strip());
            if (mRes.find()) {
                playResY = Integer.parseInt(mRes.group(1));
            }
            if (PADRAO_ESTILO.matcher(linha.stripLeading()).find()) {
                idxUltimoEstilo = cabecalho.size();
            }
            cabecalho.add(linha);
        }
        resultado.setEventosTotais(eventos.size());

        // --- classifica eventos: musical (KFX/estilo de música) vs diálogo ---
        // Musical = nome do estilo indica música OU tag \k crua. NÃO usar a
        // assinatura de template (\t + alta densidade de tags): placas/letreiros
        // animados têm a mesma assinatura e DEVEM sair byte-idênticos — é o
        // mesmo alerta documentado no DetectorEfeitoKaraokeService.
        List<EventoAss> musicais = new ArrayList<>();
        List<EventoAss> dialogo = new ArrayList<>();
        for (EventoAss evento : eventos) {
            boolean musical = detectorKaraoke.eEstiloDeMusica(evento.estilo())
                || detectorKaraoke.temTagKaraoke(evento.texto());
            if (musical) {
                musicais.add(evento);
            } else {
                dialogo.add(evento);
                resultado.incrementarDialogoPreservado();
            }
        }

        if (musicais.isEmpty()) {
            logStream.publicarLog(CANAL_LOG, "   Nenhum evento de karaokê/música detectado — arquivo copiado sem alterações.");
            resultado.setTamanhoNovoBytes(bytesOriginais.length);
            if (gravar) {
                Files.write(pastaDestino.resolve(nome), bytesOriginais);
            }
            return resultado;
        }

        // --- reconstrói linhas simples por estilo musical ---
        List<LinhaSimplesKaraoke> linhasSimples = new ArrayList<>();
        List<EventoAss> preservados = new ArrayList<>();
        Map<String, List<EventoAss>> porEstilo = new LinkedHashMap<>();
        for (EventoAss evento : musicais) {
            porEstilo.computeIfAbsent(evento.estilo(), k -> new ArrayList<>()).add(evento);
        }
        for (Map.Entry<String, List<EventoAss>> entrada : porEstilo.entrySet()) {
            processarEstiloMusical(entrada.getKey(), entrada.getValue(), linhasSimples, preservados, resultado);
        }
        linhasSimples = deduplicarLinhasSimples(linhasSimples, resultado);
        linhasSimples.sort(Comparator.comparingLong(LinhaSimplesKaraoke::inicioCs));
        resultado.getLinhasCriadas().addAll(linhasSimples);

        // --- monta o arquivo de saída ---
        List<String> saida = montarSaida(cabecalho, idxUltimoEstilo, playResY, dialogo, preservados, linhasSimples);
        String novoConteudo = String.join(crlf ? "\r\n" : "\n", saida) + (crlf ? "\r\n" : "\n");
        byte[] novosBytes = novoConteudo.getBytes(StandardCharsets.UTF_8);
        resultado.setTamanhoNovoBytes(novosBytes.length + (temBom ? 3 : 0));

        Path destino = pastaDestino.resolve(nome);
        resultado.setArquivoDestino(destino.toString());
        if (gravar) {
            byte[] gravavel;
            if (temBom) {
                gravavel = new byte[novosBytes.length + 3];
                gravavel[0] = (byte) 0xEF;
                gravavel[1] = (byte) 0xBB;
                gravavel[2] = (byte) 0xBF;
                System.arraycopy(novosBytes, 0, gravavel, 3, novosBytes.length);
            } else {
                gravavel = novosBytes;
            }
            Files.write(destino, gravavel);
        }

        logStream.publicarLog(CANAL_LOG, String.format(Locale.ROOT,
            "   [%s] %s: %d eventos → %d falas preservadas + %d linhas de música simples (%d eventos KFX removidos, %d preservados por segurança) | %d KB → %d KB (-%d%%)",
            gravar ? "GRAVADO" : "SIMULADO", nome,
            resultado.getEventosTotais(), resultado.getEventosDialogoPreservados(), linhasSimples.size(),
            resultado.getEventosKaraokeRemovidos(), resultado.getEventosPreservadosPorSeguranca(),
            resultado.getTamanhoOriginalBytes() / 1024, resultado.getTamanhoNovoBytes() / 1024,
            resultado.getPercentualReducao()));
        return resultado;
    }

    /**
     * Reconstrói as frases de um estilo musical. Candidatas a "linha inteira"
     * têm ao menos duas palavras; fragmentos silábicos do KFX são descartados
     * (estão sempre dentro da janela de uma linha inteira). Se algum evento
     * musical ficar fora de qualquer linha reconstruída, ele é preservado
     * intacto — nunca se perde um trecho de música.
     */
    private void processarEstiloMusical(
        String estilo,
        List<EventoAss> eventosEstilo,
        List<LinhaSimplesKaraoke> linhasSimples,
        List<EventoAss> preservados,
        ResultadoConversaoKaraoke resultado
    ) {
        // agrupa candidatas por texto visível, separando repetições distantes (refrão)
        Map<String, List<EventoAss>> porTexto = new LinkedHashMap<>();
        for (EventoAss evento : eventosEstilo) {
            String visivel = limparArtefatosVisiveis(evento.textoVisivel());
            if (contarPalavras(visivel) >= 2) {
                porTexto.computeIfAbsent(visivel, k -> new ArrayList<>()).add(evento);
            }
        }

        List<Grupo> grupos = new ArrayList<>();
        for (Map.Entry<String, List<EventoAss>> entrada : porTexto.entrySet()) {
            List<EventoAss> doTexto = new ArrayList<>(entrada.getValue());
            doTexto.sort(Comparator.comparingLong(EventoAss::inicioCs));
            Grupo atual = null;
            for (EventoAss evento : doTexto) {
                if (atual != null && evento.inicioCs() - atual.fimCs <= GAP_MESMA_FRASE_CS) {
                    atual.absorver(evento);
                } else {
                    atual = new Grupo(entrada.getKey(), evento);
                    grupos.add(atual);
                }
            }
        }

        // funde variantes divergentes (mesma janela, texto parecido) por voto majoritário
        grupos.sort(Comparator.comparingLong(g -> g.inicioCs));
        List<Grupo> finais = new ArrayList<>();
        for (Grupo grupo : grupos) {
            Grupo variante = null;
            for (Grupo candidato : finais) {
                if (sobreposicao(grupo, candidato) >= SOBREPOSICAO_MINIMA_VARIANTE
                        && similaridadeTokens(grupo.texto, candidato.texto) >= SIMILARIDADE_MINIMA_TOKENS) {
                    variante = candidato;
                    break;
                }
            }
            if (variante == null) {
                finais.add(grupo);
            } else {
                variante.fundirVariante(grupo);
                logStream.publicarLog(CANAL_LOG, String.format(Locale.ROOT,
                    "   [VOTO] %s: variantes divergentes na janela %s — mantida a majoritária (\"%s\")",
                    estilo, variante.janela(), resumir(variante.texto)));
            }
        }

        for (Grupo grupo : finais) {
            LinhaSimplesKaraoke linha = grupo.paraLinha();
            linhasSimples.add(linha);
            logStream.publicarLog(CANAL_LOG, String.format(Locale.ROOT,
                "   [TROCA] %s %s–%s | %d eventos KFX → 1 linha: \"%s\"",
                estilo, linha.inicioAss(), linha.fimAss(), grupo.totalEventosJanela(eventosEstilo), resumir(linha.texto())));
        }

        List<EventoAss> semCoberturaInicial = eventosEstilo.stream()
            .filter(evento -> finais.stream().noneMatch(g -> cobre(g, evento)))
            .toList();
        List<GrupoFragmentos> gruposFragmentos = reconstruirFragmentosKfx(estilo, semCoberturaInicial);
        for (GrupoFragmentos grupo : gruposFragmentos) {
            LinhaSimplesKaraoke linha = grupo.paraLinha();
            linhasSimples.add(linha);
            logStream.publicarLog(CANAL_LOG, String.format(Locale.ROOT,
                "   [KFX LIMPO] %s %s–%s | %d fragmentos animados → 1 linha simples: \"%s\"",
                estilo, linha.inicioAss(), linha.fimAss(), linha.eventosOrigem(), resumir(linha.texto())));
        }

        int removidos = 0;
        List<EventoAss> semCobertura = new ArrayList<>();
        for (EventoAss evento : eventosEstilo) {
            boolean coberto = finais.stream().anyMatch(g -> cobre(g, evento))
                || gruposFragmentos.stream().anyMatch(g -> g.cobre(evento));
            boolean efeitoVazio = eEventoKfxSimplificavel(evento) && limparArtefatosVisiveis(evento.textoVisivel()).isBlank();
            if (coberto || efeitoVazio) {
                removidos++;
            } else {
                semCobertura.add(evento);
            }
        }
        for (int i = 0; i < removidos; i++) {
            resultado.incrementarKaraokeRemovido();
        }
        if (!semCobertura.isEmpty()) {
            preservados.addAll(semCobertura);
            resultado.adicionarPreservadosPorSeguranca(semCobertura.size());
            String aviso = String.format(Locale.ROOT,
                "Estilo \"%s\": %d eventos de música sem linha inteira correspondente foram PRESERVADOS intactos (KFX apenas silábico?).",
                estilo, semCobertura.size());
            resultado.adicionarAviso(aviso);
            logStream.publicarLog(CANAL_LOG, "   [AVISO] " + aviso);
        }
    }

    private List<GrupoFragmentos> reconstruirFragmentosKfx(String estilo, List<EventoAss> eventos) {
        List<FragmentoKfx> fragmentos = new ArrayList<>();
        int ordem = 0;
        for (EventoAss evento : eventos) {
            if (!eEventoKfxSimplificavel(evento)) {
                continue;
            }
            double[] coordenada = coordenadaPrincipal(evento.texto());
            String texto = limparArtefatosVisiveis(evento.textoVisivel());
            fragmentos.add(new FragmentoKfx(evento, texto, coordenada[0], coordenada[1], ordem++));
        }
        fragmentos.sort(Comparator
            .comparingInt(FragmentoKfx::faixaVertical)
            .thenComparingLong(f -> f.evento().inicioCs())
            .thenComparingDouble(FragmentoKfx::xOrdenacao)
            .thenComparingInt(FragmentoKfx::ordem));
        if (fragmentos.isEmpty()) {
            return List.of();
        }

        List<GrupoFragmentos> grupos = new ArrayList<>();
        GrupoFragmentos atual = null;
        for (FragmentoKfx fragmento : fragmentos) {
            EventoAss evento = fragmento.evento();
            if (atual == null
                    || atual.faixaVertical != fragmento.faixaVertical()
                    || evento.inicioCs() - atual.fimCs > GAP_FRAGMENTOS_KFX_CS) {
                if (atual != null && atual.eLinhaUtil()) {
                    grupos.add(atual);
                }
                atual = new GrupoFragmentos(fragmento);
            } else {
                atual.absorver(fragmento);
            }
        }
        if (atual != null && atual.eLinhaUtil()) {
            grupos.add(atual);
        }

        if (!grupos.isEmpty()) {
            String aviso = String.format(Locale.ROOT,
                "Estilo \"%s\": linha inteira não encontrada em parte do KFX; reconstrução por fragmentos ativada para remover animações pesadas.",
                estilo);
            logStream.publicarLog(CANAL_LOG, "   [INFO] " + aviso);
        }
        return grupos;
    }

    private boolean eEventoKfxSimplificavel(EventoAss evento) {
        return detectorKaraoke.temTagKaraoke(evento.texto()) || detectorKaraoke.eEfeitoKaraoke(evento.texto());
    }

    private static double[] coordenadaPrincipal(String texto) {
        if (texto == null) {
            return new double[] {Double.NaN, Double.NaN};
        }
        Matcher matcher = PADRAO_COORDENADA_PRINCIPAL.matcher(texto);
        if (!matcher.find()) {
            return new double[] {Double.NaN, Double.NaN};
        }
        return new double[] {Double.parseDouble(matcher.group(1)), Double.parseDouble(matcher.group(2))};
    }

    private static boolean cobre(Grupo grupo, EventoAss evento) {
        return evento.inicioCs() < grupo.fimCs + MARGEM_COBERTURA_CS
            && evento.fimCs() > grupo.inicioCs - MARGEM_COBERTURA_CS;
    }

    private List<String> montarSaida(
        List<String> cabecalho,
        int idxUltimoEstilo,
        int playResY,
        List<EventoAss> dialogo,
        List<EventoAss> preservados,
        List<LinhaSimplesKaraoke> linhasSimples
    ) {
        List<String> saida = new ArrayList<>(cabecalho);
        if (!linhasSimples.isEmpty()) {
            String estiloNovo = definirEstiloSimples(playResY);
            if (idxUltimoEstilo >= 0) {
                saida.add(idxUltimoEstilo + 1, estiloNovo);
            } else {
                saida.add(estiloNovo);
            }
        }

        record Emissao(long inicioCs, int ordem, String linha) {}
        List<Emissao> emissoes = new ArrayList<>();
        int ordem = 0;
        for (EventoAss e : dialogo) {
            emissoes.add(new Emissao(e.inicioCs(), ordem++, e.linhaCrua()));
        }
        for (EventoAss e : preservados) {
            emissoes.add(new Emissao(e.inicioCs(), ordem++, e.linhaCrua()));
        }
        for (LinhaSimplesKaraoke linha : linhasSimples) {
            emissoes.add(new Emissao(linha.inicioCs(), ordem++, linha.paraEventoAss(NOME_ESTILO_SIMPLES)));
        }
        emissoes.sort(Comparator.comparingLong(Emissao::inicioCs).thenComparingInt(Emissao::ordem));
        for (Emissao emissao : emissoes) {
            saida.add(emissao.linha());
        }
        return saida;
    }

    /**
     * Estilo da legenda simples: topo da tela ({@code an8}), fonte Arial (sempre
     * disponível — evita o problema de fontes legadas de fansub), tamanho
     * proporcional ao PlayResY do arquivo.
     */
    private String definirEstiloSimples(int playResY) {
        int base = playResY > 0 ? playResY : 360;
        int tamanho = Math.max(14, Math.round(base / 22.0f));
        int margemV = Math.max(8, Math.round(base / 54.0f));
        return "Style: " + NOME_ESTILO_SIMPLES + ",Arial," + tamanho
            + ",&H00FFFFFF,&H000000FF,&H00000000,&H96000000,0,-1,0,0,100,100,0,0,1,2,1,8,30,30," + margemV + ",1";
    }

    private List<LinhaSimplesKaraoke> deduplicarLinhasSimples(
        List<LinhaSimplesKaraoke> linhas,
        ResultadoConversaoKaraoke resultado
    ) {
        Map<String, List<LinhaSimplesKaraoke>> porJanela = new LinkedHashMap<>();
        for (LinhaSimplesKaraoke linha : linhas) {
            String chave = linha.inicioCs() + "|" + linha.fimCs();
            porJanela.computeIfAbsent(chave, k -> new ArrayList<>()).add(linha);
        }

        List<LinhaSimplesKaraoke> deduplicadas = new ArrayList<>();
        int removidas = 0;
        for (List<LinhaSimplesKaraoke> grupo : porJanela.values()) {
            if (grupo.size() == 1) {
                deduplicadas.add(grupo.getFirst());
                continue;
            }
            LinhaSimplesKaraoke melhor = grupo.stream()
                .min(Comparator.comparingInt(this::pontuarPreferenciaLinha)
                    .thenComparing(Comparator.comparingInt((LinhaSimplesKaraoke l) -> l.eventosOrigem()).reversed()))
                .orElse(grupo.getFirst());
            deduplicadas.add(melhor);
            removidas += grupo.size() - 1;
            logStream.publicarLog(CANAL_LOG, String.format(Locale.ROOT,
                "   [DEDUP] %s–%s | %d camadas simultâneas → mantida: \"%s\"",
                melhor.inicioAss(), melhor.fimAss(), grupo.size(), resumir(melhor.texto())));
        }

        if (removidas > 0) {
            String aviso = removidas + " linha(s) simultânea(s) de karaokê foram removidas por deduplicação romaji/PT-BR.";
            resultado.adicionarAviso(aviso);
            logStream.publicarLog(CANAL_LOG, "   [AVISO] " + aviso);
        }
        return deduplicadas;
    }

    private int pontuarPreferenciaLinha(LinhaSimplesKaraoke linha) {
        String texto = linha.texto();
        int pontuacao = 0;
        if (temArtefatoColchetes(texto)) {
            pontuacao += 30;
        }
        if (pareceOriginalJaponesOuRomaji(texto)) {
            pontuacao += 100;
        }
        pontuacao -= Math.min(8, contarPalavras(texto));
        pontuacao -= pontuacaoPortugues(texto) * 4;
        if (NAO_ASCII_PATTERN.matcher(texto).find() && !PADRAO_JAPONES.matcher(texto).find()) {
            pontuacao -= 8;
        }
        return pontuacao;
    }

    private static String limparArtefatosVisiveis(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        String limpo = texto.replaceAll("(?i)\\[!?\\[?TAG\\d+\\]?\\]", "")
            .replace("[[]]", "")
            .replace("[[", "[")
            .replace("]]", "]");

        limpo = PADRAO_ARTEFATO_TAG_VISIVEL.matcher(limpo).replaceAll("");
        for (int i = 0; i < 4; i++) {
            limpo = limpo.replaceAll("\\[\\s*\\]", "");
        }
        return limpo
            .replaceAll("\\s+([,.!?;:])", "$1")
            .replaceAll("\\s{2,}", " ")
            .strip();
    }

    private static boolean temArtefatoColchetes(String texto) {
        return texto != null && (texto.contains("[]") || texto.matches("(?i).*\\[!?\\[?TAG\\d+\\]?.*"));
    }

    private static boolean pareceOriginalJaponesOuRomaji(String texto) {
        if (texto == null || texto.isBlank()) {
            return false;
        }
        if (PADRAO_JAPONES.matcher(texto).find()) {
            return true;
        }
        String normalizado = texto.toLowerCase(Locale.ROOT)
            .replace('ā', 'a').replace('ī', 'i').replace('ū', 'u')
            .replace('ē', 'e').replace('ō', 'o')
            .replace("'", "");
        if (NAO_ASCII_PATTERN.matcher(normalizado).find()) {
            return false;
        }

        int palavras = 0;
        int letras = 0;
        for (String palavra : normalizado.split("[^a-z]+")) {
            if (palavra.isBlank()) {
                continue;
            }
            if (!PALAVRA_ROMAJI_PATTERN.matcher(palavra).matches()) {
                return false;
            }
            palavras++;
            letras += palavra.length();
        }
        return palavras >= 2 && letras >= 6;
    }

    private static int pontuacaoPortugues(String texto) {
        String normalizado = texto.toLowerCase(Locale.ROOT)
            .replace('á', 'a').replace('à', 'a').replace('ã', 'a').replace('â', 'a')
            .replace('é', 'e').replace('ê', 'e')
            .replace('í', 'i')
            .replace('ó', 'o').replace('õ', 'o').replace('ô', 'o')
            .replace('ú', 'u')
            .replace('ç', 'c');
        int pontos = 0;
        for (String palavra : normalizado.split("[^a-z0-9]+")) {
            pontos += switch (palavra) {
                case "nao", "voce", "voces", "eu", "meu", "minha", "mundo", "verdadeiro",
                    "ceu", "fugaz", "adeus", "crepusculo", "eterno", "ignorancia",
                    "deliberada", "voluntaria", "revoltante", "repugnante", "espectador",
                    "esperando", "flor", "vento", "culpados", "todos", "final", "apenas",
                    "quanto", "deseje", "nada", "muda", "sorrir", "depois", "ver", "oi" -> 2;
                case "de", "do", "da", "dos", "das", "para", "por", "com", "em", "no",
                    "na", "nos", "nas", "que", "uma", "um", "ao", "aos", "e", "o", "a",
                    "os", "as", "se" -> 1;
                default -> 0;
            };
        }
        return pontos;
    }

    private void publicarResumo(List<ResultadoConversaoKaraoke> resultados, int falhas, long duracaoMs, boolean gravar) {
        int arquivos = resultados.size();
        int removidos = resultados.stream().mapToInt(ResultadoConversaoKaraoke::getEventosKaraokeRemovidos).sum();
        int linhas = resultados.stream().mapToInt(r -> r.getLinhasCriadas().size()).sum();
        int preservadosSeguranca = resultados.stream().mapToInt(ResultadoConversaoKaraoke::getEventosPreservadosPorSeguranca).sum();
        long bytesAntes = resultados.stream().mapToLong(ResultadoConversaoKaraoke::getTamanhoOriginalBytes).sum();
        long bytesDepois = resultados.stream().mapToLong(ResultadoConversaoKaraoke::getTamanhoNovoBytes).sum();
        logStream.publicarLog(CANAL_LOG, "");
        logStream.publicarLog(CANAL_LOG, "==== RESUMO ====");
        logStream.publicarLog(CANAL_LOG, String.format(Locale.ROOT,
            "Arquivos processados: %d (%d com falha) | Eventos KFX removidos: %d | Linhas simples criadas: %d | Preservados por segurança: %d",
            arquivos, falhas, removidos, linhas, preservadosSeguranca));
        logStream.publicarLog(CANAL_LOG, String.format(Locale.ROOT,
            "Tamanho total: %d KB → %d KB | Duração: %.1fs",
            bytesAntes / 1024, bytesDepois / 1024, duracaoMs / 1000.0));
        if (falhas == 0) {
            logStream.publicarLog(CANAL_LOG, gravar
                ? "[SUCESSO] Conversão concluída — originais preservados na pasta de origem."
                : "[SUCESSO] Simulação concluída — nenhum arquivo foi gravado.");
        } else {
            logStream.publicarLog(CANAL_LOG, "[ATENÇÃO] Operação concluída com " + falhas + " falha(s) — verifique o log acima.");
        }
    }

    private void registrarTelemetriaEManifesto(Path origem, Path destino, List<ResultadoConversaoKaraoke> resultados, long duracaoMs) {
        int removidos = resultados.stream().mapToInt(ResultadoConversaoKaraoke::getEventosKaraokeRemovidos).sum();
        int linhas = resultados.stream().mapToInt(r -> r.getLinhasCriadas().size()).sum();
        var operacao = TelemetriaService.criarOperacao(
            "NOVO_KARAOKE",
            "KFX → legenda simples: " + origem.getFileName() + " → " + destino,
            duracaoMs,
            resultados.size(),
            removidos,
            linhas
        );
        telemetriaService.registrarOperacao(operacao);
        try {
            Path manifesto = persistencia.salvarManifesto(origem, destino, resultados, duracaoMs);
            logStream.publicarLog(CANAL_LOG, "Manifesto de auditoria salvo em: " + manifesto);
        } catch (IOException e) {
            log.warn("Falha ao salvar manifesto de auditoria do novo karaokê", e);
            logStream.publicarLog(CANAL_LOG, "[AVISO] Não foi possível salvar o manifesto de auditoria: " + e.getMessage());
        }
    }

    private static int contarPalavras(String texto) {
        if (texto == null || texto.isBlank()) {
            return 0;
        }
        return texto.strip().split("\\s+").length;
    }

    private static double sobreposicao(Grupo a, Grupo b) {
        long inicio = Math.max(a.inicioCs, b.inicioCs);
        long fim = Math.min(a.fimCs, b.fimCs);
        long intersecao = Math.max(0, fim - inicio);
        long menor = Math.min(Math.max(1, a.fimCs - a.inicioCs), Math.max(1, b.fimCs - b.inicioCs));
        return (double) intersecao / menor;
    }

    private static double similaridadeTokens(String a, String b) {
        var tokensA = tokens(a);
        var tokensB = tokens(b);
        if (tokensA.isEmpty() || tokensB.isEmpty()) {
            return 0;
        }
        long comuns = tokensA.stream().filter(tokensB::contains).count();
        return (double) comuns / Math.min(tokensA.size(), tokensB.size());
    }

    private static List<String> tokens(String texto) {
        return Stream.of(texto.toLowerCase(Locale.ROOT).split("[^\\p{L}\\d]+"))
            .filter(t -> !t.isBlank())
            .distinct()
            .toList();
    }

    private static String resumir(String texto) {
        return texto.length() <= 70 ? texto : texto.substring(0, 67) + "...";
    }

    /** Grupo de eventos KFX da mesma frase (mesmo texto, janela contígua). */
    private static final class Grupo {
        private String texto;
        private long inicioCs;
        private long fimCs;
        private int eventos;
        private int variantes = 1;

        private Grupo(String texto, EventoAss primeiro) {
            this.texto = texto;
            this.inicioCs = primeiro.inicioCs();
            this.fimCs = primeiro.fimCs();
            this.eventos = 1;
        }

        private void absorver(EventoAss evento) {
            inicioCs = Math.min(inicioCs, evento.inicioCs());
            fimCs = Math.max(fimCs, evento.fimCs());
            eventos++;
        }

        /** Funde uma variante divergente: vence o texto com mais eventos de origem. */
        private void fundirVariante(Grupo outro) {
            if (outro.eventos > this.eventos) {
                this.texto = outro.texto;
                this.eventos = outro.eventos;
            }
            inicioCs = Math.min(inicioCs, outro.inicioCs);
            fimCs = Math.max(fimCs, outro.fimCs);
            variantes += outro.variantes;
        }

        private String janela() {
            return EventoAss.csParaTempo(inicioCs) + "–" + EventoAss.csParaTempo(fimCs);
        }

        private int totalEventosJanela(List<EventoAss> eventosEstilo) {
            return (int) eventosEstilo.stream()
                .filter(e -> e.inicioCs() < fimCs && e.fimCs() > inicioCs)
                .count();
        }

        private LinhaSimplesKaraoke paraLinha() {
            return new LinhaSimplesKaraoke(texto, inicioCs, fimCs, eventos, variantes);
        }
    }

    private record FragmentoKfx(EventoAss evento, String texto, double x, double y, int ordem) {
        private int faixaVertical() {
            return Double.isNaN(y) ? Integer.MAX_VALUE : (int) Math.round(y / 120.0);
        }

        private double xOrdenacao() {
            return Double.isNaN(x) ? Double.MAX_VALUE : x;
        }
    }

    /** Fallback para KFX que só tem sílabas/letras/frames, sem uma linha inteira pronta. */
    private static final class GrupoFragmentos {
        private final List<FragmentoKfx> fragmentos = new ArrayList<>();
        private final List<FragmentoKfx> fragmentosUnicos = new ArrayList<>();
        private final List<String> chavesVistas = new ArrayList<>();
        private final int faixaVertical;
        private long inicioCs;
        private long fimCs;
        private int eventos;
        private String ultimoFragmento;
        private long ultimoInicioCs;

        private GrupoFragmentos(FragmentoKfx primeiro) {
            this.faixaVertical = primeiro.faixaVertical();
            this.inicioCs = primeiro.evento().inicioCs();
            this.fimCs = primeiro.evento().fimCs();
            this.eventos = 0;
            absorver(primeiro);
        }

        private void absorver(FragmentoKfx fragmento) {
            EventoAss evento = fragmento.evento();
            inicioCs = Math.min(inicioCs, evento.inicioCs());
            fimCs = Math.max(fimCs, evento.fimCs());
            eventos++;
            fragmentos.add(fragmento);

            String normalizado = fragmento.texto() == null ? "" : fragmento.texto().strip();
            boolean repeticaoDoMesmoFrame = normalizado.equalsIgnoreCase(ultimoFragmento)
                && evento.inicioCs() - ultimoInicioCs <= GAP_REPETICAO_FRAGMENTO_CS;
            String chavePosicional = chavePosicional(fragmento, normalizado);
            if (!repeticaoDoMesmoFrame && !chavesVistas.contains(chavePosicional)) {
                fragmentosUnicos.add(fragmento);
                chavesVistas.add(chavePosicional);
                ultimoFragmento = normalizado;
                ultimoInicioCs = evento.inicioCs();
            }
        }

        private boolean cobre(EventoAss evento) {
            return evento.inicioCs() < fimCs + MARGEM_COBERTURA_CS
                && evento.fimCs() > inicioCs - MARGEM_COBERTURA_CS;
        }

        private boolean eLinhaUtil() {
            return eventos >= 2 && fragmentosUnicos.stream()
                .map(FragmentoKfx::texto)
                .mapToInt(t -> t == null ? 0 : t.strip().length())
                .sum() >= 2;
        }

        private LinhaSimplesKaraoke paraLinha() {
            return new LinhaSimplesKaraoke(montarTexto(), inicioCs, fimCs, eventos, 1);
        }

        private String montarTexto() {
            List<FragmentoKfx> ordenados = new ArrayList<>(fragmentosUnicos);
            if (ordenados.stream().filter(f -> !Double.isNaN(f.x())).count() >= Math.max(2, ordenados.size() / 2)) {
                ordenados.sort(Comparator.comparingDouble(FragmentoKfx::xOrdenacao).thenComparingInt(FragmentoKfx::ordem));
            } else {
                ordenados.sort(Comparator.comparingLong((FragmentoKfx f) -> f.evento().inicioCs()).thenComparingInt(FragmentoKfx::ordem));
            }

            boolean modoCaracter = ordenados.stream()
                .map(FragmentoKfx::texto)
                .filter(t -> t != null && !t.isBlank())
                .allMatch(t -> t.strip().length() <= 1);

            StringBuilder texto = new StringBuilder();
            for (FragmentoKfx fragmento : ordenados) {
                String parte = fragmento.texto() == null ? "" : fragmento.texto().strip();
                if (parte.isBlank()) {
                    if (!texto.isEmpty() && texto.charAt(texto.length() - 1) != ' ') {
                        texto.append(' ');
                    }
                    continue;
                }
                if (!modoCaracter && !texto.isEmpty() && texto.charAt(texto.length() - 1) != ' ') {
                    texto.append(' ');
                }
                texto.append(parte);
            }
            return texto.toString()
                .replaceAll("\\s+([,.!?;:])", "$1")
                .replaceAll("\\s{2,}", " ")
                .strip();
        }

        private static String chavePosicional(FragmentoKfx fragmento, String texto) {
            long x = Double.isNaN(fragmento.x()) ? Long.MIN_VALUE : Math.round(fragmento.x() / 3.0);
            long y = Double.isNaN(fragmento.y()) ? Long.MIN_VALUE : Math.round(fragmento.y() / 3.0);
            return texto.toLowerCase(Locale.ROOT) + "|" + x + "|" + y;
        }
    }
}
