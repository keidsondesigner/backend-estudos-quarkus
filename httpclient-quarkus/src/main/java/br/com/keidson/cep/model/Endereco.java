package br.com.keidson.cep.model;

/**
 * Modelo de domínio único, independente da API de origem.
 * O campo "fonte" mostra qual provedor respondeu (viacep, brasilapi, jdk-httpclient).
 */
public record Endereco(
        String cep,
        String logradouro,
        String complemento,
        String bairro,
        String cidade,
        String uf,
        String ibge,
        String ddd,
        String fonte) {
}
