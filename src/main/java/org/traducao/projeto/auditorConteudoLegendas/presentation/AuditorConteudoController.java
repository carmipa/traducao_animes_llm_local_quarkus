package org.traducao.projeto.auditorConteudoLegendas.presentation;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traducao.projeto.auditorConteudoLegendas.application.AuditorConteudoUseCase;
import org.traducao.projeto.auditorConteudoLegendas.domain.RelatorioAuditoriaConteudo;

@Path("/api/auditoria-conteudo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuditorConteudoController {

    @Inject
    AuditorConteudoUseCase auditorConteudoUseCase;

    public record AuditoriaRequest(String caminhoOriginal, String caminhoTraduzido) {}

    @POST
    public Response auditar(AuditoriaRequest request) {
        if (request.caminhoOriginal() == null || request.caminhoTraduzido() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Caminhos original e traduzido são obrigatórios.")
                    .build();
        }

        try {
            java.nio.file.Path original = java.nio.file.Path.of(request.caminhoOriginal());
            java.nio.file.Path traduzido = java.nio.file.Path.of(request.caminhoTraduzido());
            
            RelatorioAuditoriaConteudo relatorio = auditorConteudoUseCase.auditar(original, traduzido);
            return Response.ok(relatorio).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }
}
