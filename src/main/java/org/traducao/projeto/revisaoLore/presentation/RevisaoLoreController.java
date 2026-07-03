package org.traducao.projeto.revisaoLore.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.traducao.projeto.revisaoLore.application.RevisarLoreUseCase;
import org.traducao.projeto.revisaoLore.domain.ResultadoRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.exceptions.RevisaoLoreException;
import org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto;
import org.traducao.projeto.traducao.presentation.web.LogStreamService;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api")
public class RevisaoLoreController {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final RevisarLoreUseCase revisarLoreUseCase;
    private final GerenciadorContexto gerenciadorContexto;
    private final LogStreamService logStreamService;

    public RevisaoLoreController(
        RevisarLoreUseCase revisarLoreUseCase,
        GerenciadorContexto gerenciadorContexto,
        LogStreamService logStreamService
    ) {
        this.revisarLoreUseCase = revisarLoreUseCase;
        this.gerenciadorContexto = gerenciadorContexto;
        this.logStreamService = logStreamService;
    }

    public record RevisaoLoreRequest(
        String diretorioOriginal,
        String diretorioTraduzido,
        String contextoId,
        boolean revisarTodasFalas
    ) {}

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
        if (!gerenciadorContexto.existeContexto(req.contextoId())) {
            return ResponseEntity.badRequest().body(Map.of(
                "erro", "Contexto desconhecido: \"" + req.contextoId()
                    + "\". Recarregue a pagina e selecione uma obra valida."));
        }

        Path pastaOriginal = Path.of(req.diretorioOriginal().trim());
        Path pastaTraduzida = Path.of(req.diretorioTraduzido().trim());
        boolean revisarTodas = req.revisarTodasFalas();

        executor.submit(() -> {
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
                if (resultado.caminhoLogSessao() != null) {
                    System.out.println("\u001B[36m  • Log da sessao        : " + resultado.caminhoLogSessao() + "\u001B[0m");
                }
                if (resultado.caminhoRelatorio() != null) {
                    System.out.println("\u001B[36m  • Relatorio/telemetria : " + resultado.caminhoRelatorio() + "\u001B[0m");
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
