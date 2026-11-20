package br.com.keidson.cep.provider;

import br.com.keidson.cep.model.Endereco;

/** Contrato comum para qualquer fonte de CEP. */
public interface CepProvider {

    /**
     * @param cep somente os 8 dígitos (já normalizado)
     */
    Endereco buscar(String cep);

    String nome();
}
