package br.com.keidson.resource;

import br.com.keidson.dto.ProcessamentoRequest;
import br.com.keidson.dto.ProcessamentoResponse;
import br.com.keidson.dto.StatusResponse;
import br.com.keidson.service.ProcessamentoService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/processar")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProcessamentoResource {

    @Inject
    ProcessamentoService processamentoService;

    @POST
    public ProcessamentoResponse processar(ProcessamentoRequest request) {
        return processamentoService.processar(request);
    }

    @GET
    @Path("/{idDocumento}/status")
    public StatusResponse consultarStatus(@PathParam("idDocumento") String idDocumento) {
        return processamentoService.consultarStatus(idDocumento);
    }
}