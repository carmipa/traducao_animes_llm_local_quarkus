package org.traducao.projeto.auditorConteudoLegendas.presentation;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traducao.projeto.auditorConteudoLegendas.application.AuditorConteudoUseCase;
import org.traducao.projeto.auditorConteudoLegendas.domain.AuditoriaException;
import org.traducao.projeto.auditorConteudoLegendas.domain.ModoAuditoria;
import org.traducao.projeto.auditorConteudoLegendas.domain.RelatorioAuditoriaConteudo;
import org.traducao.projeto.traducao.presentation.web.LogStreamService;

@Path("/api/auditoria-conteudo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuditorConteudoController {

    @Inject
    AuditorConteudoUseCase auditorConteudoUseCase;

    @Inject
    LogStreamService logStreamService;

    public record AuditoriaRequest(String modo, String caminhoOriginal, String caminhoTraduzido) {}

    /**
     * PROPÓSITO DE NEGÓCIO: expõe a Análise de Conteúdo nos três escopos das abas
     * do painel (só original, só traduzido, ambos) sobre o mesmo endpoint.
     * <p>INVARIANTES DO DOMÍNIO: o modo determina quais caminhos são obrigatórios;
     * modo ausente equivale a AMBAS (retrocompatível).
     * <p>COMPORTAMENTO EM CASO DE FALHA: caminho exigido em branco → 400 didático;
     * {@link AuditoriaException} → 400 com a mensagem de domínio; erro inesperado
     * → 500.
     */
    @POST
    public Response auditar(AuditoriaRequest request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Requisicao de auditoria ausente.")
                .build();
        }

        ModoAuditoria modo = ModoAuditoria.porNome(request.modo());
        boolean temOriginal = request.caminhoOriginal() != null && !request.caminhoOriginal().isBlank();
        boolean temTraduzido = request.caminhoTraduzido() != null && !request.caminhoTraduzido().isBlank();

        String erroValidacao = switch (modo) {
            case AMBAS -> (temOriginal && temTraduzido) ? null
                : "Caminhos original e traduzido sao obrigatorios.";
            case ORIGINAL -> temOriginal ? null
                : "Caminho do arquivo original e obrigatorio.";
            case TRADUZIDO -> temTraduzido ? null
                : "Caminho do arquivo traduzido e obrigatorio.";
        };
        if (erroValidacao != null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(erroValidacao).build();
        }

        try {
            java.nio.file.Path original = temOriginal
                ? java.nio.file.Path.of(request.caminhoOriginal().trim()) : null;
            java.nio.file.Path traduzido = temTraduzido
                ? java.nio.file.Path.of(request.caminhoTraduzido().trim()) : null;
            logStreamService.definirCanalAtual("auditor-conteudo");
            long inicioMs = System.currentTimeMillis();
            RelatorioAuditoriaConteudo relatorio = auditorConteudoUseCase.auditar(modo, original, traduzido);
            System.out.println(org.traducao.projeto.core.util.DuracaoUtil.linhaRelatorioFinal(
                "Análise de Conteúdo de Legendas", inicioMs));
            return Response.ok(relatorio).build();
        } catch (AuditoriaException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(e.getMessage())
                .build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }
}
