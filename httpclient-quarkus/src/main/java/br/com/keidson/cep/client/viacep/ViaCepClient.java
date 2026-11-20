package br.com.keidson.cep.client.viacep;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import br.com.keidson.cep.exception.CepInvalidoException;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;

/**
 * Cliente HTTP DECLARATIVO (MicroProfile REST Client).
 * Você só escreve a interface; o Quarkus gera a implementação em build time.
 * A URL base vem de: quarkus.rest-client.viacep.url
 */
@RegisterRestClient(configKey = "viacep")
@Path("/ws")
@Produces(MediaType.APPLICATION_JSON)
public interface ViaCepClient {

    /** GET https://viacep.com.br/ws/69005040/json/ */
    @GET
    @Path("/{cep}/json/")
    ViaCepResponse buscarPorCep(@PathParam("cep") String cep);

    /** GET https://viacep.com.br/ws/AM/Manaus/Djalma/json/ */
    @GET
    @Path("/{uf}/{cidade}/{logradouro}/json/")
    List<ViaCepResponse> buscarPorEndereco(
            @PathParam("uf") String uf,
            @PathParam("cidade") String cidade,
            @PathParam("logradouro") String logradouro);

    /**
     * Converte status HTTP de erro da API em exceções de domínio.
     * Retornar null = deixa o Quarkus aplicar o tratamento padrão (WebApplicationException).
     */
    @ClientExceptionMapper
    static RuntimeException toException(Response response) {
        if (response.getStatus() == 400) {
            return new CepInvalidoException("Requisição inválida para o ViaCEP");
        }
        return null;
    }
}
