package br.com.keidson.cep.service;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import br.com.keidson.cep.exception.CepInvalidoException;
import br.com.keidson.cep.exception.CepNaoEncontradoException;
import br.com.keidson.cep.exception.ServicoIndisponivelException;
import br.com.keidson.cep.model.Endereco;
import br.com.keidson.cep.provider.BrasilApiProvider;
import br.com.keidson.cep.provider.ViaCepProvider;
import br.com.keidson.cep.util.CepUtils;
import io.quarkus.cache.CacheResult;

/**
 * Orquestra a busca: cache -> ViaCEP -> (se falhar) BrasilAPI.
 */
@ApplicationScoped
public class CepService {

    private static final Logger LOG = Logger.getLogger(CepService.class);

    @Inject
    ViaCepProvider viaCep;

    @Inject
    BrasilApiProvider brasilApi;

    /**
     * Resultado fica em cache por CEP (ver quarkus.cache.caffeine."ceps").
     * Exceções NÃO são cacheadas.
     *
     * @param cep já normalizado (8 dígitos), para que "69005-040" e "69005040" usem a mesma chave
     */
    @CacheResult(cacheName = "ceps")
    public Endereco buscar(String cep) {
        String normalizado = CepUtils.normalizar(cep);
        try {
            return viaCep.buscar(normalizado);
        } catch (CepNaoEncontradoException | CepInvalidoException e) {
            throw e; // resposta definitiva: não adianta perguntar a outro provedor
        } catch (RuntimeException e) {
            LOG.warnf("ViaCEP falhou para %s (%s). Usando fallback BrasilAPI.", normalizado, e.toString());
            return buscarNoFallback(normalizado);
        }
    }

    public List<Endereco> buscarPorEndereco(String uf, String cidade, String logradouro) {
        return viaCep.buscarPorEndereco(uf, cidade, logradouro);
    }

    private Endereco buscarNoFallback(String cep) {
        try {
            return brasilApi.buscar(cep);
        } catch (CepNaoEncontradoException | CepInvalidoException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ServicoIndisponivelException("Nenhum provedor de CEP está disponível no momento", e);
        }
    }
}
