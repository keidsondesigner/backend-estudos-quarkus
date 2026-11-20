package br.com.keidson.client;

import br.com.keidson.dto.ProcessamentoRequest;
import br.com.keidson.dto.ProcessamentoResponse;
import br.com.keidson.dto.StatusResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

// o Client consome um endpoint "/processar" que existe em outro serviço
// worker-interno, que é o serviço que vai processar a requisição
// ProcessamentoResource.java
// curl ──POST /fluxos──▶ [gestao-fluxo :8080]
//                             │
//                        FluxoResource      ◀── recebe (rota EXISTE aqui)
//                             │
//                        FluxoService
//                             │
//                     WorkerInternoClient   ◀── envia (rota NÃO existe aqui,
//                             │                  é só o "endereço" do destino)
//                             ▼
//                   ──POST /processar──▶ [worker-interno :8081]
//                                             │
//                                      ProcessamentoResource  ◀── recebe
//                                             │                   (rota EXISTE aqui)
//                                      ProcessamentoService
@Path("/processar")
@RegisterRestClient(configKey = "worker-interno")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface WorkerInternoClient {

    @POST
    ProcessamentoResponse processar(ProcessamentoRequest request);

    @GET
    @Path("/{idDocumento}/status")
    StatusResponse consultarStatus(@PathParam("idDocumento") String idDocumento);
}