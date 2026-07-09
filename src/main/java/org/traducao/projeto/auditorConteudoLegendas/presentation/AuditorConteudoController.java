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

    public record AuditoriaRequest(String caminhoOriginal, String caminhoTraduzido) {}

    @POST
    public Response auditar(AuditoriaRequest request) {
        if (request == null
            || request.caminhoOriginal() == null || request.caminhoOriginal().isBlank()
            || request.caminhoTraduzido() == null || request.caminhoTraduzido().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Caminhos original e traduzido sao obrigatorios.")
                .build();
        }

        try {
            java.nio.file.Path original = java.nio.file.Path.of(request.caminhoOriginal().trim());
            java.nio.file.Path traduzido = java.nio.file.Path.of(request.caminhoTraduzido().trim());
            logStreamService.definirCanalAtual("auditor-conteudo");
            long inicioMs = System.currentTimeMillis();
            RelatorioAuditoriaConteudo relatorio = auditorConteudoUseCase.auditar(original, traduzido);
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
