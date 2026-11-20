package br.com.keidson.cep.exception;

import java.time.Instant;

/** Corpo padrão de erro devolvido pela API. */
public record ErroResponse(int status, String erro, String mensagem, Instant timestamp) {

    public static ErroResponse of(int status, String erro, String mensagem) {
        return new ErroResponse(status, erro, mensagem, Instant.now());
    }
}
