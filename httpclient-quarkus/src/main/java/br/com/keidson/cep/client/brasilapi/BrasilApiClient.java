package br.com.keidson.cep.client.brasilapi;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import br.com.keidson.cep.exception.CepInvalidoException;
import br.com.keidson.cep.exception.CepNaoEncontradoException;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;

/**
 * Segundo provedor de CEP, usado como fallback quando o ViaCEP falha.
 * URL base: quarkus.rest-client.brasilapi.url
 */
@RegisterRestClient(configKey = "brasilapi")
@Path("/api/cep/v1")
@Produces(MediaType.APPLICATION_JSON)
public interface BrasilApiClient {

    /** GET https://brasilapi.com.br/api/cep/v1/69005040 */
    @GET
    @Path("/{cep}")
    BrasilApiResponse buscarPorCep(@PathParam("cep") String cep);

    /** A BrasilAPI responde 404 para CEP inexistente (diferente do ViaCEP). */
    @ClientExceptionMapper
    static RuntimeException toException(Response response) {
        return switch (response.getStatus()) {
            case 404 -> new CepNaoEncontradoException("CEP não encontrado");
            case 400 -> new CepInvalidoException("Requisição inválida para a BrasilAPI");
            default -> null;
        };
    }
}
