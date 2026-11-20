package br.com.keidson.cep.client.brasilapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import br.com.keidson.cep.model.Endereco;
import br.com.keidson.cep.util.CepUtils;

/** Resposta de https://brasilapi.com.br/api/cep/v1/{cep} */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BrasilApiResponse(
        String cep,
        String state,
        String city,
        String neighborhood,
        String street,
        String service) {

    public Endereco toEndereco(String fonte) {
        return new Endereco(
                CepUtils.formatar(cep),
                street,
                null,
                neighborhood,
                city,
                state,
                null,
                null,
                fonte);
    }
}
