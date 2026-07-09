package org.traducao.projeto.telemetria;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traducao.projeto.core.util.ProcessoExternoUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Publica a telemetria acumulada como DATASET PÚBLICO num repositório Git
 * DEDICADO ({@code kronos-anime-translation-telemetry-dataset}, seguindo a
 * convenção {@code [NomeDoSistema]-telemetry-dataset} para dados de pesquisa/ML).
 * <p>
 * O serviço é auto-suficiente: se o repositório local não existir, ele clona o
 * remoto configurado (ou inicializa um novo e associa o remoto); na primeira
 * publicação gera README com declaração de anonimização (LGPD/GDPR), LICENSE e
 * a estrutura {@code metrics/}. Cada publicação = 1 commit + push, e o
 * histórico Git é o versionamento natural dos snapshots.
 * <p>
 * Sanitização deliberada — o dataset é feito para consumo externo, então
 * carrega apenas MÉTRICAS: nada de textos de legenda (os avisos viram
 * contagem), nada de caminhos de máquina (o campo {@code detalhe} das
 * operações é descartado e nomes de episódio perdem qualquer diretório).
 */
@ApplicationScoped
public class TelemetriaDatasetService {

    private static final Logger log = LoggerFactory.getLogger(TelemetriaDatasetService.class);

    static final String NOME_ARQUIVO_DATASET = "kronos-telemetria-dataset.json";
    private static final Duration TIMEOUT_GIT = Duration.ofSeconds(30);
    private static final Duration TIMEOUT_REDE = Duration.ofMinutes(2);

    private final TelemetriaService telemetria;
    private final TelemetriaDatasetProperties propriedades;
    private final AmbienteExecucaoDatasetService ambienteExecucao;
    private final ObjectMapper mapper = new ObjectMapper();

    public TelemetriaDatasetService(
            TelemetriaService telemetria,
            TelemetriaDatasetProperties propriedades,
            AmbienteExecucaoDatasetService ambienteExecucao) {
        this.telemetria = telemetria;
        this.propriedades = propriedades;
        this.ambienteExecucao = ambienteExecucao;
    }

    /** Resultado da publicação, devolvido ao painel de Telemetria. */
    public record ResultadoPublicacao(String repositorio, String commit, boolean pushOk, String mensagem) {}

    public synchronized ResultadoPublicacao publicar() throws IOException {
        Path repo = Path.of(propriedades.repositorioLocal()).toAbsolutePath().normalize();
        prepararRepositorio(repo);
        garantirDocumentosBase(repo);

        TelemetriaResumo resumo = telemetria.gerarResumo(Path.of("cache"));
        Path pastaMetrics = repo.resolve("metrics");
        Files.createDirectories(pastaMetrics);
        Path arquivo = pastaMetrics.resolve(NOME_ARQUIVO_DATASET);
        // Pretty-print proposital: o arquivo é lido por humanos no GitHub.
        mapper.writerWithDefaultPrettyPrinter().writeValue(arquivo.toFile(),
            montarDatasetSanitizado(resumo, mapper, ambienteExecucao.detectar(propriedades.hardware())));
        log.info("Dataset de telemetria gerado em {}", arquivo);

        git(repo, TIMEOUT_GIT, "add", "README.md", "LICENSE", "metrics/" + NOME_ARQUIVO_DATASET);
        String mensagemCommit = String.format(Locale.ROOT,
            "dataset: snapshot com %d episódios e %d operações",
            resumo.totalEpisodios(), resumo.operacoes() != null ? resumo.operacoes().size() : 0);
        ProcessoExternoUtil.Resultado commit = git(repo, TIMEOUT_GIT, "commit", "-m", mensagemCommit);
        boolean semMudancas = commit.codigoSaida() != 0
            && saida(commit).toLowerCase(Locale.ROOT).contains("nothing to commit");
        if (commit.codigoSaida() != 0 && !semMudancas) {
            throw new IOException("git commit falhou no repositório do dataset: " + resumir(saida(commit)));
        }
        String hash = saida(git(repo, TIMEOUT_GIT, "rev-parse", "--short", "HEAD")).trim();

        // Push sempre (mesmo sem commit novo): publica commits pendentes de
        // tentativas anteriores sem rede/sem repositório remoto criado.
        ProcessoExternoUtil.Resultado push = git(repo, TIMEOUT_REDE, "push");
        if (push.codigoSaida() != 0 && saida(push).contains("--set-upstream")) {
            push = git(repo, TIMEOUT_REDE, "push", "-u", "origin", "HEAD");
        }
        if (push.codigoSaida() != 0 && saida(push).contains("[rejected]")) {
            // Repositório remoto criado com README/commits próprios (caso real
            // de 2026-07-09): integra o histórico preferindo a versão LOCAL em
            // conflito — o gerador local é a fonte de verdade do dataset.
            git(repo, TIMEOUT_REDE, "pull", "--no-edit", "--allow-unrelated-histories", "-X", "ours", "origin", "main");
            push = git(repo, TIMEOUT_REDE, "push", "-u", "origin", "HEAD");
        }
        boolean pushOk = push.codigoSaida() == 0;
        if (!pushOk) {
            log.warn("git push do dataset falhou: {}", saida(push));
        }

        String mensagem = montarMensagem(semMudancas, pushOk, hash, saida(push));
        return new ResultadoPublicacao(repo.toString(), semMudancas ? "sem mudanças" : hash, pushOk, mensagem);
    }

    private String montarMensagem(boolean semMudancas, boolean pushOk, String hash, String saidaPush) {
        if (pushOk) {
            return semMudancas
                ? "Dataset já estava atualizado — nenhum commit novo; push confirmado."
                : "Dataset publicado no repositório dedicado (commit " + hash + ").";
        }
        String dica = saidaPush != null && (saidaPush.contains("not found") || saidaPush.contains("does not exist"))
            ? " Crie o repositório \"" + nomeRepositorioRemoto() + "\" no GitHub e publique novamente."
            : "";
        return (semMudancas ? "Dataset já estava atualizado" : "Commit " + hash + " criado localmente")
            + ", mas o push falhou: " + resumir(saidaPush) + dica;
    }

    private String nomeRepositorioRemoto() {
        String remoto = propriedades.repositorioRemoto();
        if (remoto == null || remoto.isBlank()) {
            return "kronos-anime-translation-telemetry-dataset";
        }
        String semSufixo = remoto.replaceFirst("\\.git$", "");
        int barra = semSufixo.lastIndexOf('/');
        return barra >= 0 ? semSufixo.substring(barra + 1) : semSufixo;
    }

    /** Clona o remoto configurado ou inicializa um repositório novo com o remoto associado. */
    private void prepararRepositorio(Path repo) throws IOException {
        if (Files.isDirectory(repo.resolve(".git"))) {
            return;
        }
        String remoto = propriedades.repositorioRemoto();
        if (remoto != null && !remoto.isBlank()) {
            log.info("Repositório do dataset não existe em {}; clonando {}", repo, remoto);
            ProcessoExternoUtil.Resultado clone = executarGit(
                List.of("git", "clone", remoto, repo.toString()), TIMEOUT_REDE);
            if (clone.codigoSaida() == 0) {
                return;
            }
            log.warn("Clone do dataset falhou ({}); inicializando repositório local novo.", resumir(saida(clone)));
        }
        Files.createDirectories(repo);
        ProcessoExternoUtil.Resultado init = git(repo, TIMEOUT_GIT, "init", "-b", "main");
        if (init.codigoSaida() != 0) {
            throw new IOException("git init falhou em " + repo + ": " + resumir(saida(init)));
        }
        if (remoto != null && !remoto.isBlank()) {
            git(repo, TIMEOUT_GIT, "remote", "add", "origin", remoto);
        }
    }

    /**
     * Bootstrap do repositório na primeira publicação: README com formato dos
     * dados e declaração de anonimização (LGPD/GDPR) + LICENSE (MIT) — os três
     * itens que a comunidade procura primeiro num repositório de dataset.
     */
    private void garantirDocumentosBase(Path repo) throws IOException {
        Path readme = repo.resolve("README.md");
        if (!Files.exists(readme)) {
            Files.writeString(readme, README_DATASET, StandardCharsets.UTF_8);
        }
        Path licenca = repo.resolve("LICENSE");
        if (!Files.exists(licenca)) {
            Files.writeString(licenca, textoLicencaMit(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Monta o snapshot sanitizado: métricas por episódio e por operação, sem
     * textos de fala (avisos viram {@code quantidadeAvisos}) e sem caminhos de
     * máquina ({@code detalhe} descartado; episódio reduzido ao nome do arquivo).
     */
    static ObjectNode montarDatasetSanitizado(TelemetriaResumo resumo, ObjectMapper mapper) {
        return montarDatasetSanitizado(resumo, mapper, null);
    }

    static ObjectNode montarDatasetSanitizado(
            TelemetriaResumo resumo,
            ObjectMapper mapper,
            AmbienteExecucaoDataset ambienteExecucao) {
        ObjectNode root = mapper.createObjectNode();
        root.put("dataset", "kronos-anime-translation-telemetry-dataset");
        root.put("versaoFormato", 1);
        root.put("geradoEm", Instant.now().toString());
        root.put("descricao", "Métricas operacionais de tradução de legendas de anime com LLM 100% local "
            + "(LM Studio). Sem textos de legenda e sem caminhos de máquina — apenas métricas.");

        adicionarAmbienteExecucao(root, ambienteExecucao);

        ObjectNode agregado = root.putObject("resumo");
        agregado.put("totalEpisodiosTraduzidos", resumo.totalEpisodios());
        agregado.put("totalLinhasTraduzidas", resumo.totalLinhas());
        agregado.put("tempoMedioPorLinhaMs", resumo.tempoMedioPorLinhaMs());
        agregado.put("totalFalasReaproveitadasDoCache", resumo.totalCacheHits());
        agregado.put("alucinacoesLlmPrevenidas", resumo.alucinacoesPrevenidas());
        agregado.put("arquivosRenomeados", resumo.arquivosSanitizados());
        agregado.put("totalOperacoesRegistradas", resumo.operacoes() != null ? resumo.operacoes().size() : 0);

        ArrayNode traducoes = root.putArray("traducoesLlm");
        if (resumo.traducoesLlm() != null) {
            for (LlmTelemetria t : resumo.traducoesLlm()) {
                ObjectNode item = traducoes.addObject();
                item.put("episodio", apenasNomeDeArquivo(t.nomeEpisodio()));
                item.put("anime", t.animeNome());
                item.put("temporada", t.temporada());
                item.put("modeloLlm", t.modeloLlm());
                item.put("totalLinhas", t.totalLinhas());
                item.put("falasTraduzidas", t.falasTraduzidas());
                item.put("falasDoCache", t.falasDoCache());
                item.put("tempoTotalMs", t.tempoTotalMs());
                item.put("quantidadeAvisos", contarAvisos(t.errosOcorridos()));
                item.put("registradoEm", t.registradoEm());
            }
        }

        ArrayNode operacoes = root.putArray("operacoes");
        if (resumo.operacoes() != null) {
            for (OperacaoTelemetria op : resumo.operacoes()) {
                ObjectNode item = operacoes.addObject();
                item.put("tipo", op.tipo());
                item.put("tempoTotalMs", op.tempoTotalMs());
                item.put("arquivosProcessados", op.arquivosProcessados());
                item.put("itensDetectados", op.itensDetectados());
                item.put("itensCorrigidos", op.itensCorrigidos());
                item.put("registradoEm", op.registradoEm());
            }
        }
        return root;
    }

    private static void adicionarAmbienteExecucao(ObjectNode root, AmbienteExecucaoDataset ambiente) {
        if (ambiente == null) {
            return;
        }
        ObjectNode node = root.putObject("ambienteExecucao");
        putIfPresent(node, "fabricante", ambiente.fabricante());
        putIfPresent(node, "modeloMaquina", ambiente.modeloMaquina());
        putIfPresent(node, "cpu", ambiente.cpu());
        putIfPresent(node, "gpuPrincipal", ambiente.gpuPrincipal());
        putIfPresent(node, "gpuDetectadaSistema", ambiente.gpuDetectadaSistema());
        if (ambiente.ramTotalGb() != null) {
            node.put("ramTotalGb", ambiente.ramTotalGb());
        }
        putIfPresent(node, "sistemaOperacional", ambiente.sistemaOperacional());
        putIfPresent(node, "arquitetura", ambiente.arquitetura());
        node.put("hardwareColetadoAutomaticamente", ambiente.hardwareColetadoAutomaticamente());
        node.put("gpuPublicaConfigurada", ambiente.gpuPublicaConfigurada());
    }

    private static void putIfPresent(ObjectNode node, String campo, String valor) {
        if (valor != null && !valor.isBlank()) {
            node.put(campo, valor);
        }
    }

    private static final java.util.regex.Pattern MARCADOR_AVISOS_OMITIDOS =
        java.util.regex.Pattern.compile("\\(\\+(\\d+) avisos omitidos");

    /**
     * Conta os avisos REAIS do episódio: a telemetria canônica guarda no
     * máximo 30 avisos + uma linha-resumo "(+N avisos omitidos...)" (ver
     * {@link TelemetriaService}); aqui o total é reconstituído para o dataset
     * não subnotificar a métrica de qualidade.
     */
    static int contarAvisos(List<String> avisos) {
        if (avisos == null || avisos.isEmpty()) {
            return 0;
        }
        var matcher = MARCADOR_AVISOS_OMITIDOS.matcher(avisos.getLast());
        if (matcher.find()) {
            return avisos.size() - 1 + Integer.parseInt(matcher.group(1));
        }
        return avisos.size();
    }

    static String apenasNomeDeArquivo(String nome) {
        if (nome == null) {
            return null;
        }
        String normalizado = nome.replace('\\', '/');
        int barra = normalizado.lastIndexOf('/');
        return barra >= 0 ? normalizado.substring(barra + 1) : nome;
    }

    private ProcessoExternoUtil.Resultado git(Path repo, Duration timeout, String... argumentos) throws IOException {
        List<String> comando = new ArrayList<>(List.of("git", "-C", repo.toString()));
        comando.addAll(List.of(argumentos));
        return executarGit(comando, timeout);
    }

    private ProcessoExternoUtil.Resultado executarGit(List<String> comando, Duration timeout) throws IOException {
        try {
            return ProcessoExternoUtil.executar(comando, timeout, true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Publicação do dataset interrompida.", e);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new IOException("Comando git excedeu o tempo limite: " + String.join(" ", comando), e);
        }
    }

    private static String saida(ProcessoExternoUtil.Resultado resultado) {
        return new String(resultado.stdout(), StandardCharsets.UTF_8);
    }

    private static String resumir(String texto) {
        String plano = texto == null ? "" : texto.replaceAll("\\s+", " ").trim();
        return plano.length() > 180 ? plano.substring(0, 177) + "..." : plano;
    }

    private static String textoLicencaMit() {
        return "MIT License\n\n"
            + "Copyright (c) " + Year.now() + " Paulo André Carminati\n\n"
            + "Permission is hereby granted, free of charge, to any person obtaining a copy\n"
            + "of this software and associated documentation files (the \"Software\"), to deal\n"
            + "in the Software without restriction, including without limitation the rights\n"
            + "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell\n"
            + "copies of the Software, and to permit persons to whom the Software is\n"
            + "furnished to do so, subject to the following conditions:\n\n"
            + "The above copyright notice and this permission notice shall be included in all\n"
            + "copies or substantial portions of the Software.\n\n"
            + "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR\n"
            + "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,\n"
            + "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE\n"
            + "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER\n"
            + "LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,\n"
            + "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE\n"
            + "SOFTWARE.\n";
    }

    private static final String README_DATASET = """
        # KRONOS CORE — Telemetry Dataset

        > **English summary:** operational metrics dataset from [KRONOS CORE](https://github.com/carmipa/traducao_animes_llm_local_quarkus),
        > an industrial anime-subtitle translation pipeline running a 100% local LLM (LM Studio).
        > Metrics only — no subtitle texts, no personal data, no machine paths. Updated via one-click
        > publish from the running system; the Git history is the snapshot timeline.

        Dataset de **métricas operacionais** do [KRONOS CORE](https://github.com/carmipa/traducao_animes_llm_local_quarkus) —
        pipeline industrial de tradução de legendas de anime com **LLM 100% local** (LM Studio, sem nuvem).
        Cada commit é um snapshot; o histórico Git é a linha do tempo do dataset.

        ## Estrutura

        ```
        ├── README.md
        ├── LICENSE
        └── metrics/
            └── kronos-telemetria-dataset.json
        ```

        ## Formato dos dados

        JSON próprio (documentado abaixo), UTF-8, com `versaoFormato` para evolução do schema.

        ### `resumo` (agregado)

        | Campo | Significado |
        |-------|-------------|
        | `totalEpisodiosTraduzidos` | Episódios processados pelo pipeline de tradução LLM |
        | `totalLinhasTraduzidas` | Falas de legenda traduzidas |
        | `tempoMedioPorLinhaMs` | Latência média de tradução por fala (LLM local) |
        | `totalFalasReaproveitadasDoCache` | Falas resolvidas pelo cache persistente (sem chamada de LLM) |
        | `alucinacoesLlmPrevenidas` | Respostas de LLM rejeitadas pelas guardas anti-alucinação |
        | `arquivosRenomeados` | Arquivos padronizados pelo módulo de renomeação |
        | `totalOperacoesRegistradas` | Operações de pipeline registradas (todos os módulos) |

        ### `ambienteExecucao` (snapshot de hardware seguro)

        | Campo | Significado |
        |-------|-------------|
        | `fabricante` / `modeloMaquina` | Fabricante e modelo genérico reportados pelo sistema |
        | `cpu` | Nome público do processador |
        | `gpuPrincipal` | GPU publicada para comparação de benchmark |
        | `gpuDetectadaSistema` | Nome detectado pelo driver/SO quando diferente do nome público configurado |
        | `ramTotalGb` | RAM física total arredondada em GB |
        | `sistemaOperacional` / `arquitetura` | Plataforma de execução sem usuário, hostname ou caminhos |
        | `hardwareColetadoAutomaticamente` | Indica se a coleta veio do sistema local |
        | `gpuPublicaConfigurada` | Indica se houve override público da GPU (ex: nome comercial conhecido) |

        ### `traducoesLlm[]` (por episódio)

        | Campo | Significado |
        |-------|-------------|
        | `episodio` | Nome do arquivo de legenda (sem diretórios) |
        | `anime` / `temporada` | Obra e temporada |
        | `modeloLlm` | Modelo local usado (id reportado pelo LM Studio) |
        | `totalLinhas` / `falasTraduzidas` / `falasDoCache` | Volume e origem das traduções |
        | `tempoTotalMs` | Duração total da tradução do episódio |
        | `quantidadeAvisos` | Quantidade de avisos de qualidade (falas mantidas sem tradução, suspeitas etc.) |
        | `registradoEm` | Timestamp UTC (ISO-8601) |

        ### `operacoes[]` (por operação de pipeline)

        `tipo`, `tempoTotalMs`, `arquivosProcessados`, `itensDetectados`, `itensCorrigidos`, `registradoEm` —
        cobre remux, extração, revisões (lore/concordância), karaokê, renomeação e auditorias.

        ## Anonimização (LGPD/GDPR)

        Este dataset **não contém PII**: sem textos de legenda (conteúdo protegido vira apenas contagem
        de avisos), sem caminhos de disco/usuário, sem IPs, tokens, credenciais, hostname, MAC,
        número de série ou identificadores de dispositivo. Os únicos identificadores são nomes
        públicos de obras/arquivos de release, ids de modelos LLM e metadados genéricos de hardware.

        ## Licença

        [MIT](LICENSE) — uso livre com atribuição.

        ## Como é gerado

        Botão **"Publicar Dataset"** no painel de Telemetria do KRONOS CORE: o sistema sanitiza a
        telemetria acumulada, escreve `metrics/kronos-telemetria-dataset.json`, commita e faz push.
        """;
}
