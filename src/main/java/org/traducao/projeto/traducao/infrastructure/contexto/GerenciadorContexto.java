package org.traducao.projeto.traducao.infrastructure.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.exceptions.ContextoNaoEncontradoException;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class GerenciadorContexto {

    private final List<ProvedorContexto> provedores;
    private final ProvedorContexto provedorPadrao;

    // Mutado pela thread única do executor de background (ApiController) e lido
    // pela mesma thread ao montar o prompt do LLM (MistralClientAdapter). O
    // volatile aqui é uma garantia defensiva de visibilidade, não uma alegação
    // de que múltiplas threads concorrem por este campo.
    private volatile ProvedorContexto provedorAtivo;

    public GerenciadorContexto(List<ProvedorContexto> provedores) {
        this.provedores = provedores.stream()
                .sorted(Comparator.comparing(ProvedorContexto::getNomeExibicao, String.CASE_INSENSITIVE_ORDER))
                .toList();
        validarIdsUnicos(this.provedores);
        this.provedorPadrao = null; // Removido fallback hardcoded para 'danmachi'
        this.provedorAtivo = null;
    }

    public List<ProvedorContexto> getProvedores() {
        return provedores;
    }

    /**
     * Id do contexto usado quando nenhuma seleção explícita é feita.
     * Retorna nulo por padrão para forçar seleção explícita na UI.
     */
    public String getIdContextoPadrao() {
        return provedorPadrao != null ? provedorPadrao.getId() : null;
    }

    public boolean existeContexto(String id) {
        return id != null && provedores.stream().anyMatch(p -> p.getId().equals(id));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: Define explicitamente qual o contexto de lore que o modelo
     * de IA usará para a sessão de tradução ativa. Isso impede que legendas de animes
     * diferentes sofram vazamento de dados (cross-contamination) entre si.
     *
     * INVARIANTES DO DOMÍNIO:
     * - O ID do contexto deve existir na lista de provedores carregados.
     * - Nunca deve aceitar IDs em branco (evitando fallbacks cegos e silenciosos).
     *
     * COMPORTAMENTO EM CASO DE FALHA:
     * - Dispara {@link ContextoNaoEncontradoException} se o ID fornecido for nulo,
     *   em branco ou não corresponder a um contexto registrado. A aplicação aborta a operação.
     */
    public ProvedorContexto definirContextoAtivo(String id) {
        if (id == null || id.isBlank()) {
            throw new ContextoNaoEncontradoException(
                    "ID do contexto não pode ser vazio. É obrigatório selecionar o anime na interface web antes de processar.");
        }
        ProvedorContexto encontrado = provedores.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ContextoNaoEncontradoException(
                        "Contexto de tradução desconhecido: \"" + id + "\". Contextos disponíveis: "
                                + provedores.stream().map(ProvedorContexto::getId).collect(Collectors.joining(", "))));
        this.provedorAtivo = encontrado;
        return this.provedorAtivo;
    }

    public String obterPromptAtivo() {
        if (this.provedorAtivo == null) {
            return "Voce e um tradutor especialista. Traduza fielmente e nao resuma ou adicione comentarios.";
        }
        return this.provedorAtivo.obterPromptSistema();
    }

    /**
     * Retorna apenas a lore/terminologia do contexto ativo, sem o restante do
     * prompt de traducao (prioridades, regras de concordancia, regras de
     * saida). Usado por revisoes pontuais (ex.: concordancia PT-BR) que nao
     * devem reenviar o prompt de traducao inteiro ao LLM como se fosse lore.
     */
    public String obterLoreAtiva() {
        return ContextoPrompt.obterLore(obterPromptAtivo());
    }

    public String obterNomeContextoAtivo() {
        return this.provedorAtivo != null ? this.provedorAtivo.getNomeExibicao() : "Padrao";
    }

    /**
     * Id do contexto ativo (não o nome de exibição). Usado para carimbar a
     * proveniência do cache de tradução, de modo que uma legenda em cache saiba
     * com qual lore foi produzida. Retorna {@code null} se não houver contexto ativo.
     */
    public String obterIdContextoAtivo() {
        return this.provedorAtivo != null ? this.provedorAtivo.getId() : null;
    }

    /**
     * Termos protegidos (não traduzir) do lore atualmente ativo. Usado pelo
     * detector de tradução idêntica para acompanhar o lore selecionado. Vazio
     * quando não há contexto ativo ou o contexto não declara termos.
     */
    public java.util.Set<String> termosProtegidosAtivos() {
        return this.provedorAtivo != null ? this.provedorAtivo.termosProtegidos() : java.util.Set.of();
    }

    private void validarIdsUnicos(List<ProvedorContexto> provedores) {
        Map<String, Long> contagemPorId = provedores.stream()
                .map(ProvedorContexto::getId)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        List<String> duplicados = contagemPorId.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();

        if (!duplicados.isEmpty()) {
            throw new IllegalStateException("IDs de contexto duplicados: " + duplicados);
        }
    }
}
