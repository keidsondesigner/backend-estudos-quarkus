package br.com.keidson.cep.provider;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.keidson.cep.client.viacep.ViaCepResponse;
import br.com.keidson.cep.exception.CepInvalidoException;
import br.com.keidson.cep.exception.CepNaoEncontradoException;
import br.com.keidson.cep.exception.ServicoIndisponivelException;
import br.com.keidson.cep.model.Endereco;

/**
 * Abordagem "MANUAL" com java.net.http.HttpClient (JDK 11+), sem REST Client.
 * Serve para comparar: aqui você monta URL, request, trata status e faz o parse do JSON na mão.
 */
@ApplicationScoped
public class JdkHttpCepProvider implements CepProvider {

    public static final String NOME = "jdk-httpclient";

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final String baseUrl;
    private final Duration timeout;

    @Inject
    public JdkHttpCepProvider(
            ObjectMapper mapper,
            @ConfigProperty(name = "cep.jdk.base-url", defaultValue = "https://viacep.com.br") String baseUrl,
            @ConfigProperty(name = "cep.jdk.timeout", defaultValue = "PT3S") Duration timeout) {
        this.mapper = mapper;
        this.baseUrl = baseUrl;
        this.timeout = timeout;
        // HttpClient é thread-safe e caro de criar: crie UMA vez e reutilize.
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                // Respeita -Dhttps.proxyHost/-Dhttps.proxyPort (útil em rede corporativa)
                .proxy(ProxySelector.getDefault())
                .build();
    }

    /** Versão síncrona (bloqueante). */
    @Override
    public Endereco buscar(String cep) {
        try {
            HttpResponse<String> resposta = httpClient.send(montarRequest(cep), HttpResponse.BodyHandlers.ofString());
            return converter(cep, resposta);
        } catch (IOException e) {
            throw new ServicoIndisponivelException("Falha ao chamar o ViaCEP: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServicoIndisponivelException("Chamada ao ViaCEP interrompida", e);
        }
    }

    /** Versão assíncrona (não bloqueante) com sendAsync. */
    public CompletionStage<Endereco> buscarAsync(String cep) {
        return httpClient.sendAsync(montarRequest(cep), HttpResponse.BodyHandlers.ofString())
                .thenApply(resposta -> converter(cep, resposta));
    }

    private HttpRequest montarRequest(String cep) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/ws/" + cep + "/json/"))
                .timeout(timeout)
                .header("Accept", "application/json")
                .GET()
                .build();
    }

    private Endereco converter(String cep, HttpResponse<String> resposta) {
        int status = resposta.statusCode();
        if (status == 400) {
            throw new CepInvalidoException("CEP inválido: " + cep);
        }
        if (status < 200 || status >= 300) {
            throw new ServicoIndisponivelException("ViaCEP respondeu HTTP " + status);
        }
        try {
            ViaCepResponse body = mapper.readValue(resposta.body(), ViaCepResponse.class);
            if (body.naoEncontrado()) {
                throw new CepNaoEncontradoException("CEP " + cep + " não encontrado");
            }
            return body.toEndereco(NOME);
        } catch (JsonProcessingException e) {
            throw new ServicoIndisponivelException("Resposta inválida do ViaCEP", e);
        }
    }

    @Override
    public String nome() {
        return NOME;
    }
}
