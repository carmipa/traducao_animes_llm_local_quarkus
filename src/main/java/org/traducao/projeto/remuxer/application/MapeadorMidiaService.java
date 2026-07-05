package org.traducao.projeto.remuxer.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.remuxer.domain.RemuxTarefa;
import org.traducao.projeto.remuxer.domain.RemuxerException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class MapeadorMidiaService {
    
    private static final Logger log = LoggerFactory.getLogger(MapeadorMidiaService.class);

    public List<RemuxTarefa> construirFilaProcessamento(Path pastaVideos, Path pastaLegendas, Path pastaSaida) {
        log.debug("Escaneando diretório de vídeos: {}", pastaVideos);
        List<RemuxTarefa> fila = new ArrayList<>();

        if (!Files.exists(pastaVideos) || !Files.exists(pastaLegendas)) {
            throw new RemuxerException("Diretórios de origem não encontrados.");
        }

        try (Stream<Path> stream = Files.list(pastaVideos)) {
            List<Path> mkvs = stream
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().toLowerCase().endsWith(".mkv"))
                .sorted()
                .toList();
            
            List<Path> todasLegendas;
            try (Stream<Path> streamLegendas = Files.list(pastaLegendas)) {
                todasLegendas = new ArrayList<>(streamLegendas
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String f = p.getFileName().toString().toLowerCase();
                        return f.endsWith(".ass") || f.endsWith(".srt");
                    })
                    .toList());
            } catch (IOException e) {
                throw new RemuxerException("Erro ao listar arquivos de legenda", e);
            }

            for (Path mkv : mkvs) {
                String nomeArq = mkv.getFileName().toString();
                String nomeBase = nomeArq.replaceFirst("[.][^.]+$", "");
                String nomeLimpoBase = nomeBase.replace("_PTBR", "").replace("_ENG", "").replace("_PT-BR", "");

                String tagEpisodio = extrairTagEpisodio(nomeArq);

                List<Path> candidatas = todasLegendas.stream()
                    .filter(p -> {
                        String f = p.getFileName().toString();
                        if (f.startsWith(nomeBase) || f.startsWith(nomeLimpoBase)) {
                            return true;
                        }
                        if (tagEpisodio != null && !tagEpisodio.isBlank()) {
                            String tagLeg = extrairTagEpisodio(f);
                            return tagEpisodio.equalsIgnoreCase(tagLeg);
                        }
                        return false;
                    })
                    .toList();

                // Priorizar as que possuem PT-BR ou PTBR no nome, depois as outras
                Path legendaEncontrada = candidatas.stream()
                    .filter(p -> p.getFileName().toString().toUpperCase().contains("PT-BR") ||
                                 p.getFileName().toString().toUpperCase().contains("PTBR"))
                    .findFirst()
                    .orElse(candidatas.isEmpty() ? null : candidatas.get(0));

                // Filme/arquivo avulso: 1 único mkv e 1 única legenda na pasta, mesmo com nomes
                // completamente diferentes (releases de fansubs distintos) - pareia direto.
                if (legendaEncontrada == null && mkvs.size() == 1 && todasLegendas.size() == 1) {
                    legendaEncontrada = todasLegendas.get(0);
                    log.info("Pareamento por arquivo único (filme): {} -> {}", nomeArq, legendaEncontrada.getFileName());
                }

                if (legendaEncontrada != null) {
                    // Remove do pool para que outro vídeo não seja pareado com a mesma legenda
                    // (ex.: dois episódios cujos nomes colidem na mesma tag/prefixo).
                    todasLegendas.remove(legendaEncontrada);
                    String nomeSaida = nomeLimpoBase.endsWith("_PTBR") ? nomeLimpoBase + ".mkv" : nomeLimpoBase + "_PTBR.mkv";
                    Path caminhoSaida = pastaSaida.resolve(nomeSaida);
                    fila.add(new RemuxTarefa(mkv.getFileName().toString(), mkv, legendaEncontrada, caminhoSaida));
                    log.info("Pareado com sucesso: {} -> {}", mkv.getFileName(), legendaEncontrada.getFileName());
                } else {
                    log.warn("Legenda ausente para: {}", mkv.getFileName());
                }
            }
        } catch (IOException e) {
            throw new RemuxerException("Erro ao listar arquivos mkv", e);
        }

        return fila;
    }

    private String extrairTagEpisodio(String nome) {
        if (nome == null) {
            return null;
        }

        // 1. Tenta padrão ocidental completo: S01E02 ou E02 (ex: S01E02_, E02_)
        Pattern p1 = Pattern.compile("(?i)(?:S\\d{1,2})?E(\\d{1,3})(?!\\d)");
        Matcher m1 = p1.matcher(nome);
        if (m1.find()) {
            return padLeft(m1.group(1));
        }

        // 2. Tenta padrão abreviado de anime: Ep02, Eps02, Episode 02, Ep.02
        Pattern p2 = Pattern.compile("(?i)\\b(?:ep|eps|episode|ep\\.|eps\\.)\\s*?(\\d{1,3})(?!\\d)");
        Matcher m2 = p2.matcher(nome);
        if (m2.find()) {
            return padLeft(m2.group(1));
        }

        // 3. Tenta padrão de separador de anime: " - 02" ou "_-_02" ou "_- 02" ou " -_02"
        Pattern p3 = Pattern.compile("(?i)(?:\\s+-\\s+|_- _|\\s*-\\s*|_-_|_[\\s]*-[-_\\s]*)\\s*?(\\d{1,3})(?!\\d)");
        Matcher m3 = p3.matcher(nome);
        if (m3.find()) {
            return padLeft(m3.group(1));
        }

        // 4. Caso não encontre nenhum dos acima, tenta achar um número solto de 2 a 3 dígitos no nome
        // (evitando pegar o "86" do nome do anime pegando o último número de 2 a 3 dígitos).
        Pattern p4 = Pattern.compile("(?<!\\d)(\\d{2,3})(?!\\d)");
        Matcher m4 = p4.matcher(nome);
        String ultimoNumero = null;
        while (m4.find()) {
            ultimoNumero = m4.group(1);
        }
        if (ultimoNumero != null) {
            return padLeft(ultimoNumero);
        }

        return null;
    }

    private String padLeft(String num) {
        if (num == null) return null;
        if (num.length() == 1) {
            return "0" + num;
        }
        return num;
    }
}
