package br.com.keidson.cep.exception;

/** CEP com formato válido, mas inexistente -> HTTP 404. */
public class CepNaoEncontradoException extends RuntimeException {

    public CepNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
