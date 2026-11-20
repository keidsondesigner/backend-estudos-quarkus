package br.com.keidson.resource;

import br.com.keidson.dto.FluxoRequest;
import br.com.keidson.dto.FluxoResponse;
import br.com.keidson.dto.StatusResponse;
import br.com.keidson.service.FluxoService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/fluxos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FluxoResource {

    @Inject
    FluxoService fluxoService;

    @POST
    public FluxoResponse criarFluxo(FluxoRequest request) {
        return fluxoService.criarFluxo(request);
    }

    @GET
    @Path("/{idDocumento}/status")
    public StatusResponse consultarStatus(@PathParam("idDocumento") String idDocumento) {
        return fluxoService.consultarStatus(idDocumento);
    }
}