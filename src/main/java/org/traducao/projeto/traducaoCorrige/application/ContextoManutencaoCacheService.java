package org.traducao.projeto.traducaoCorrige.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.traducao.infrastructure.cache.CacheManutencaoService;
import org.traducao.projeto.traducao.infrastructure.cache.ProvenienciaCache;
import org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto;

/**
 * PROPÓSITO DE NEGÓCIO: garante que cada arquivo da pasta cache seja analisado
 * com a lore da obra que realmente o originou, mesmo quando a raiz contém
 * caches de vários animes.
 *
 * <p>INVARIANTES DO DOMÍNIO: a proveniência versionada tem prioridade; contexto
 * manual serve somente como fallback para cache legado; contexto desconhecido
 * nunca cai silenciosamente no padrão.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: lança {@link IllegalArgumentException} e o
 * arquivo é contabilizado como falha sem ser modificado.
 */
@Service
public class ContextoManutencaoCacheService {

    private final GerenciadorContexto gerenciadorContexto;

    /**
     * PROPÓSITO DE NEGÓCIO: conecta a manutenção ao catálogo local de lores.
     * <p>INVARIANTES DO DOMÍNIO: existe um gerenciador compartilhado pela fila serial.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede o uso do serviço.
     */
    public ContextoManutencaoCacheService(GerenciadorContexto gerenciadorContexto) {
        this.gerenciadorContexto = gerenciadorContexto;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: ativa e devolve o contexto correto para uma unidade
     * de cache antes de qualquer classificação ou chamada de LLM.
     *
     * <p>INVARIANTES DO DOMÍNIO: cache versionado não pode ser reinterpretado
     * com o fallback de outra obra; cache legado exige seleção explícita.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lança {@link IllegalArgumentException}
     * para contexto ausente/desconhecido, preservando o arquivo.
     */
    public String ativar(CacheManutencaoService.DocumentoEditavel documento, String contextoFallback) {
        ProvenienciaCache proveniencia = documento.proveniencia();
        String contextoId = proveniencia != null ? proveniencia.contextoId() : contextoFallback;
        if (contextoId == null || contextoId.isBlank()) {
            throw new IllegalArgumentException(
                "Cache legado sem proveniência: selecione a Obra / Contexto para processar "
                    + documento.arquivo().getFileName());
        }
        if (!gerenciadorContexto.existeContexto(contextoId)) {
            throw new IllegalArgumentException(
                "Contexto da proveniência não existe no projeto: \"" + contextoId + "\" em "
                    + documento.arquivo().getFileName());
        }
        gerenciadorContexto.definirContextoAtivo(contextoId);
        return contextoId;
    }
}
