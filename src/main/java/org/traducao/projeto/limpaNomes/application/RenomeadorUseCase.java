package org.traducao.projeto.limpaNomes.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traducao.projeto.limpaNomes.domain.OperacaoRenomeacao;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.traducao.presentation.web.LogStreamService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@ApplicationScoped
public class RenomeadorUseCase {
    private static final Logger log = LoggerFactory.getLogger(RenomeadorUseCase.class);
    private static final String ARQUIVO_UNDO = ".kronos_undo_renomeacao.json";
    
    // Regex para pegar o episódio de trackers.
    // Ex: "[SubsPlease] Nome Anime - 01 (1080p).mkv" -> 01
    // Ex: "Anime - 02.mkv" -> 02
    // Ex: "Anime Ep 03.mkv" -> 03
    // Ex: "Anime E04.mkv" -> 04
    private static final Pattern EPISODE_PATTERN = Pattern.compile("(?:-|Ep|Epis[oó]dio|E|Episode)\\s*(\\d{1,4})", Pattern.CASE_INSENSITIVE);
    
    // Regex fallback se não tiver separador claro, pega o primeiro número isolado
    private static final Pattern EPISODE_FALLBACK = Pattern.compile("(?<=\\s|^)(\\d{2,4})(?=\\s|v\\d|\\.|$)");

    @Inject
    ObjectMapper objectMapper;

    @Inject
    TelemetriaService telemetriaService;

    @Inject
    LogStreamService logStream;

    public List<OperacaoRenomeacao.ItemRenomeado> simularRenomeacao(Path pasta, String nomePadrao) {
        logStream.publicarLog("limpa-nome", "Iniciando simulação (Dry-Run) em: " + pasta);
        List<OperacaoRenomeacao.ItemRenomeado> itens = new ArrayList<>();
        
        if (!Files.exists(pasta) || !Files.isDirectory(pasta)) {
            logStream.publicarLog("limpa-nome", "O caminho fornecido não existe ou não é um diretório.");
            return itens;
        }

        try (Stream<Path> stream = Files.list(pasta)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> !p.getFileName().toString().equals(ARQUIVO_UNDO))
                  .forEach(arquivo -> {
                      String nomeOriginal = arquivo.getFileName().toString();
                      String extensao = obterExtensao(nomeOriginal);
                      String nomeNovo = gerarNomeNovo(nomeOriginal, nomePadrao, extensao);
                      
                      if (!nomeOriginal.equals(nomeNovo)) {
                          itens.add(new OperacaoRenomeacao.ItemRenomeado(nomeOriginal, nomeNovo));
                          logStream.publicarLog("limpa-nome", "[DRY-RUN] " + nomeOriginal + " \u001b[33m->\u001b[0m " + nomeNovo);
                      } else {
                          logStream.publicarLog("limpa-nome", "[IGNORADO] " + nomeOriginal + " já está no padrão.");
                      }
                  });
            logStream.publicarLog("limpa-nome", "Simulação concluída. " + itens.size() + " arquivos seriam renomeados.");
        } catch (IOException e) {
            logStream.publicarLog("limpa-nome", "Erro ao acessar a pasta: " + e.getMessage());
        }
        
        return itens;
    }

    public void aplicarRenomeacao(Path pasta, String nomePadrao) {
        logStream.publicarLog("limpa-nome", "Iniciando APLICAÇÃO da renomeação em: " + pasta);
        List<OperacaoRenomeacao.ItemRenomeado> simulação = simularRenomeacao(pasta, nomePadrao);
        
        if (simulação.isEmpty()) {
            logStream.publicarLog("limpa-nome", "Nenhum arquivo precisa ser renomeado.");
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
                    logStream.publicarLog("limpa-nome", "[ERRO] Arquivo de destino já existe: " + destino.getFileName());
                    erros++;
                    continue;
                }
                
                Files.move(origem, destino, StandardCopyOption.ATOMIC_MOVE);
                aplicados.add(item);
                sucesso++;
                logStream.publicarLog("limpa-nome", "[OK] " + item.nomeOriginal() + " renomeado para " + item.nomeNovo());
                
                // Incrementa a telemetria nova
                telemetriaService.registrarArquivoSanitizado();
            } catch (Exception e) {
                logStream.publicarLog("limpa-nome", "[ERRO] Falha ao renomear " + item.nomeOriginal() + ": " + e.getMessage());
                erros++;
            }
        }
        
        if (!aplicados.isEmpty()) {
            salvarBackupReversao(pasta, aplicados);
            logStream.publicarLog("limpa-nome", "Arquivo de reversão salvo em: " + ARQUIVO_UNDO);
        }
        
        logStream.publicarLog("limpa-nome", "PROCESSO CONCLUÍDO! Renomeados: " + sucesso + " | Erros: " + erros);
    }

    public void reverterRenomeacao(Path pasta) {
        logStream.publicarLog("limpa-nome", "Iniciando REVERSÃO de arquivos em: " + pasta);
        Path arquivoUndo = pasta.resolve(ARQUIVO_UNDO);
        
        if (!Files.exists(arquivoUndo)) {
            logStream.publicarLog("limpa-nome", "[ERRO] Arquivo de backup não encontrado na pasta: " + ARQUIVO_UNDO);
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
                        logStream.publicarLog("limpa-nome", "[REVERTIDO] " + item.nomeNovo() + " -> " + item.nomeOriginal());
                    } catch (IOException e) {
                        logStream.publicarLog("limpa-nome", "[ERRO] Falha ao reverter " + item.nomeNovo() + ": " + e.getMessage());
                        erros++;
                    }
                } else {
                    logStream.publicarLog("limpa-nome", "[AVISO] Arquivo atual não encontrado para reverter: " + item.nomeNovo());
                    erros++;
                }
            }
            
            logStream.publicarLog("limpa-nome", "Reversão concluída. Sucessos: " + sucesso + " | Erros: " + erros);
            
            if (sucesso > 0 && erros == 0) {
                Files.deleteIfExists(arquivoUndo);
                logStream.publicarLog("limpa-nome", "Arquivo de backup removido.");
            }
            
        } catch (IOException e) {
            logStream.publicarLog("limpa-nome", "[ERRO] Falha ao ler arquivo de backup: " + e.getMessage());
        }
    }

    private void salvarBackupReversao(Path pasta, List<OperacaoRenomeacao.ItemRenomeado> itens) {
        Path arquivoUndo = pasta.resolve(ARQUIVO_UNDO);
        OperacaoRenomeacao op = new OperacaoRenomeacao(
            UUID.randomUUID().toString(),
            Instant.now().toString(),
            pasta.toString(),
            itens
        );
        try {
            objectMapper.writeValue(arquivoUndo.toFile(), op);
        } catch (IOException e) {
            logStream.publicarLog("limpa-nome", "[ERRO FATAL] Falha ao salvar backup de reversão: " + e.getMessage());
        }
    }

    private String gerarNomeNovo(String nomeOriginal, String padrao, String extensao) {
        String episodio = extrairEpisodio(nomeOriginal);
        if (episodio == null) {
            return nomeOriginal; // Não encontrou episódio, mantém o nome original
        }
        return padrao + " - S01E" + episodio + extensao;
    }

    private String extrairEpisodio(String nome) {
        // Remover tags de brackets pra não confundir
        String semBrackets = nome.replaceAll("\\[.*?\\]", "").trim();
        
        Matcher m1 = EPISODE_PATTERN.matcher(semBrackets);
        if (m1.find()) {
            return String.format("%02d", Integer.parseInt(m1.group(1)));
        }
        
        Matcher m2 = EPISODE_FALLBACK.matcher(semBrackets);
        if (m2.find()) {
            return String.format("%02d", Integer.parseInt(m2.group(1)));
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
}
