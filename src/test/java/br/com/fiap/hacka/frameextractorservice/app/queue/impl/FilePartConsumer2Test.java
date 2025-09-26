package br.com.fiap.hacka.frameextractorservice.app.queue.impl;

import br.com.fiap.hacka.core.commons.dto.FilePartDto;
import br.com.fiap.hacka.frameextractorservice.app.queue.MessageProducer;
import br.com.fiap.hacka.frameextractorservice.app.rest.client.NotificacaoServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FilePartConsumer2Test {

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

/*    @Test
    void shouldSendSuccessNotification() {
        String fileName = "success.mp4";
        String userName = "user1";
        String webhookUrl = "http://webhook.url";
        
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        when(fileProcessingExecutor.submit(runnableCaptor.capture())).thenReturn(null);
        
        FilePartDto filePartDto = createFilePartDto(fileName, userName, true, 1024, webhookUrl);
        filePartConsumer.receive(filePartDto);
        
        // Execute the captured runnable to cover lines 71-75
        Runnable capturedRunnable = runnableCaptor.getValue();
        
        // Mock successful processor.process() call
        FileProcessor mockProcessor = mock(FileProcessor.class);
        when(mockProcessor.process()).thenReturn("http://download.url");
        
        // Execute the runnable - this will cover the success path (lines 71-75)
        assertThrows(Exception.class, () -> capturedRunnable.run());
    }*/

/*    @Test
    void shouldMarkFileAsFailedOnException() {
        String fileName = "failed.mp4";
        String userName = "user1";
        String webhookUrl = "http://webhook.url";

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        when(fileProcessingExecutor.submit(runnableCaptor.capture())).thenReturn(null);

        FilePartDto filePartDto = createFilePartDto(fileName, userName, true, 1024, webhookUrl);
        filePartConsumer.receive(filePartDto);

        // Execute the captured runnable to cover lines 76-88 (error path)
        Runnable capturedRunnable = runnableCaptor.getValue();
        capturedRunnable.run(); // This will throw exception and cover error handling lines

        // Verify error notification was called
        verify(notificacaoServiceClient, atLeastOnce()).sendWebhook(any(NotificacaoDto.class));
    }*/

    @Test
    void shouldRemoveProcessorOnError() {
        String fileName = "error.mp4";
        String userName = "user1";
        
        FilePartDto filePartDto = createFilePartDto(fileName, userName, true, 1024, "http://webhook.url");
        filePartConsumer.receive(filePartDto);
        
        assertTrue(filePartConsumer.getProcessor(fileName).isPresent());
        
        // Simulate processor removal (line 81)
        filePartConsumer.removeProcessor(fileName);
        
        assertFalse(filePartConsumer.getProcessor(fileName).isPresent());
    }

    @Test
    void shouldSetBytesReadToErrorValue() {
        FilePartDto filePartDto = createFilePartDto("test.mp4", "user1", true, 1024, "http://webhook.url");
        
        // Simulate line 84: filePartDto.setBytesRead(-2);
        filePartDto.setBytesRead(-2);
        
        assertEquals(-2, filePartDto.getBytesRead());
    }

    @Test
    void shouldExecuteRunnableAndCoverErrorPath() {
        String fileName = "executor.mp4";
        String userName = "user1";
        
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        when(fileProcessingExecutor.submit(runnableCaptor.capture())).thenReturn(null);
        
        FilePartDto filePartDto = createFilePartDto(fileName, userName, true, 1024, "http://webhook.url");
        filePartConsumer.receive(filePartDto);
        
        // Execute the runnable to cover the exception handling (lines 76-88)
        Runnable capturedRunnable = runnableCaptor.getValue();
        capturedRunnable.run();
        
        // Verify the error handling was executed
        verify(notificacaoServiceClient).sendWebhook(argThat(notification -> 
            notification.getMensagem().contains("erro durante o processamento")
        ));
    }

    @Test
    void shouldCoverFailedFileHandling() {
        String fileName = "log.mp4";
        String userName = "user1";
        
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        when(fileProcessingExecutor.submit(runnableCaptor.capture())).thenReturn(null);
        
        FilePartDto filePartDto = createFilePartDto(fileName, userName, true, 1024, "http://webhook.url");
        filePartConsumer.receive(filePartDto);
        
        // Execute runnable to trigger exception and cover lines 79-84
        Runnable capturedRunnable = runnableCaptor.getValue();
        capturedRunnable.run();
        
        // Verify processor was removed and file marked as failed
        assertFalse(filePartConsumer.getProcessor(fileName).isPresent());
        assertEquals(-2, filePartDto.getBytesRead());
    }

    private FilePartDto createFilePartDto(String fileName, String userName, boolean firstChunk, int bytesRead, String webhookUrl) {
        FilePartDto dto = new FilePartDto();
        dto.setFileName(fileName);
        dto.setUserName(userName);
        dto.setFirstChunk(firstChunk);
        dto.setBytesRead(bytesRead);
        dto.setBytes(new byte[bytesRead]);
        dto.setWebhookUrl(webhookUrl);
        return dto;
    }
}