package br.com.fiap.hacka.frameextractorservice.app.queue;

import br.com.fiap.hacka.core.commons.dto.FilePartDto;

public interface MessageProducer {
    void send(String queueName, FilePartDto filePartDto);
}