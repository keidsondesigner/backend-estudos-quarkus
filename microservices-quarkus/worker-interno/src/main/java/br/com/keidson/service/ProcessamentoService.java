package br.com.keidson.service;

import br.com.keidson.dto.ProcessamentoRequest;
import br.com.keidson.dto.ProcessamentoResponse;
import br.com.keidson.dto.StatusResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ProcessamentoService {

    // "banco de dados" em memória — no real, seria o MongoDB
    private final Map<String, ProcessamentoResponse> processados = new ConcurrentHashMap<>();

    public ProcessamentoResponse processar(ProcessamentoRequest request) {
        var response = new ProcessamentoResponse();
        response.idDocumento = request.idDocumento;
        response.estado = "DOCUMENTO_CONCLUIDO";
        response.processadoEm = LocalDateTime.now().toString();

        processados.put(request.idDocumento, response);
        return response;
    }

    public StatusResponse consultarStatus(String idDocumento) {
        ProcessamentoResponse processado = processados.get(idDocumento);

        if (processado == null) {
            throw new NotFoundException(
                "Documento não encontrado: " + idDocumento);
        }

        var status = new StatusResponse();
        status.idDocumento = processado.idDocumento;
        status.estado = processado.estado;
        status.processadoEm = processado.processadoEm;
        return status;
    }
}