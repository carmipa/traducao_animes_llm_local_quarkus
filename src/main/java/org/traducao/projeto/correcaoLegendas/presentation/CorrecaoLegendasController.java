package org.traducao.projeto.correcaoLegendas.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.traducao.projeto.correcaoLegendas.application.CorrigirLegendasUseCase;
import org.traducao.projeto.correcaoLegendas.domain.ResultadoCorrecaoLegendas;
import org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto;
import org.traducao.projeto.traducao.presentation.web.LogStreamService;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CorrecaoLegendasController {

    private final CorrigirLegendasUseCase corrigirLegendasUseCase;
    private final GerenciadorContexto gerenciadorContexto;
    private final LogStreamService logStreamService;

    public CorrecaoLegendasController(
        CorrigirLegendasUseCase corrigirLegendasUseCase,
        GerenciadorContexto gerenciadorContexto,
        LogStreamService logStreamService
    ) {
        this.corrigirLegendasUseCase = corrigirLegendasUseCase;
        this.gerenciadorContexto = gerenciadorContexto;
        this.logStreamService = logStreamService;
    }

    @PostMapping("/correcao-legendas")
    public ResponseEntity<Map<String, Object>> iniciarCorrecaoLegendas(@RequestBody Map<String, String> payload) {
        return executarCorrecaoLegendas(payload);
    }

    @PostMapping("/cura-tags")
    public ResponseEntity<Map<String, Object>> iniciarCuraTagsLegado(@RequestBody Map<String, String> payload) {
        return executarCorrecaoLegendas(payload);
    }

    private ResponseEntity<Map<String, Object>> executarCorrecaoLegendas(Map<String, String> payload) {
        logStreamService.definirCanalAtual("correcao-legendas");
        String diretorioOriginal = payload.get("diretorioOriginal");
        String diretorioTraduzido = payload.get("diretorioTraduzido");
        if (diretorioOriginal == null || diretorioOriginal.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Pasta com as legendas originais/referência não informada."));
        }
        if (diretorioTraduzido == null || diretorioTraduzido.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Pasta com as legendas traduzidas (PT-BR) não informada."));
        }

        String contextoId = payload.get("contextoId");
        if (contextoId != null && !contextoId.isBlank() && !gerenciadorContexto.existeContexto(contextoId)) {
            return ResponseEntity.badRequest().body(Map.of(
                "erro", "Contexto de tradução desconhecido: \"" + contextoId + "\". Recarregue a página e selecione um contexto válido."));
        }

        Path pastaOriginal;
        Path pastaTraduzida;
        try {
            pastaOriginal = Paths.get(diretorioOriginal.trim());
            pastaTraduzida = Paths.get(diretorioTraduzido.trim());
        } catch (InvalidPathException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Caminho de pasta inválido: " + e.getMessage()));
        }

        try {
            ResultadoCorrecaoLegendas resultado = corrigirLegendasUseCase.corrigirPasta(pastaOriginal, pastaTraduzida, contextoId);

            String mensagem = String.format(
                "Correção de legendas finalizada: %d arquivo(s) corrigido(s), %d fala(s) corrigida(s) via LLM, %d já perfeito(s), %d sem tradução pareada, %d fala(s) sem tradução (vazia), %d erro(s) de %d arquivo(s).",
                resultado.curados(), resultado.corrigidosLlm(), resultado.semAlteracao(), resultado.semPar(),
                resultado.traducaoAusente(), resultado.totalErros(), resultado.totalArquivosAnalisados());

            if (resultado.teveErros()) {
                return ResponseEntity.internalServerError().body(Map.of(
                    "erro", mensagem,
                    "detalhesErros", resultado.erros(),
                    "relatorioJson", valorOuVazio(resultado.relatorioJson())
                ));
            }
            return ResponseEntity.ok(Map.of(
                "mensagem", mensagem,
                "relatorioJson", valorOuVazio(resultado.relatorioJson())
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("erro", "Falha ao corrigir legendas: " + e.getMessage()));
        }
    }

    private String valorOuVazio(String valor) {
        return valor != null ? valor : "";
    }
}
