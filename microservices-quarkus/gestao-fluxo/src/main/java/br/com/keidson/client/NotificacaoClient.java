// client/NotificacaoClient.java
package br.com.keidson.client;

import br.com.keidson.dto.NotificacaoRequest;
import br.com.keidson.dto.NotificacaoResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/notificacoes")
@RegisterRestClient(configKey = "notificacao-service")   // ← chave DIFERENTE
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface NotificacaoClient {

    @POST
    NotificacaoResponse enviar(NotificacaoRequest request);
}