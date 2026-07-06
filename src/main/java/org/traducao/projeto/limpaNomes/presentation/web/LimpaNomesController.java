package org.traducao.projeto.limpaNomes.presentation.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traducao.projeto.limpaNomes.application.RenomeadorUseCase;
import org.traducao.projeto.limpaNomes.domain.OperacaoRenomeacao;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Path("/api/limpa-nomes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LimpaNomesController {

    @Inject
    RenomeadorUseCase renomeadorUseCase;

    @POST
    @Path("/simular")
    public Response simular(LimpaNomesRequest request) {
        if (request.caminhoOrigem() == null || request.caminhoOrigem().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "Caminho de origem não informado")).build();
        }
        
        // Executamos de forma assíncrona para não bloquear a thread REST (útil se tiver muitos arquivos)
        CompletableFuture.runAsync(() -> {
            renomeadorUseCase.simularRenomeacao(Paths.get(request.caminhoOrigem()), request.nomePadrao());
        });
        
        return Response.ok(Map.of("mensagem", "Simulação iniciada. Acompanhe no terminal/logs.")).build();
    }

    @POST
    @Path("/aplicar")
    public Response aplicar(LimpaNomesRequest request) {
        if (request.caminhoOrigem() == null || request.caminhoOrigem().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "Caminho de origem não informado")).build();
        }
        
        CompletableFuture.runAsync(() -> {
            renomeadorUseCase.aplicarRenomeacao(Paths.get(request.caminhoOrigem()), request.nomePadrao());
        });
        
        return Response.ok(Map.of("mensagem", "Aplicação de renomeação iniciada. Acompanhe nos logs.")).build();
    }

    @POST
    @Path("/reverter")
    public Response reverter(LimpaNomesRequest request) {
        if (request.caminhoOrigem() == null || request.caminhoOrigem().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "Caminho de origem não informado")).build();
        }
        
        CompletableFuture.runAsync(() -> {
            renomeadorUseCase.reverterRenomeacao(Paths.get(request.caminhoOrigem()));
        });
        
        return Response.ok(Map.of("mensagem", "Reversão iniciada. Acompanhe nos logs.")).build();
    }
}
