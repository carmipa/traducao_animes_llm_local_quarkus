package org.traducao.projeto.traducao.infrastructure.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

public class RecordsMistral {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Mensagem(String role, String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChatRequest(String model, List<Mensagem> messages, double temperature, int max_tokens) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(Mensagem message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RespostaLlm(List<Choice> choices) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ModeloDisponivel(String id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ListaModelos(List<ModeloDisponivel> data) {}
}
