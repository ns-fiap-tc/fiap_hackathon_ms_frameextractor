package br.com.fiap.hacka.frameextractorservice.app.queue.impl;

import br.com.fiap.hacka.core.commons.dto.FilePartDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitMqMessageProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private RabbitMqMessageProducer messageProducer;

    @BeforeEach
    void setUp() {
        messageProducer = new RabbitMqMessageProducer(rabbitTemplate);
    }

    @Test
    void shouldSendMessage() {
        String queueName = "test-queue";
        FilePartDto filePartDto = createFilePartDto();

        messageProducer.send(queueName, filePartDto);

        verify(rabbitTemplate).convertAndSend(queueName, filePartDto);
    }

    private FilePartDto createFilePartDto() {
        FilePartDto dto = new FilePartDto();
        dto.setFileName("test.mp4");
        dto.setUserName("user1");
        dto.setBytesRead(1024);
        dto.setBytes(new byte[1024]);
        return dto;
    }
}