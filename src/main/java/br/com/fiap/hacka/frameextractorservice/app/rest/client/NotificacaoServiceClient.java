package br.com.fiap.hacka.frameextractorservice.app.rest.client;

import br.com.fiap.hacka.core.commons.dto.NotificacaoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "notificacao-service-client")
public interface NotificacaoServiceClient {

    @PostMapping("/send/webhook")
    void sendWebhook(NotificacaoDto notificacaoDto);
}