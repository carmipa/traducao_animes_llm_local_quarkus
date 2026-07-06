package org.traducao.projeto.renomearArquivos.presentation.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traducao.projeto.renomearArquivos.application.RenomeadorUseCase;
import org.traducao.projeto.renomearArquivos.domain.OperacaoRenomeacao;
import org.traducao.projeto.traducao.presentation.web.LogStreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Path("/api/renomear-arquivos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RenomearArquivosController {

    private static final Logger log = LoggerFactory.getLogger(RenomearArquivosController.class);

    @Inject
    RenomeadorUseCase renomeadorUseCase;

    @Inject
    LogStreamService logStream;

    @POST
    @Path("/simular")
    public Response simular(RenomearArquivosRequest request) {
        if (request.caminhoOrigem() == null || request.caminhoOrigem().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Caminho de origem não fornecido.").build();
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                renomeadorUseCase.simularRenomeacao(Paths.get(request.caminhoOrigem()), request.nomePadrao());
            } catch (Exception e) {
                log.error("Erro ao simular renomeação", e);
                logStream.publicarLog("renomear-arquivos", "[ERRO FATAL] Falha durante a simulação: " + e.getMessage());
            }
        });
        
        return Response.ok(Map.of("mensagem", "Simulação iniciada. Acompanhe no terminal/logs.")).build();
    }

    @POST
    @Path("/aplicar")
    public Response aplicar(RenomearArquivosRequest request) {
        if (request.caminhoOrigem() == null || request.caminhoOrigem().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Caminho de origem não fornecido.").build();
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                renomeadorUseCase.aplicarRenomeacao(Paths.get(request.caminhoOrigem()), request.nomePadrao());
            } catch (Exception e) {
                log.error("Erro ao aplicar renomeação", e);
                logStream.publicarLog("renomear-arquivos", "[ERRO FATAL] Falha durante a aplicação: " + e.getMessage());
            }
        });
        
        return Response.ok(Map.of("mensagem", "Aplicação de renomeação iniciada. Acompanhe nos logs.")).build();
    }

    @POST
    @Path("/reverter")
    public Response reverter(RenomearArquivosRequest request) {
        if (request.caminhoOrigem() == null || request.caminhoOrigem().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Caminho de origem não fornecido.").build();
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                renomeadorUseCase.reverterRenomeacao(Paths.get(request.caminhoOrigem()));
            } catch (Exception e) {
                log.error("Erro ao reverter renomeação", e);
                logStream.publicarLog("renomear-arquivos", "[ERRO FATAL] Falha durante a reversão: " + e.getMessage());
            }
        });
        
        return Response.ok(Map.of("mensagem", "Reversão iniciada. Acompanhe nos logs.")).build();
    }
}
