package br.com.keidson.resource;

import br.com.keidson.dto.NotificacaoRequest;
import br.com.keidson.dto.NotificacaoResponse;
import br.com.keidson.service.NotificacaoService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/notificacoes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificacaoResource {

    @Inject
    NotificacaoService notificacaoService;

    @POST
    public NotificacaoResponse enviar(NotificacaoRequest request) {
        return notificacaoService.enviar(request);
    }
}