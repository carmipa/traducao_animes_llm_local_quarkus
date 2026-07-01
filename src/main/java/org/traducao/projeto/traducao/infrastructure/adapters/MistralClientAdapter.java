package org.traducao.projeto.traducao.infrastructure.adapters;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.domain.Lote;
import org.traducao.projeto.traducao.domain.StatusLlm;
import org.traducao.projeto.traducao.domain.TraducaoLote;
import org.traducao.projeto.traducao.domain.exceptions.RespostaLlmVaziaException;
import org.traducao.projeto.traducao.domain.ports.MistralPort;
import org.traducao.projeto.traducao.contexto.RegrasConcordanciaPtBr;
import org.traducao.projeto.traducao.infrastructure.config.LlmProperties;
import org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto;
import org.traducao.projeto.traducao.infrastructure.dtos.RecordsMistral.*;
import org.traducao.projeto.traducao.infrastructure.http.JsonHttpClient;
import org.traducao.projeto.traducao.infrastructure.http.JsonHttpClient.HttpClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class MistralClientAdapter implements MistralPort {

    private static final Logger log = LoggerFactory.getLogger(MistralClientAdapter.class);

    private static final int MAX_TENTATIVAS = 3;
    private static final int MAX_TENTATIVAS_REVISAO = 2;
    private static final long PAUSA_ENTRE_TENTATIVAS_MS = 2_000;
    private static final double TEMPERATURA_REVISAO = 0.15;
    private static final double TEMPERATURA_CORRECAO_TRADUCAO = 0.3;

    private final JsonHttpClient httpClient;
    private final LlmProperties propriedades;
    private final GerenciadorContexto gerenciadorContexto;

    public MistralClientAdapter(LlmProperties propriedades, GerenciadorContexto gerenciadorContexto, ObjectMapper mapper) {
        this.propriedades = propriedades;
        this.gerenciadorContexto = gerenciadorContexto;
        this.httpClient = new JsonHttpClient(propriedades, propriedades.baseUrl(), mapper);
    }

    @Override
    public StatusLlm verificarDisponibilidade() {
        String modeloConfigurado = propriedades.model();
        try {
            ListaModelos resposta = httpClient.get("/models", ListaModelos.class);

            List<ModeloDisponivel> modelos = resposta != null ? resposta.data() : null;
            if (modelos == null || modelos.isEmpty()) {
                return new StatusLlm(true, false,
                    "Servidor LLM em " + propriedades.baseUrl() + " respondeu, mas nenhum modelo está carregado em memória.");
            }

            // 1. Tenta achar o modelo configurado (busca exata ou parcial case-insensitive)
            Optional<String> modeloEncontrado = modelos.stream()
                .map(ModeloDisponivel::id)
                .filter(id -> id != null)
                .filter(id -> id.equalsIgnoreCase(modeloConfigurado) 
                    || id.toLowerCase().contains(modeloConfigurado.toLowerCase()) 
                    || modeloConfigurado.toLowerCase().contains(id.toLowerCase()))
                .findFirst();

            if (modeloEncontrado.isPresent()) {
                // Modelo configurado está carregado. Garante que usamos o ID oficial retornado pelo LM Studio
                propriedades.setModel(modeloEncontrado.get());
                return new StatusLlm(true, true,
                    "Servidor LLM online e modelo \"" + modeloEncontrado.get() + "\" carregado em memória.");
            }

            // 2. Se o configurado não está carregado, mas há outro(s) modelo(s) em memória, adota o primeiro ativo!
            String modeloAtivo = modelos.get(0).id();
            log.info("Modelo configurado (\"{}\") não está carregado no LM Studio. Adaptando dinamicamente para o modelo carregado na memória: \"{}\"",
                modeloConfigurado, modeloAtivo);
            propriedades.setModel(modeloAtivo);

            return new StatusLlm(true, true,
                "Servidor LLM online. Adaptado dinamicamente para usar o modelo ativo em memória: \"" + modeloAtivo + "\".");
        } catch (Exception e) {
            return new StatusLlm(false, false,
                "Não foi possível conectar ao servidor LLM em " + propriedades.baseUrl() + ": " + e.getMessage());
        }
    }

    @Override
    public TraducaoLote traduzir(Lote lote) {
        String prompt = montarPrompt(lote);
        ChatRequest request = new ChatRequest(
            propriedades.model(),
            List.of(
                new Mensagem("system", gerenciadorContexto.obterPromptAtivo()),
                new Mensagem("user", prompt)
            ),
            propriedades.temperature(),
            propriedades.maxTokens()
        );

        Exception ultimaFalha = null;
        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            try {
                log.debug("Enviando lote {} ao LLM ({} linha(s)) — tentativa {}/{}",
                    lote.idLote(), lote.linhasOriginais().size(), tentativa, MAX_TENTATIVAS);
                long inicio = System.currentTimeMillis();

                RespostaLlm resposta = httpClient.post("/chat/completions", request, RespostaLlm.class);

                long duracaoMs = System.currentTimeMillis() - inicio;

                if (resposta == null || resposta.choices() == null || resposta.choices().isEmpty()) {
                    throw new RespostaLlmVaziaException("Resposta vazia do LLM para o lote " + lote.idLote());
                }

                Mensagem mensagem = resposta.choices().getFirst().message();
                String traduzidoText = mensagem != null ? mensagem.content() : null;
                if (traduzidoText == null || traduzidoText.isBlank()) {
                    throw new RespostaLlmVaziaException("Conteudo vazio retornado pelo LLM para o lote " + lote.idLote());
                }

                List<String> linhasTraduzidas = extrairLinhasTraduzidas(traduzidoText);
                log.info("Lote {} traduzido em {} ms ({} -> {} linha(s))",
                    lote.idLote(), duracaoMs, lote.linhasOriginais().size(), linhasTraduzidas.size());

                return new TraducaoLote(lote.idLote(), linhasTraduzidas, true, null);

            } catch (RespostaLlmVaziaException e) {
                log.warn(e.getMessage());
                return new TraducaoLote(lote.idLote(), null, false, e.getMessage());
            } catch (HttpClientException e) {
                ultimaFalha = e;
                log.warn("LLM respondeu com erro HTTP {} para o lote {} (tentativa {}/{}): {}",
                    e.statusCode(), lote.idLote(), tentativa, MAX_TENTATIVAS, e.getMessage());
            } catch (Exception e) {
                ultimaFalha = e;
                if (JsonHttpClient.isErroRedeOuTimeout(e)) {
                    log.warn("Falha de rede/timeout ao chamar o LLM para o lote {} (tentativa {}/{}): {}",
                        lote.idLote(), tentativa, MAX_TENTATIVAS, e.getMessage());
                } else if (e.getMessage() != null && e.getMessage().contains("interrupt")) {
                    log.warn("Erro ao traduzir o lote {} - Thread abortada (tentativa {}/{})",
                        lote.idLote(), tentativa, MAX_TENTATIVAS);
                } else {
                    log.warn("Erro ao traduzir o lote {} (tentativa {}/{}): {}",
                        lote.idLote(), tentativa, MAX_TENTATIVAS, e.getMessage());
                }
            }

            if (tentativa < MAX_TENTATIVAS) {
                try {
                    Thread.sleep(PAUSA_ENTRE_TENTATIVAS_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        String mensagemFinal = "Erro ao traduzir o lote " + lote.idLote()
            + " após " + MAX_TENTATIVAS + " tentativa(s)";
        if (ultimaFalha != null) {
            mensagemFinal += ": " + ultimaFalha.getMessage();
        }
        log.error(mensagemFinal);
        return new TraducaoLote(lote.idLote(), null, false, mensagemFinal);
    }

    @Override
    public Optional<String> revisarConcordancia(
        String originalInglesMascarado,
        String traducaoPtMascarada,
        List<String> problemasDetectados
    ) {
        String promptUsuario = montarPromptRevisao(originalInglesMascarado, traducaoPtMascarada, problemasDetectados);
        String promptSistema = RegrasConcordanciaPtBr.montarPromptRevisao(gerenciadorContexto.obterLoreAtiva());

        ChatRequest request = new ChatRequest(
            propriedades.model(),
            List.of(
                new Mensagem("system", promptSistema),
                new Mensagem("user", promptUsuario)
            ),
            TEMPERATURA_REVISAO,
            propriedades.maxTokens()
        );

        return postarLinhaUnica(request);
    }

    @Override
    public Optional<String> corrigirTraducao(
        String originalInglesMascarado,
        String traducaoPtMascarada,
        String motivoDetectado
    ) {
        String promptUsuario = montarPromptCorrecaoTraducao(originalInglesMascarado, traducaoPtMascarada, motivoDetectado);

        ChatRequest request = new ChatRequest(
            propriedades.model(),
            List.of(
                new Mensagem("system", gerenciadorContexto.obterPromptAtivo()),
                new Mensagem("user", promptUsuario)
            ),
            TEMPERATURA_CORRECAO_TRADUCAO,
            propriedades.maxTokens()
        );

        return postarLinhaUnica(request);
    }

    private Optional<String> postarLinhaUnica(ChatRequest request) {
        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS_REVISAO; tentativa++) {
            try {
                RespostaLlm resposta = httpClient.post("/chat/completions", request, RespostaLlm.class);

                if (resposta == null || resposta.choices() == null || resposta.choices().isEmpty()) {
                    continue;
                }

                Mensagem mensagem = resposta.choices().getFirst().message();
                String texto = mensagem != null ? mensagem.content() : null;
                if (texto == null || texto.isBlank()) {
                    continue;
                }

                return Optional.of(normalizarLinhaUnica(texto));
            } catch (Exception e) {
                log.warn("Falha na chamada LLM (tentativa {}/{}): {}",
                    tentativa, MAX_TENTATIVAS_REVISAO, e.getMessage());
                if (tentativa < MAX_TENTATIVAS_REVISAO) {
                    try {
                        Thread.sleep(PAUSA_ENTRE_TENTATIVAS_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        return Optional.empty();
    }

    private String montarPromptCorrecaoTraducao(String originalIngles, String traducaoPt, String motivoDetectado) {
        return """
            A traducao abaixo ficou com residuo em ingles, incompleta ou alucinada.
            Retraduza esta fala para PT-BR corretamente, preservando o sentido e os
            marcadores [[TAGn]] literalmente (nao traduza nem remova marcadores).

            Original (ingles):
            %s

            Traducao atual (problema detectado: %s):
            %s

            Responda com uma unica linha: a traducao corrigida em portugues.
            """.formatted(originalIngles, motivoDetectado, traducaoPt);
    }

    private String montarPromptRevisao(
        String originalIngles, String traducaoPt, List<String> problemasDetectados
    ) {
        String listaProblemas = problemasDetectados == null || problemasDetectados.isEmpty()
            ? "(nenhum detalhe heurístico)"
            : String.join("\n- ", problemasDetectados);

        return """
            Corrija APENAS concordancia de genero/pronomes/adjetivos na traducao em portugues.
            Original em ingles (referencia de genero/contexto):
            %s

            Traducao atual em portugues (corrigir se necessario):
            %s

            Problemas detectados automaticamente:
            - %s

            Se o problema mencionar "masculino marcado" e o original nao indicar genero:
            - use feminino quando a lore/personagem indicar falante ou interlocutora mulher;
            - se a lore nao permitir inferir, troque por uma formulacao neutra natural em PT-BR;
            - nao mantenha masculino apenas por costume ou padrao generico.

            Responda com uma unica linha: a traducao corrigida.
            """.formatted(originalIngles, traducaoPt, listaProblemas);
    }

    private String normalizarLinhaUnica(String texto) {
        String normalizado = texto.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (normalizado.startsWith("```") && normalizado.endsWith("```")) {
            normalizado = removerCercaMarkdown(normalizado).strip();
        }
        int quebra = normalizado.indexOf('\n');
        if (quebra >= 0) {
            normalizado = normalizado.substring(0, quebra).stripTrailing();
        }
        return normalizado;
    }

    private String montarPrompt(Lote lote) {
        int totalLinhas = lote.linhasOriginais().size();
        String linhas = String.join("\n", lote.linhasOriginais());
        return "Traduza estas " + totalLinhas + " linha(s), uma por linha. Responda com exatamente "
            + totalLinhas + " linha(s) de saida, na mesma ordem:\n" + linhas;
    }

    private List<String> extrairLinhasTraduzidas(String texto) {
        String normalizado = texto.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (normalizado.startsWith("```") && normalizado.endsWith("```")) {
            normalizado = removerCercaMarkdown(normalizado).strip();
        }

        String[] linhas = normalizado.split("\n", -1);
        List<String> resultado = new ArrayList<>(linhas.length);
        for (String linha : linhas) {
            resultado.add(linha.stripTrailing());
        }
        return resultado;
    }

    private String removerCercaMarkdown(String texto) {
        int primeiraQuebra = texto.indexOf('\n');
        int ultimaCerca = texto.lastIndexOf("```");
        if (primeiraQuebra < 0 || ultimaCerca <= primeiraQuebra) {
            return texto;
        }
        return texto.substring(primeiraQuebra + 1, ultimaCerca);
    }
}
