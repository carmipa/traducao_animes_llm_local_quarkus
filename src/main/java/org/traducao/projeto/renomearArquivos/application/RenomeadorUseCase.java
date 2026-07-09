package org.traducao.projeto.renomearArquivos.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traducao.projeto.core.util.DuracaoUtil;
import org.traducao.projeto.renomearArquivos.domain.OperacaoRenomeacao;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.traducao.presentation.web.LogStreamService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@ApplicationScoped
public class RenomeadorUseCase {
    private static final Logger log = LoggerFactory.getLogger(RenomeadorUseCase.class);
    private static final Path PASTA_UNDO_PROJETO =
        TelemetriaService.resolverPastaArtefatosOperacionais("renomear-arquivos").resolve("undo");
    private static final String PREFIXO_ARQUIVO_UNDO = "kronos_undo_renomeacao_";
    
    // Regex para pegar o episódio de trackers.
    // Ex: "[SubsPlease] Nome Anime - 01 (1080p).mkv" -> 01
    // Ex: "[DB]86_-_01_(Dual Audio_10bit_BD1080p_x265)_PTBR.mkv" -> 01
    // Ex: "Anime - 02.mkv" -> 02
    // Ex: "Anime Ep 03.mkv" -> 03
    // Ex: "Anime E04.mkv" -> 04
    private static final Pattern EPISODE_SEPARATOR_PATTERN = Pattern.compile("(?:^|[\\s._])[-–—][\\s._]*(\\d{1,4})(?=$|[\\s._(\\[]|v\\d)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EPISODE_LABEL_PATTERN = Pattern.compile("\\b(?:Ep|Epis[oó]dio|Episode|E)\\s*(\\d{1,4})(?=$|[\\s._(\\[]|v\\d)", Pattern.CASE_INSENSITIVE);
    
    // Regex fallback: depois de remover metadados técnicos, pega o último número
    // restante. Isso evita usar o "86" do título como episódio quando há "86 01".
    private static final Pattern EPISODE_FALLBACK = Pattern.compile("(?<!\\d)(\\d{1,4})(?!\\d)");

    // Conteúdo especial/creditless (NCOP, NCED, SP, OVA, PV...): não segue a numeração
    // dos episódios e o fallback numérico geraria nomes errados (ex.: NCED01 viraria
    // S01E02 por causa do "Track2" no nome). Esses arquivos são pulados.
    private static final Pattern CONTEUDO_ESPECIAL_PATTERN = Pattern.compile(
        "(?i)(?:^|[\\s._\\-\\[(])(?:NC(?:OP|ED)\\d*|OVA|OAD|SP\\d*|PV\\d*|Menu|Preview|Special)(?=$|[\\s._\\-\\])(])");

    private static final List<String> EXTENSOES_VIDEO = List.of(
        ".mkv", ".mp4", ".avi", ".webm", ".mov", ".m4v", ".wmv", ".flv", ".ts"
    );
    private static final List<String> EXTENSOES_LEGENDA = List.of(
        ".ass", ".ssa", ".srt", ".vtt", ".sub"
    );

    @Inject
    ObjectMapper objectMapper;

    @Inject
    TelemetriaService telemetriaService;

    @Inject
    LogStreamService logStream;

    public List<OperacaoRenomeacao.ItemRenomeado> simularRenomeacao(Path pasta, String nomePadrao) {
        long inicioMs = System.currentTimeMillis();
        List<OperacaoRenomeacao.ItemRenomeado> itens = simularInterno(pasta, nomePadrao);
        logStream.publicarLog("renomear-arquivos",
            DuracaoUtil.linhaRelatorioFinal("Renomear Arquivos (simulação)", inicioMs));
        return itens;
    }

    // Núcleo da simulação sem o relatório final: a aplicação reusa este método
    // e emite um único relatório ao término, em vez de um por fase.
    private List<OperacaoRenomeacao.ItemRenomeado> simularInterno(Path pasta, String nomePadrao) {
        logStream.publicarLog("renomear-arquivos", "Iniciando simulação (Dry-Run) em: " + pasta);
        List<OperacaoRenomeacao.ItemRenomeado> itens = new ArrayList<>();
        
        if (!Files.exists(pasta) || !Files.isDirectory(pasta)) {
            logStream.publicarLog("renomear-arquivos", "O caminho fornecido não existe ou não é um diretório.");
            return itens;
        }

        try (Stream<Path> stream = Files.list(pasta)) {
            List<Path> arquivos = stream.filter(Files::isRegularFile)
                .filter(a -> isVideoFile(a) || isLegendaFile(a))
                .sorted()
                .toList();

            if (arquivos.isEmpty()) {
                logStream.publicarLog("renomear-arquivos", "Nenhum vídeo ou legenda encontrado na pasta. Extensões aceitas: "
                    + String.join(", ", EXTENSOES_VIDEO) + ", " + String.join(", ", EXTENSOES_LEGENDA) + ".");
            }

            long videosRenomeaveis = arquivos.stream()
                .filter(this::isVideoFile)
                .filter(a -> !ehConteudoEspecial(a.getFileName().toString()))
                .count();
            long legendasRenomeaveis = arquivos.stream()
                .filter(this::isLegendaFile)
                .filter(a -> !ehConteudoEspecial(a.getFileName().toString()))
                .count();

            Set<String> nomesGerados = new HashSet<>();
            for (Path arquivo : arquivos) {
                String nomeOriginal = arquivo.getFileName().toString();

                if (ehConteudoEspecial(nomeOriginal)) {
                    logStream.publicarLog("renomear-arquivos", "[IGNORADO] " + nomeOriginal + " é conteúdo especial (SP/NCOP/NCED/OVA/PV) sem numeração de episódio; renomeie manualmente se necessário.");
                    continue;
                }

                String extensao = obterExtensao(nomeOriginal);
                String episodio = extrairEpisodio(nomeOriginal);
                boolean renomearComoFilme = isVideoFile(arquivo) ? videosRenomeaveis == 1 : legendasRenomeaveis == 1;
                String nomeNovo = gerarNomeNovo(nomeOriginal, nomePadrao, extensao, episodio, renomearComoFilme);

                if (!nomeOriginal.equals(nomeNovo)) {
                    if (!nomesGerados.add(nomeNovo.toLowerCase(Locale.ROOT))) {
                        logStream.publicarLog("renomear-arquivos", "[CONFLITO] " + nomeOriginal + " geraria o mesmo nome de outro arquivo da pasta (" + nomeNovo + "); ignorado para evitar sobrescrita.");
                        continue;
                    }
                    if (Files.exists(pasta.resolve(nomeNovo))) {
                        logStream.publicarLog("renomear-arquivos", "[CONFLITO] " + nomeOriginal + " não será renomeado: já existe um arquivo chamado " + nomeNovo + " na pasta.");
                        continue;
                    }
                    itens.add(new OperacaoRenomeacao.ItemRenomeado(nomeOriginal, nomeNovo));
                    logStream.publicarLog("renomear-arquivos", "[DRY-RUN] " + nomeOriginal + " \u001b[33m->\u001b[0m " + nomeNovo);
                } else if (episodio == null) {
                    logStream.publicarLog("renomear-arquivos", "[IGNORADO] " + nomeOriginal + " sem episódio detectável no nome. Para filmes, deixe apenas um vídeo (e uma legenda) na pasta.");
                } else {
                    logStream.publicarLog("renomear-arquivos", "[IGNORADO] " + nomeOriginal + " já está no padrão.");
                }
            }
            logStream.publicarLog("renomear-arquivos", "Simulação concluída. " + itens.size() + " arquivos seriam renomeados.");
        } catch (IOException e) {
            logStream.publicarLog("renomear-arquivos", "Erro ao acessar a pasta: " + e.getMessage());
        }
        
        return itens;
    }

    public void aplicarRenomeacao(Path pasta, String nomePadrao) {
        long inicioMs = System.currentTimeMillis();
        logStream.publicarLog("renomear-arquivos", "Iniciando APLICAÇÃO da renomeação em: " + pasta);
        List<OperacaoRenomeacao.ItemRenomeado> simulação = simularInterno(pasta, nomePadrao);
        
        if (simulação.isEmpty()) {
            logStream.publicarLog("renomear-arquivos", "Nenhum arquivo precisa ser renomeado.");
            return;
        }

        List<OperacaoRenomeacao.ItemRenomeado> aplicados = new ArrayList<>();
        int sucesso = 0;
        int erros = 0;

        for (OperacaoRenomeacao.ItemRenomeado item : simulação) {
            Path origem = pasta.resolve(item.nomeOriginal());
            Path destino = pasta.resolve(item.nomeNovo());
            
            try {
                if (Files.exists(destino)) {
                    logStream.publicarLog("renomear-arquivos", "[ERRO] Arquivo de destino já existe: " + destino.getFileName());
                    erros++;
                    continue;
                }
                
                Files.move(origem, destino, StandardCopyOption.ATOMIC_MOVE);
                aplicados.add(item);
                sucesso++;
                logStream.publicarLog("renomear-arquivos", "[OK] " + item.nomeOriginal() + " renomeado para " + item.nomeNovo());
            } catch (Exception e) {
                logStream.publicarLog("renomear-arquivos", "[ERRO] Falha ao renomear " + item.nomeOriginal() + ": " + e.getMessage());
                erros++;
            }
        }
        
        if (!aplicados.isEmpty()) {
            Path arquivoUndo = salvarBackupReversao(pasta, aplicados);
            if (arquivoUndo != null) {
                logStream.publicarLog("renomear-arquivos", "Manifesto de reversão salvo no projeto em: " + arquivoUndo);
            }
        }

        // Um único registro para o lote inteiro: registrar por arquivo reescrevia
        // o JSON canônico + broadcast SSE a cada rename (dezenas por segundo).
        telemetriaService.registrarArquivosSanitizados(sucesso);

        logStream.publicarLog("renomear-arquivos", "PROCESSO CONCLUÍDO! Renomeados: " + sucesso + " | Erros: " + erros);
        logStream.publicarLog("renomear-arquivos",
            DuracaoUtil.linhaRelatorioFinal("Renomear Arquivos (aplicação)", inicioMs));
    }

    public void reverterRenomeacao(Path pasta) {
        long inicioMs = System.currentTimeMillis();
        logStream.publicarLog("renomear-arquivos", "Iniciando REVERSÃO de arquivos em: " + pasta);
        Path arquivoUndo = resolverArquivoUndo(pasta);
        
        if (!Files.exists(arquivoUndo)) {
            logStream.publicarLog("renomear-arquivos", "[ERRO] Manifesto de reversão não encontrado no projeto: " + arquivoUndo);
            return;
        }
        
        try {
            OperacaoRenomeacao operacao = objectMapper.readValue(arquivoUndo.toFile(), OperacaoRenomeacao.class);
            int sucesso = 0;
            int erros = 0;
            
            for (OperacaoRenomeacao.ItemRenomeado item : operacao.itens()) {
                Path arquivoAtual = pasta.resolve(item.nomeNovo());
                Path destinoReversao = pasta.resolve(item.nomeOriginal());
                
                if (Files.exists(arquivoAtual)) {
                    try {
                        Files.move(arquivoAtual, destinoReversao, StandardCopyOption.ATOMIC_MOVE);
                        sucesso++;
                        logStream.publicarLog("renomear-arquivos", "[REVERTIDO] " + item.nomeNovo() + " -> " + item.nomeOriginal());
                    } catch (IOException e) {
                        logStream.publicarLog("renomear-arquivos", "[ERRO] Falha ao reverter " + item.nomeNovo() + ": " + e.getMessage());
                        erros++;
                    }
                } else {
                    logStream.publicarLog("renomear-arquivos", "[AVISO] Arquivo atual não encontrado para reverter: " + item.nomeNovo());
                    erros++;
                }
            }
            
            logStream.publicarLog("renomear-arquivos", "Reversão concluída. Sucessos: " + sucesso + " | Erros: " + erros);
            logStream.publicarLog("renomear-arquivos",
                DuracaoUtil.linhaRelatorioFinal("Renomear Arquivos (reversão)", inicioMs));
            
            if (sucesso > 0 && erros == 0) {
                Files.deleteIfExists(arquivoUndo);
                logStream.publicarLog("renomear-arquivos", "Manifesto de reversão removido.");
            }
            
        } catch (IOException e) {
            logStream.publicarLog("renomear-arquivos", "[ERRO] Falha ao ler manifesto de reversão: " + e.getMessage());
        }
    }

    private Path salvarBackupReversao(Path pasta, List<OperacaoRenomeacao.ItemRenomeado> itens) {
        Path arquivoUndo = resolverArquivoUndo(pasta);
        OperacaoRenomeacao op = new OperacaoRenomeacao(
            UUID.randomUUID().toString(),
            Instant.now().toString(),
            pasta.toString(),
            itens
        );
        try {
            Files.createDirectories(arquivoUndo.getParent());
            objectMapper.writeValue(arquivoUndo.toFile(), op);
        } catch (IOException e) {
            logStream.publicarLog("renomear-arquivos", "[ERRO FATAL] Falha ao salvar manifesto de reversão: " + e.getMessage());
            return null;
        }
        return arquivoUndo;
    }

    Path resolverArquivoUndo(Path pasta) {
        return PASTA_UNDO_PROJETO.resolve(PREFIXO_ARQUIVO_UNDO + hashPasta(pasta) + ".json");
    }

    private String hashPasta(Path pasta) {
        String chave = pasta.toAbsolutePath().normalize().toString().toLowerCase(Locale.ROOT);
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(chave.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível para manifesto de reversão.", e);
        }
    }

    private String gerarNomeNovo(String nomeOriginal, String padrao, String extensao, String episodio, boolean renomearComoFilme) {
        if (episodio == null) {
            return renomearComoFilme ? padrao + extensao : nomeOriginal;
        }
        return padrao + " - S01E" + episodio + extensao;
    }

    private String extrairEpisodio(String nome) {
        String semExtensao = nome.replaceAll("(?:\\.[A-Za-z0-9]{1,5})+$", "");
        // Remove tags de release group e metadados de qualidade para não confundir
        // o fallback, mas preserva o trecho " - 01" quando ele existir.
        String semBrackets = semExtensao.replaceAll("\\[.*?\\]", " ").trim();
        
        Matcher m1 = EPISODE_SEPARATOR_PATTERN.matcher(semBrackets);
        if (m1.find()) {
            return String.format("%02d", Integer.parseInt(m1.group(1)));
        }

        Matcher m2 = EPISODE_LABEL_PATTERN.matcher(semBrackets);
        if (m2.find()) {
            return String.format("%02d", Integer.parseInt(m2.group(1)));
        }
        
        // Normaliza separadores antes de remover palavras técnicas: com "_" colado
        // nas palavras, os \b do regex não casam (ex.: "_Track6_" nunca era removido).
        String semRuidoTecnico = semBrackets
            .replaceAll("\\([^\\)]*\\)", " ")
            .replaceAll("[_.-]+", " ")
            .replaceAll("(?i)\\b(1080p|720p|2160p|4k|BD|BDRip|WEBRip|WEB\\s*DL|Dual\\s*Audio|Multi\\s*Audio|10bit|8bit|HEVC|AV1|x264|x265|Track\\s*\\d+|PTBR|PT\\s*BR)\\b", " ")
            .replaceAll("\\s+", " ")
            .trim();

        Matcher fallback = EPISODE_FALLBACK.matcher(semRuidoTecnico);
        String ultimoNumero = null;
        while (fallback.find()) {
            ultimoNumero = fallback.group(1);
        }

        if (ultimoNumero != null) {
            return String.format("%02d", Integer.parseInt(ultimoNumero));
        }
        
        return null;
    }

    private String obterExtensao(String arquivo) {
        int index = arquivo.lastIndexOf('.');
        if (index > 0) {
            return arquivo.substring(index);
        }
        return "";
    }

    private boolean isVideoFile(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase(Locale.ROOT);
        return EXTENSOES_VIDEO.stream().anyMatch(nome::endsWith);
    }

    private boolean isLegendaFile(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase(Locale.ROOT);
        return EXTENSOES_LEGENDA.stream().anyMatch(nome::endsWith);
    }

    private boolean ehConteudoEspecial(String nome) {
        return CONTEUDO_ESPECIAL_PATTERN.matcher(nome).find();
    }
}
