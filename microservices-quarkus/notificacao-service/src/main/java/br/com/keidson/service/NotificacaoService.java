package br.com.keidson.service;

import br.com.keidson.dto.NotificacaoRequest;
import br.com.keidson.dto.NotificacaoResponse;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class NotificacaoService {

    public NotificacaoResponse enviar(NotificacaoRequest request) {
        // no mundo real: enviaria e-mail, push, SMS, mensagem no app do banco...
        // no lab: simula o envio logando no console
        System.out.println("📧 Notificação enviada | documento="
                + request.idDocumento + " | mensagem=" + request.mensagem);

        var response = new NotificacaoResponse();
        response.idNotificacao = UUID.randomUUID().toString();
        response.idDocumento = request.idDocumento;
        response.status = "ENVIADA";
        response.enviadaEm = LocalDateTime.now().toString();
        return response;
    }
}