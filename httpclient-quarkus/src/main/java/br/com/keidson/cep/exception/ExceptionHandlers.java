package br.com.keidson.cep.exception;

import java.util.concurrent.CompletionException;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response.Status;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Mapeadores globais de exceção -> resposta HTTP (equivalente ao @ControllerAdvice do Spring).
 */
public class ExceptionHandlers {

    private static final Logger LOG = Logger.getLogger(ExceptionHandlers.class);

    @ServerExceptionMapper
    public RestResponse<ErroResponse> cepInvalido(CepInvalidoException e) {
        return resposta(Status.BAD_REQUEST, e.getMessage());
    }

    @ServerExceptionMapper
    public RestResponse<ErroResponse> cepNaoEncontrado(CepNaoEncontradoException e) {
        return resposta(Status.NOT_FOUND, e.getMessage());
    }

    @ServerExceptionMapper
    public RestResponse<ErroResponse> servicoIndisponivel(ServicoIndisponivelException e) {
        LOG.error("Serviço externo indisponível", e);
        return resposta(Status.SERVICE_UNAVAILABLE, e.getMessage());
    }

    /** Estourou o @Timeout do Fault Tolerance. */
    @ServerExceptionMapper
    public RestResponse<ErroResponse> timeout(TimeoutException e) {
        return resposta(Status.GATEWAY_TIMEOUT, "A API externa demorou demais para responder");
    }

    /** Erro de rede do REST Client (DNS, conexão recusada, read timeout...). */
    @ServerExceptionMapper
    public RestResponse<ErroResponse> processing(ProcessingException e) {
        LOG.error("Falha de comunicação com API externa", e);
        return resposta(Status.SERVICE_UNAVAILABLE, "Falha de comunicação com a API externa: " + e.getMessage());
    }

    /** Endpoints assíncronos (CompletionStage) embrulham a exceção real em CompletionException. */
    @ServerExceptionMapper
    public RestResponse<ErroResponse> completion(CompletionException e) {
        Throwable causa = e.getCause();
        if (causa instanceof CepInvalidoException c) {
            return cepInvalido(c);
        }
        if (causa instanceof CepNaoEncontradoException c) {
            return cepNaoEncontrado(c);
        }
        if (causa instanceof ServicoIndisponivelException c) {
            return servicoIndisponivel(c);
        }
        LOG.error("Erro inesperado", e);
        return resposta(Status.INTERNAL_SERVER_ERROR, "Erro inesperado");
    }

    private static RestResponse<ErroResponse> resposta(Status status, String mensagem) {
        return RestResponse.status(status, ErroResponse.of(status.getStatusCode(), status.getReasonPhrase(), mensagem));
    }
}
