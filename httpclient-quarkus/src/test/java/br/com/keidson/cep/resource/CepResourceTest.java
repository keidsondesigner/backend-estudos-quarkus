package br.com.keidson.cep.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import jakarta.ws.rs.ProcessingException;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import br.com.keidson.cep.client.brasilapi.BrasilApiClient;
import br.com.keidson.cep.client.brasilapi.BrasilApiResponse;
import br.com.keidson.cep.client.viacep.ViaCepClient;
import br.com.keidson.cep.client.viacep.ViaCepResponse;
import br.com.keidson.cep.exception.CepNaoEncontradoException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Testes de integração da API com os REST Clients mockados
 * (nenhuma chamada real à internet é feita).
 * Cada teste usa um CEP diferente porque o cache é compartilhado.
 */
@QuarkusTest
class CepResourceTest {

    @InjectMock
    @RestClient
    ViaCepClient viaCepClient;

    @InjectMock
    @RestClient
    BrasilApiClient brasilApiClient;

    @Test
    void deveBuscarCepNoViaCep() {
        when(viaCepClient.buscarPorCep("69005040")).thenReturn(viaCep("69005-040", "Avenida Eduardo Ribeiro", "Centro", "Manaus", "AM"));

        given()
                .when().get("/ceps/69005-040")
                .then()
                .statusCode(200)
                .body("cep", is("69005-040"))
                .body("logradouro", is("Avenida Eduardo Ribeiro"))
                .body("cidade", is("Manaus"))
                .body("uf", is("AM"))
                .body("ddd", is("92"))
                .body("fonte", is("viacep"));

        verify(brasilApiClient, never()).buscarPorCep(anyString());
    }

    @Test
    void deveRetornar400ParaCepInvalido() {
        given()
                .when().get("/ceps/123")
                .then()
                .statusCode(400)
                .body("status", is(400));
    }

    @Test
    void deveRetornar404QuandoViaCepRespondeErro() {
        when(viaCepClient.buscarPorCep("99999999")).thenReturn(viaCepNaoEncontrado());

        given()
                .when().get("/ceps/99999999")
                .then()
                .statusCode(404)
                .body("status", is(404));

        // Não encontrado é resposta definitiva: sem retry e sem fallback
        verify(viaCepClient, times(1)).buscarPorCep("99999999");
        verify(brasilApiClient, never()).buscarPorCep(anyString());
    }

    @Test
    void deveUsarFallbackBrasilApiQuandoViaCepFalha() {
        when(viaCepClient.buscarPorCep("01001000")).thenThrow(new ProcessingException("Connection refused"));
        when(brasilApiClient.buscarPorCep("01001000"))
                .thenReturn(new BrasilApiResponse("01001000", "SP", "São Paulo", "Sé", "Praça da Sé", "open-cep"));

        given()
                .when().get("/ceps/01001000")
                .then()
                .statusCode(200)
                .body("cep", is("01001-000"))
                .body("cidade", is("São Paulo"))
                .body("fonte", is("brasilapi"));

        // 1 tentativa + 2 retries no ViaCEP antes de cair no fallback
        verify(viaCepClient, times(3)).buscarPorCep("01001000");
        verify(brasilApiClient, times(1)).buscarPorCep("01001000");
    }

    @Test
    void deveRetornar503QuandoTodosOsProvedoresFalham() {
        when(viaCepClient.buscarPorCep("20040002")).thenThrow(new ProcessingException("timeout"));
        when(brasilApiClient.buscarPorCep("20040002")).thenThrow(new ProcessingException("timeout"));

        given()
                .when().get("/ceps/20040002")
                .then()
                .statusCode(503);
    }

    @Test
    void deveRetornar404QuandoFallbackNaoEncontra() {
        when(viaCepClient.buscarPorCep("30130010")).thenThrow(new ProcessingException("timeout"));
        when(brasilApiClient.buscarPorCep("30130010")).thenThrow(new CepNaoEncontradoException("CEP não encontrado"));

        given()
                .when().get("/ceps/30130010")
                .then()
                .statusCode(404);
    }

    @Test
    void deveUsarCacheNaSegundaChamada() {
        when(viaCepClient.buscarPorCep("69020030")).thenReturn(viaCep("69020-030", "Rua Teste", "Centro", "Manaus", "AM"));

        given().when().get("/ceps/69020030").then().statusCode(200);
        given().when().get("/ceps/69020-030").then().statusCode(200); // mesma chave após normalizar

        verify(viaCepClient, times(1)).buscarPorCep("69020030");
    }

    @Test
    void deveBuscarPorEndereco() {
        when(viaCepClient.buscarPorEndereco("AM", "Manaus", "Djalma")).thenReturn(List.of(
                viaCep("69050-010", "Avenida Djalma Batista", "Chapada", "Manaus", "AM"),
                viaCep("69050-030", "Avenida Djalma Batista", "Nossa Senhora das Graças", "Manaus", "AM")));

        given()
                .queryParam("uf", "am")
                .queryParam("cidade", "Manaus")
                .queryParam("logradouro", "Djalma")
                .when().get("/ceps")
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("[0].logradouro", is("Avenida Djalma Batista"));
    }

    @Test
    void deveValidarParametrosDaBuscaPorEndereco() {
        given()
                .queryParam("uf", "AMZ")
                .queryParam("cidade", "Ma")
                .queryParam("logradouro", "Dj")
                .when().get("/ceps")
                .then()
                .statusCode(400);
    }

    // ------------------------------------------------------------------

    private static ViaCepResponse viaCep(String cep, String logradouro, String bairro, String cidade, String uf) {
        return new ViaCepResponse(cep, logradouro, "", "", bairro, cidade, uf, null, null,
                "1302603", "", "92", "0255", null);
    }

    private static ViaCepResponse viaCepNaoEncontrado() {
        return new ViaCepResponse(null, null, null, null, null, null, null, null, null,
                null, null, null, null, Boolean.TRUE);
    }
}
