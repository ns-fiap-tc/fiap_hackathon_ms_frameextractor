package br.com.fiap.hacka.frameextractorservice.app.queue.impl;

import br.com.fiap.hacka.core.commons.dto.FilePartDto;
import br.com.fiap.hacka.frameextractorservice.app.queue.MessageProducer;
import br.com.fiap.hacka.frameextractorservice.app.rest.client.NotificacaoServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FilePartConsumerTest {

    @Mock
    private ExecutorService fileProcessingExecutor;
    
    @Mock
    private MessageProducer messageProducer;
    
    @Mock
    private NotificacaoServiceClient notificacaoServiceClient;
    
    @Mock
    private S3Client s3Client;
    
    @Mock
    private S3Presigner s3Presigner;

    private FilePartConsumer filePartConsumer;

    @BeforeEach
    void setUp() {
        filePartConsumer = new FilePartConsumer(
            fileProcessingExecutor,
            messageProducer,
            notificacaoServiceClient,
            s3Client,
            s3Presigner
        );
        
        ReflectionTestUtils.setField(filePartConsumer, "queueName", "test-queue");
        ReflectionTestUtils.setField(filePartConsumer, "frameInterval", 5);
        ReflectionTestUtils.setField(filePartConsumer, "bucketName", "test-bucket");
    }

    @Test
    void shouldProcessFirstChunk() {
        FilePartDto filePartDto = createFilePartDto("test.mp4", "user1", true, 1024);
        
        filePartConsumer.receive(filePartDto);
        
        verify(messageProducer).send(eq("test-queue"), eq(filePartDto));
        verify(fileProcessingExecutor).submit(any(Runnable.class));
    }

    @Test
    void shouldSkipChunkForFailedFile() {
        String fileName = "failed.mp4";
        String userName = "user1";
        
        FilePartDto firstChunk = createFilePartDto(fileName, userName, true, 1024);
        firstChunk.setBytesRead(-2);
        
        filePartConsumer.receive(firstChunk);
        
        FilePartDto secondChunk = createFilePartDto(fileName, userName, false, 1024);
        
        filePartConsumer.receive(secondChunk);
        
        verify(messageProducer, times(2)).send(any(), any());
    }

    @Test
    void shouldRemoveFailedFileOnFirstChunk() {
        String fileName = "retry.mp4";
        String userName = "user1";
        
        FilePartDto failedChunk = createFilePartDto(fileName, userName, false, 1024);
        failedChunk.setBytesRead(-2);
        filePartConsumer.receive(failedChunk);
        
        FilePartDto firstChunk = createFilePartDto(fileName, userName, true, 1024);
        
        filePartConsumer.receive(firstChunk);
        
        verify(messageProducer, times(2)).send(any(), any());
    }

    @Test
    void shouldGetProcessor() {
        String fileName = "test.mp4";
        FilePartDto filePartDto = createFilePartDto(fileName, "user1", true, 1024);
        
        filePartConsumer.receive(filePartDto);
        
        assertTrue(filePartConsumer.getProcessor(fileName).isPresent());
    }

    @Test
    void shouldRemoveProcessor() {
        String fileName = "test.mp4";
        FilePartDto filePartDto = createFilePartDto(fileName, "user1", true, 1024);
        
        filePartConsumer.receive(filePartDto);
        assertTrue(filePartConsumer.getProcessor(fileName).isPresent());
        
        filePartConsumer.removeProcessor(fileName);
        assertFalse(filePartConsumer.getProcessor(fileName).isPresent());
    }

    private FilePartDto createFilePartDto(String fileName, String userName, boolean firstChunk, int bytesRead) {
        FilePartDto dto = new FilePartDto();
        dto.setFileName(fileName);
        dto.setUserName(userName);
        dto.setFirstChunk(firstChunk);
        dto.setBytesRead(bytesRead);
        dto.setBytes(new byte[bytesRead]);
        dto.setWebhookUrl("http://test.webhook.url");
        return dto;
    }
}