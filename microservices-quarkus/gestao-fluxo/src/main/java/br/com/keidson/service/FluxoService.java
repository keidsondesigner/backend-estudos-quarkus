package br.com.keidson.service;

import br.com.keidson.client.NotificacaoClient;
import br.com.keidson.client.WorkerInternoClient;
import br.com.keidson.dto.FluxoRequest;
import br.com.keidson.dto.FluxoResponse;
import br.com.keidson.dto.NotificacaoRequest;
import br.com.keidson.dto.ProcessamentoRequest;
import br.com.keidson.dto.ProcessamentoResponse;
import br.com.keidson.dto.StatusResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import java.util.UUID;

@ApplicationScoped
public class FluxoService {

    @Inject
    @RestClient
    WorkerInternoClient workerClient;

    @Inject
    @RestClient
    NotificacaoClient notificacaoClient;      // ← segundo client, lado a lado

    public FluxoResponse criarFluxo(FluxoRequest request) {
        String idFluxo = UUID.randomUUID().toString();

        // 1. chama o worker
        var procRequest = new ProcessamentoRequest();
        procRequest.idDocumento = request.idDocumento;
        procRequest.nomeArquivo = request.nomeArquivo;
        ProcessamentoResponse resultado = workerClient.processar(procRequest);

        // 2. chama o serviço de notificação
        var notifRequest = new NotificacaoRequest();
        notifRequest.idDocumento = resultado.idDocumento;
        notifRequest.mensagem = "Documento concluído!";
        notificacaoClient.enviar(notifRequest);

        // 3. responde
        var response = new FluxoResponse();
        response.idFluxo = idFluxo;
        response.idDocumento = resultado.idDocumento;
        response.estado = resultado.estado;
        return response;
    }

    public StatusResponse consultarStatus(String idDocumento) {
        return workerClient.consultarStatus(idDocumento);
    }
}