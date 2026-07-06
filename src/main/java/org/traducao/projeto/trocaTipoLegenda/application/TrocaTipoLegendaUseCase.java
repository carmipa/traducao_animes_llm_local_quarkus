package org.traducao.projeto.trocaTipoLegenda.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda;
import org.traducao.projeto.traducao.infrastructure.legenda.EscritorLegendaAss;
import org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaAss;
import org.traducao.projeto.traducao.presentation.ui.AnsiCores;
import org.traducao.projeto.trocaTipoLegenda.domain.AuditoriaFonteInfo;
import org.traducao.projeto.trocaTipoLegenda.domain.AuditoriaLegendaResultado;
import org.traducao.projeto.trocaTipoLegenda.domain.EntradaAuditoriaTrocaFonte;
import org.traducao.projeto.trocaTipoLegenda.domain.ResultadoGeralAuditoria;
import org.traducao.projeto.trocaTipoLegenda.domain.ResultadoTrocaFonte;
import org.traducao.projeto.trocaTipoLegenda.domain.exceptions.TrocaTipoLegendaException;
import org.traducao.projeto.trocaTipoLegenda.infrastructure.TrocaTipoLegendaAuditoriaCache;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class TrocaTipoLegendaUseCase {

    private static final Logger log = LoggerFactory.getLogger(TrocaTipoLegendaUseCase.class);
    private static final DateTimeFormatter UTC_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private static final DateTimeFormatter TIMESTAMP_DIR = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final LeitorLegendaAss leitor;
    private final EscritorLegendaAss escritor;
    private final AuditoriaFontesService auditoriaService;
    private final TelemetriaService telemetriaService;
    private final TrocaTipoLegendaAuditoriaCache auditoriaCache;
    private final ObjectMapper objectMapper;

    private static final class SessaoTroca {
        final long inicioMs = System.currentTimeMillis();
        final List<String> logsSessao = new ArrayList<>();

        void out(String msg) {
            String limpa = msg.replaceAll("\\u001B\\[[0-9;]*m", "").replaceAll("\\033\\[[0-9;]*m", "");
            long decorridoMs = Math.max(0, System.currentTimeMillis() - inicioMs);
            String prefixo = "[UTC " + UTC_FORMATTER.format(Instant.now()) + " | +" + formatarDuracao(decorridoMs) + "]";
            String logCompleto = prefixo + " " + limpa;
            
            System.out.println(msg);
            log.info(logCompleto);
            logsSessao.add(logCompleto);
        }

        private String formatarDuracao(long ms) {
            long s = ms / 1000;
            long m = s / 60;
            s = s % 60;
            return String.format("%02d:%02d", m, s);
        }
    }

    public TrocaTipoLegendaUseCase(
        LeitorLegendaAss leitor,
        EscritorLegendaAss escritor,
        AuditoriaFontesService auditoriaService,
        TelemetriaService telemetriaService,
        TrocaTipoLegendaAuditoriaCache auditoriaCache
    ) {
        this.leitor = leitor;
        this.escritor = escritor;
        this.auditoriaService = auditoriaService;
        this.telemetriaService = telemetriaService;
        this.auditoriaCache = auditoriaCache;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public ResultadoGeralAuditoria escanear(Path diretorio) {
        validarDiretorio(diretorio);

        List<Path> arquivos = listarLegendas(diretorio);
        List<AuditoriaLegendaResultado> resultados = new ArrayList<>();
        int totalComProblemas = 0;

        for (Path arq : arquivos) {
            try {
                DocumentoLegenda doc = leitor.ler(arq);
                List<AuditoriaFonteInfo> fontes = auditoriaService.analisarCabecalho(doc.cabecalho());
                boolean temProblemas = fontes.stream().anyMatch(AuditoriaFonteInfo::problematica);
                
                if (temProblemas) {
                    totalComProblemas++;
                }

                resultados.add(new AuditoriaLegendaResultado(
                    arq.getFileName().toString(),
                    fontes,
                    temProblemas
                ));
            } catch (Exception e) {
                log.warn("Erro ao auditar fontes do arquivo: " + arq.getFileName(), e);
                resultados.add(new AuditoriaLegendaResultado(
                    arq.getFileName().toString(),
                    List.of(new AuditoriaFonteInfo("ERRO_LEITURA", "N/A", "N/A", false)),
                    false
                ));
            }
        }

        return new ResultadoGeralAuditoria(resultados, arquivos.size(), totalComProblemas);
    }

    public ResultadoTrocaFonte aplicar(Path diretorio) {
        SessaoTroca sessao = new SessaoTroca();
        validarDiretorio(diretorio);

        sessao.out(AnsiCores.CYAN + "\n=== Iniciando Substituição em Lote de Fontes ASS ===" + AnsiCores.RESET);
        sessao.out("Pasta alvo: " + diretorio.toAbsolutePath());

        List<Path> arquivos = listarLegendas(diretorio);
        if (arquivos.isEmpty()) {
            sessao.out(AnsiCores.YELLOW + "[AVISO] Nenhum arquivo .ass/.ssa encontrado." + AnsiCores.RESET);
            return new ResultadoTrocaFonte(0, 0, 0, LocalDateTime.now().toString(), "N/A", null);
        }

        // Criar pasta de backup automático
        String timestamp = TIMESTAMP_DIR.format(LocalDateTime.now());
        Path pastaBackup = Path.of("backups", "troca_tipo_legenda_" + timestamp).toAbsolutePath();
        try {
            Files.createDirectories(pastaBackup);
            sessao.out("Diretório de backup criado com sucesso: " + pastaBackup);
        } catch (IOException e) {
            throw new TrocaTipoLegendaException("Falha ao criar diretório de backup: " + pastaBackup, e);
        }

        int totalAlterados = 0;
        int totalSubstituicoes = 0;
        Map<String, String> mapaProblematicas = auditoriaService.getFontesProblematicas();

        for (Path arq : arquivos) {
            if (Thread.currentThread().isInterrupted()) {
                sessao.out(AnsiCores.YELLOW + "[AVISO] Execução interrompida cooperativamente." + AnsiCores.RESET);
                break;
            }

            try {
                DocumentoLegenda doc = leitor.ler(arq);
                List<AuditoriaFonteInfo> fontes = auditoriaService.analisarCabecalho(doc.cabecalho());
                boolean temProblemas = fontes.stream().anyMatch(AuditoriaFonteInfo::problematica);

                if (!temProblemas) {
                    sessao.out("Arquivo " + arq.getFileName() + " [OK] - Sem fontes legadas problemáticas. Pulando.");
                    continue;
                }

                // Cria o backup
                Path arqBackup = pastaBackup.resolve(arq.getFileName());
                Files.copy(arq, arqBackup, StandardCopyOption.REPLACE_EXISTING);

                // Executa a substituição no cabeçalho
                String cabecalhoOriginal = doc.cabecalho();
                String cabecalhoNovo = cabecalhoOriginal;
                int substituicoesNoArquivo = 0;

                // Registra o log granular para cada estilo de fonte problemática
                for (AuditoriaFonteInfo fonteInfo : fontes) {
                    if (fonteInfo.problematica()) {
                        String fonteLegada = fonteInfo.fonteAtual();
                        String fonteUnicode = fonteInfo.fonteSugerida();

                        if (cabecalhoNovo.contains(fonteLegada)) {
                            cabecalhoNovo = cabecalhoNovo.replace(fonteLegada, fonteUnicode);
                            substituicoesNoArquivo++;
                            
                            // Registrar no cache granular de auditoria (JSONL append-only)
                            EntradaAuditoriaTrocaFonte entrada = new EntradaAuditoriaTrocaFonte(
                                Instant.now().toString(),
                                arq.getFileName().toString(),
                                fonteInfo.estilo(),
                                fonteLegada,
                                fonteUnicode,
                                pastaBackup.toString(),
                                "SUBSTITUIDO"
                            );
                            auditoriaCache.registrar(entrada);

                            sessao.out("  -> No arquivo: " + arq.getFileName() + " estilo [" + fonteInfo.estilo() + "] substituído: " + fonteLegada + " -> " + fonteUnicode);
                        }
                    }
                }

                if (substituicoesNoArquivo > 0) {
                    DocumentoLegenda novoDoc = new DocumentoLegenda(
                        cabecalhoNovo,
                        doc.eventos(),
                        doc.quebraDeLinha(),
                        doc.comBom()
                    );
                    
                    // Escreve com o escritor ASS
                    escritor.escrever(arq, novoDoc);
                    totalAlterados++;
                    totalSubstituicoes += substituicoesNoArquivo;
                    sessao.out(AnsiCores.GREEN + "  [SUCESSO] Arquivo " + arq.getFileName() + " atualizado!" + AnsiCores.RESET);
                }
            } catch (Exception e) {
                sessao.out(AnsiCores.RED + "  [ERRO] Falha ao processar arquivo: " + arq.getFileName() + ". Erro: " + e.getMessage() + AnsiCores.RESET);
                log.error("Erro no processamento do arquivo " + arq, e);
                
                // Registra falha na auditoria
                EntradaAuditoriaTrocaFonte entradaErro = new EntradaAuditoriaTrocaFonte(
                    Instant.now().toString(),
                    arq.getFileName().toString(),
                    "N/A",
                    "N/A",
                    "N/A",
                    pastaBackup.toString(),
                    "FALHA: " + e.getMessage()
                );
                auditoriaCache.registrar(entradaErro);
            }
        }

        sessao.out(AnsiCores.GREEN + "\n========================================================================" + AnsiCores.RESET);
        sessao.out(AnsiCores.GREEN + "  [SUCESSO] OPERAÇÃO DE SUBISTITUIÇÃO DE FONTES FINALIZADA!" + AnsiCores.RESET);
        sessao.out(AnsiCores.GREEN + "========================================================================" + AnsiCores.RESET);
        sessao.out("  • Arquivos analisados  : " + arquivos.size());
        sessao.out("  • Arquivos alterados   : " + totalAlterados);
        sessao.out("  • Substituições feitas : " + totalSubstituicoes);
        sessao.out("  • Pasta de Backup      : " + pastaBackup);
        sessao.out(AnsiCores.GREEN + "========================================================================" + AnsiCores.RESET);

        // Salvar relatórios JSON e Markdown
        Path pastaRelatorios = TelemetriaService.resolverPastaRelatorios(diretorio);
        Path caminhoJson = pastaRelatorios.resolve("troca_fontes_" + timestamp + ".json");
        Path caminhoMd = pastaRelatorios.resolve("troca_fontes_" + timestamp + ".md");

        ResultadoTrocaFonte resultado = new ResultadoTrocaFonte(
            arquivos.size(),
            totalAlterados,
            totalSubstituicoes,
            LocalDateTime.now().toString(),
            pastaBackup.toString(),
            caminhoJson.toAbsolutePath().toString()
        );

        try {
            Files.createDirectories(pastaRelatorios);
            // Salvar JSON
            objectMapper.writeValue(caminhoJson.toFile(), resultado);
            
            // Salvar Markdown
            String markdown = gerarRelatorioMarkdown(resultado, sessao.logsSessao);
            Files.writeString(caminhoMd, markdown, StandardCharsets.UTF_8);
            
            sessao.out("Relatórios persistidos em: " + pastaRelatorios);
        } catch (IOException e) {
            sessao.out(AnsiCores.YELLOW + "[AVISO] Falha ao gravar relatórios finais em disco: " + e.getMessage() + AnsiCores.RESET);
        }

        // Registrar na telemetria
        OperacaoTelemetria op = new OperacaoTelemetria(
            "Troca de Fontes ASS",
            "Diretório: " + diretorio.getFileName(),
            System.currentTimeMillis() - sessao.inicioMs,
            arquivos.size(),
            totalAlterados,
            totalSubstituicoes,
            Instant.now().toString()
        );
        telemetriaService.registrarOperacao(op);

        return resultado;
    }

    private void validarDiretorio(Path dir) {
        if (dir == null) {
            throw new TrocaTipoLegendaException("Diretório de legendas não pode ser nulo.");
        }
        if (!Files.exists(dir)) {
            throw new TrocaTipoLegendaException("Diretório de legendas não existe: " + dir.toAbsolutePath());
        }
        if (!Files.isDirectory(dir)) {
            throw new TrocaTipoLegendaException("O caminho fornecido não é uma pasta: " + dir.toAbsolutePath());
        }
    }

    private List<Path> listarLegendas(Path dir) {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                .filter(p -> Files.isRegularFile(p))
                .filter(p -> p.toString().toLowerCase().endsWith(".ass") || p.toString().toLowerCase().endsWith(".ssa"))
                .toList();
        } catch (IOException e) {
            throw new TrocaTipoLegendaException("Falha ao listar arquivos de legenda na pasta: " + dir, e);
        }
    }

    private String gerarRelatorioMarkdown(ResultadoTrocaFonte res, List<String> logs) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Relatório de Auditoria e Troca de Fontes ASS\n\n");
        sb.append("- **Data/Hora**: ").append(res.dataHora()).append("\n");
        sb.append("- **Total de arquivos analisados**: ").append(res.totalAnalisados()).append("\n");
        sb.append("- **Total de arquivos alterados**: ").append(res.totalAlterados()).append("\n");
        sb.append("- **Total de substituições de fonte feitas**: ").append(res.totalSubstituicoes()).append("\n");
        sb.append("- **Pasta de backup**: `").append(res.pastaBackup()).append("`\n\n");
        
        sb.append("## Logs de Execução\n");
        sb.append("```text\n");
        for (String logLine : logs) {
            sb.append(logLine).append("\n");
        }
        sb.append("```\n");
        return sb.toString();
    }
}
