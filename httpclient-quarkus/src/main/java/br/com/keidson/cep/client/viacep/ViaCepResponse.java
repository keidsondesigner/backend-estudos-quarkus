package br.com.keidson.cep.client.viacep;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import br.com.keidson.cep.model.Endereco;
import br.com.keidson.cep.util.CepUtils;

/**
 * Resposta de https://viacep.com.br/ws/{cep}/json/
 * Quando o CEP não existe, o ViaCEP responde HTTP 200 com {"erro": "true"}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ViaCepResponse(
        String cep,
        String logradouro,
        String complemento,
        String unidade,
        String bairro,
        String localidade,
        String uf,
        String estado,
        String regiao,
        String ibge,
        String gia,
        String ddd,
        String siafi,
        Boolean erro) {

    public boolean naoEncontrado() {
        return Boolean.TRUE.equals(erro);
    }

    public Endereco toEndereco(String fonte) {
        return new Endereco(
                CepUtils.formatar(cep),
                vazioParaNull(logradouro),
                vazioParaNull(complemento),
                vazioParaNull(bairro),
                localidade,
                uf,
                vazioParaNull(ibge),
                vazioParaNull(ddd),
                fonte);
    }

    private static String vazioParaNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
