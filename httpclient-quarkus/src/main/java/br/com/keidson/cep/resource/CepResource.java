package br.com.keidson.cep.resource;

import java.util.List;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import br.com.keidson.cep.model.Endereco;
import br.com.keidson.cep.provider.BrasilApiProvider;
import br.com.keidson.cep.provider.JdkHttpCepProvider;
import br.com.keidson.cep.provider.ViaCepProvider;
import br.com.keidson.cep.service.CepService;
import br.com.keidson.cep.util.CepUtils;

@Path("/ceps")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "CEP", description = "Consulta de CEP consumindo APIs externas")
public class CepResource {

    @Inject
    CepService service;

    @Inject
    ViaCepProvider viaCep;

    @Inject
    BrasilApiProvider brasilApi;

    @Inject
    JdkHttpCepProvider jdkHttp;

    @GET
    @Path("/{cep}")
    @Operation(summary = "Busca CEP (recomendado)", description = "Cache + ViaCEP com retry/timeout + fallback para BrasilAPI")
    public Endereco buscar(@PathParam("cep") String cep) {
        return service.buscar(CepUtils.normalizar(cep));
    }

    @GET
    @Operation(summary = "Busca CEPs por endereço (ViaCEP)", description = "Ex.: ?uf=AM&cidade=Manaus&logradouro=Djalma")
    public List<Endereco> buscarPorEndereco(
            @QueryParam("uf") @NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "uf deve ter 2 letras") String uf,
            @QueryParam("cidade") @NotBlank @Size(min = 3, message = "cidade deve ter ao menos 3 caracteres") String cidade,
            @QueryParam("logradouro") @NotBlank @Size(min = 3, message = "logradouro deve ter ao menos 3 caracteres") String logradouro) {
        return service.buscarPorEndereco(uf, cidade, logradouro);
    }

    // ------------------------------------------------------------------
    // Endpoints didáticos: chamam cada provedor diretamente (sem cache/fallback)
    // ------------------------------------------------------------------

    @GET
    @Path("/{cep}/viacep")
    @Operation(summary = "Somente ViaCEP via REST Client declarativo")
    public Endereco viaCep(@PathParam("cep") String cep) {
        return viaCep.buscar(CepUtils.normalizar(cep));
    }

    @GET
    @Path("/{cep}/brasilapi")
    @Operation(summary = "Somente BrasilAPI via REST Client declarativo")
    public Endereco brasilApi(@PathParam("cep") String cep) {
        return brasilApi.buscar(CepUtils.normalizar(cep));
    }

    @GET
    @Path("/{cep}/jdk")
    @Operation(summary = "ViaCEP via java.net.http.HttpClient (síncrono)")
    public Endereco jdk(@PathParam("cep") String cep) {
        return jdkHttp.buscar(CepUtils.normalizar(cep));
    }

    @GET
    @Path("/{cep}/jdk-async")
    @Operation(summary = "ViaCEP via java.net.http.HttpClient (assíncrono, sendAsync)")
    public CompletionStage<Endereco> jdkAsync(@PathParam("cep") String cep) {
        return jdkHttp.buscarAsync(CepUtils.normalizar(cep));
    }
}
