package br.com.keidson.cep.provider;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import br.com.keidson.cep.client.brasilapi.BrasilApiClient;
import br.com.keidson.cep.exception.CepInvalidoException;
import br.com.keidson.cep.exception.CepNaoEncontradoException;
import br.com.keidson.cep.model.Endereco;

@ApplicationScoped
public class BrasilApiProvider implements CepProvider {

    public static final String NOME = "brasilapi";

    @Inject
    @RestClient
    BrasilApiClient client;

    @Override
    @Timeout(5000)
    @Retry(maxRetries = 1, delay = 200, jitter = 100, abortOn = { CepNaoEncontradoException.class, CepInvalidoException.class })
    public Endereco buscar(String cep) {
        return client.buscarPorCep(cep).toEndereco(NOME);
    }

    @Override
    public String nome() {
        return NOME;
    }
}
