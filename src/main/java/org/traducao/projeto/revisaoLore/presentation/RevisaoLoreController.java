package org.traducao.projeto.revisaoLore.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.traducao.projeto.revisaoLore.application.GerenciadorPromptRevisaoLore;
import org.traducao.projeto.revisaoLore.application.RevisarLoreUseCase;
import org.traducao.projeto.revisaoLore.domain.ResultadoRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.exceptions.RevisaoLoreException;
import org.traducao.projeto.core.execucao.FilaExecucaoPipeline;
import org.traducao.projeto.traducao.presentation.web.LogStreamService;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RevisaoLoreController {

    // Fila única compartilhada do pipeline: impede que a revisão de lore rode
    // em paralelo com uma tradução/correção e troque o contexto LLM global no
    // meio do outro job (ver FilaExecucaoPipeline).
    private final FilaExecucaoPipeline filaExecucao;
    private final RevisarLoreUseCase revisarLoreUseCase;
    private final GerenciadorPromptRevisaoLore gerenciadorPromptRevisaoLore;
    private final LogStreamService logStreamService;

    public RevisaoLoreController(
        FilaExecucaoPipeline filaExecucao,
        RevisarLoreUseCase revisarLoreUseCase,
        GerenciadorPromptRevisaoLore gerenciadorPromptRevisaoLore,
        LogStreamService logStreamService
    ) {
        this.filaExecucao = filaExecucao;
        this.revisarLoreUseCase = revisarLoreUseCase;
        this.gerenciadorPromptRevisaoLore = gerenciadorPromptRevisaoLore;
        this.logStreamService = logStreamService;
    }

    public record RevisaoLoreRequest(
        String diretorioOriginal,
        String diretorioTraduzido,
        String contextoId,
        boolean revisarTodasFalas
    ) {}

    public record RevisaoLoreContextoResponse(String id, String nome, String termoMetadata) {}

    @GetMapping("/revisao-lore/contextos")
    public ResponseEntity<List<RevisaoLoreContextoResponse>> listarPromptsRevisaoLore() {
        List<RevisaoLoreContextoResponse> lista = gerenciadorPromptRevisaoLore.getProvedores().stream()
            .map(p -> new RevisaoLoreContextoResponse(
                p.getId(),
                p.getNomeExibicao(),
                limparTermoMetadata(p.getNomeExibicao())))
            .toList();
        return ResponseEntity.ok(lista);
    }

    private String limparTermoMetadata(String nomeExibicao) {
        if (nomeExibicao == null) {
            return "";
        }
        return nomeExibicao
            .replaceAll("(?i)\\s*-\\s*Revis[aã]o\\s+de\\s+Lore\\s*$", "")
            .replaceAll("(?i)\\s+Revis[aã]o\\s+de\\s+Lore\\s*$", "")
            .replaceAll("\\s+", " ")
            .trim();
    }

    @PostMapping("/revisar-lore")
    public ResponseEntity<Map<String, Object>> iniciarRevisaoLore(@RequestBody RevisaoLoreRequest req) {
        if (req.diretorioOriginal() == null || req.diretorioOriginal().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "erro", "Pasta com legendas originais em ingles nao informada."));
        }
        if (req.diretorioTraduzido() == null || req.diretorioTraduzido().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "erro", "Pasta com legendas traduzidas em portugues nao informada."));
        }
        if (req.contextoId() == null || req.contextoId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "erro", "Selecione a obra/contexto no menu para carregar a lore oficial da revisao."));
        }
        if (!gerenciadorPromptRevisaoLore.existePrompt(req.contextoId())) {
            return ResponseEntity.badRequest().body(Map.of(
                "erro", "Prompt de revisao de lore desconhecido: \"" + req.contextoId()
                    + "\". Recarregue a pagina e selecione uma obra valida."));
        }

        Path pastaOriginal = Path.of(req.diretorioOriginal().trim());
        Path pastaTraduzida = Path.of(req.diretorioTraduzido().trim());
        boolean revisarTodas = req.revisarTodasFalas();

        filaExecucao.submeter(() -> {
            logStreamService.definirCanalAtual("revisao-lore");
            try {
                ResultadoRevisaoLore resultado = revisarLoreUseCase.executar(
                    pastaOriginal, pastaTraduzida, req.contextoId(), revisarTodas);
                System.out.println("\n\u001B[32m========================================================================\u001B[0m");
                System.out.println("\u001B[32m  [SUCESSO] REVISAO DE LORE FINALIZADA!\u001B[0m");
                System.out.println("\u001B[32m========================================================================\u001B[0m");
                System.out.println("\u001B[36m  • Arquivos analisados  : " + resultado.arquivosAnalisados() + "\u001B[0m");
                System.out.println("\u001B[36m  • Arquivos alterados   : " + resultado.arquivosAlterados() + "\u001B[0m");
                System.out.println("\u001B[32m  • Falas corrigidas     : " + resultado.falasCorrigidas() + "\u001B[0m");
                if (resultado.caminhoRelatorioJson() != null) {
                    System.out.println("\u001B[36m  • Relatorio JSON       : " + resultado.caminhoRelatorioJson() + "\u001B[0m");
                }
                System.out.println("\u001B[32m========================================================================\n\u001B[0m");
            } catch (RevisaoLoreException e) {
                System.out.println("\u001B[31m[ERRO] Revisao de lore: " + e.getMessage() + "\u001B[0m");
            } catch (Exception e) {
                System.out.println("\u001B[31m[ERRO] Falha inesperada na revisao de lore: " + e.getMessage() + "\u001B[0m");
            }
        });

        return ResponseEntity.ok(Map.of(
            "mensagem", "Revisao de lore iniciada no servidor. Acompanhe os logs em tempo real."));
    }
}
