package br.com.keidson.cep.provider;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import br.com.keidson.cep.client.viacep.ViaCepClient;
import br.com.keidson.cep.client.viacep.ViaCepResponse;
import br.com.keidson.cep.exception.CepInvalidoException;
import br.com.keidson.cep.exception.CepNaoEncontradoException;
import br.com.keidson.cep.model.Endereco;

/**
 * Usa o REST Client declarativo + resiliência (timeout e retry).
 * Erros de "negócio" (CEP inválido / não encontrado) abortam o retry,
 * pois tentar de novo não vai mudar a resposta.
 */
@ApplicationScoped
public class ViaCepProvider implements CepProvider {

    public static final String NOME = "viacep";

    @Inject
    @RestClient
    ViaCepClient client;

    @Override
    @Timeout(3000)
    @Retry(maxRetries = 2, delay = 200, jitter = 100, abortOn = { CepNaoEncontradoException.class, CepInvalidoException.class })
    public Endereco buscar(String cep) {
        ViaCepResponse resposta = client.buscarPorCep(cep);
        if (resposta == null || resposta.naoEncontrado()) {
            throw new CepNaoEncontradoException("CEP " + cep + " não encontrado");
        }
        return resposta.toEndereco(NOME);
    }

    @Timeout(5000)
    @Retry(maxRetries = 1, delay = 200, jitter = 100, abortOn = CepInvalidoException.class)
    public List<Endereco> buscarPorEndereco(String uf, String cidade, String logradouro) {
        List<ViaCepResponse> resultados = client.buscarPorEndereco(uf.toUpperCase(), cidade, logradouro);
        if (resultados == null) {
            return List.of();
        }
        return resultados.stream()
                .filter(r -> !r.naoEncontrado())
                .map(r -> r.toEndereco(NOME))
                .toList();
    }

    @Override
    public String nome() {
        return NOME;
    }
}
