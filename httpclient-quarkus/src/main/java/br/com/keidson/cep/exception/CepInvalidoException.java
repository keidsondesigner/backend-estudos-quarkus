package br.com.keidson.cep.exception;

/** CEP com formato inválido -> HTTP 400. */
public class CepInvalidoException extends RuntimeException {

    public CepInvalidoException(String mensagem) {
        super(mensagem);
    }
}
