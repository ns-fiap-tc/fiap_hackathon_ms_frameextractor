package br.com.fiap.hacka.frameextractorservice.app.queue.impl;

import br.com.fiap.hacka.core.commons.dto.FilePartDto;
import br.com.fiap.hacka.frameextractorservice.app.queue.MessageProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class RabbitMqMessageProducer implements MessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public void send(String queueName, FilePartDto filePartDto) {
        rabbitTemplate.convertAndSend(queueName, filePartDto);
    }
}