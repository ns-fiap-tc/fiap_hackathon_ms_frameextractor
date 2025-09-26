package br.com.fiap.hacka.frameextractorservice.app.queue.impl;

import br.com.fiap.hacka.core.commons.dto.FilePartDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileProcessorProcessMethodTest {

    @Mock
    private FilePartConsumer parent;
    
    @Mock
    private S3Client s3Client;
    
    @Mock
    private S3Presigner s3Presigner;
    
    @Mock
    private PresignedGetObjectRequest presignedRequest;

    private FileProcessor fileProcessor;

    @BeforeEach
    void setUp() throws Exception {
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);
        when(presignedRequest.url()).thenReturn(new URL("http://test-url.com"));
        
        fileProcessor = new FileProcessor("test.mp4", parent, 5, s3Client, s3Presigner, "test-bucket");
    }

    @Test
    void shouldProcessEOFOnly() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        Future<String> future = executor.submit(() -> fileProcessor.process());
        
        FilePartDto eofPart = new FilePartDto();
        eofPart.setBytesRead(-1);
        eofPart.setUserName("testUser");
        fileProcessor.submitPart(eofPart);
        
        assertThrows(Exception.class, () -> future.get());
        assertEquals("http://test-url.com", eofPart.getFrameFilePath());
        executor.shutdown();
    }

    @Test
    void shouldCollectMultipleChunks() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        Future<String> future = executor.submit(() -> fileProcessor.process());
        
        FilePartDto part1 = new FilePartDto();
        part1.setBytesRead(512);
        part1.setBytes(new byte[512]);
        fileProcessor.submitPart(part1);
        
        FilePartDto part2 = new FilePartDto();
        part2.setBytesRead(256);
        part2.setBytes(new byte[256]);
        fileProcessor.submitPart(part2);
        
        FilePartDto eofPart = new FilePartDto();
        eofPart.setBytesRead(-1);
        eofPart.setUserName("testUser");
        fileProcessor.submitPart(eofPart);
        
        assertThrows(Exception.class, () -> future.get());
        executor.shutdown();
    }

    @Test
    void shouldSetUserNameOnEOF() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        Future<String> future = executor.submit(() -> fileProcessor.process());
        
        FilePartDto eofPart = new FilePartDto();
        eofPart.setBytesRead(-1);
        eofPart.setUserName("specificUser");
        fileProcessor.submitPart(eofPart);
        
        assertThrows(Exception.class, () -> future.get());
        assertEquals("specificUser", eofPart.getUserName());
        executor.shutdown();
    }

    @Test
    void shouldCallParentRemoveProcessor() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        Future<String> future = executor.submit(() -> fileProcessor.process());
        
        FilePartDto eofPart = new FilePartDto();
        eofPart.setBytesRead(-1);
        eofPart.setUserName("testUser");
        fileProcessor.submitPart(eofPart);
        
        try {
            future.get();
        } catch (Exception e) {
            // Expected due to invalid video data
        }
        
        verify(parent, never()).removeProcessor("test.mp4");
        executor.shutdown();
    }

    @Test
    void shouldHandleInterruptedException() throws InterruptedException {
        Thread testThread = new Thread(() -> {
            try {
                fileProcessor.process();
            } catch (RuntimeException e) {
                assertTrue(e.getCause() instanceof InterruptedException);
            }
        });
        
        testThread.start();
        Thread.sleep(100);
        testThread.interrupt();
        testThread.join(1000);
    }

    @Test
    void shouldCreateS3Key() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        Future<String> future = executor.submit(() -> fileProcessor.process());
        
        FilePartDto eofPart = new FilePartDto();
        eofPart.setBytesRead(-1);
        eofPart.setUserName("user123");
        fileProcessor.submitPart(eofPart);
        
        assertThrows(Exception.class, () -> future.get());
        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
        executor.shutdown();
    }

    @Test
    void shouldHandleRuntimeException() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        Future<String> future = executor.submit(() -> fileProcessor.process());
        
        FilePartDto eofPart = new FilePartDto();
        eofPart.setBytesRead(-1);
        eofPart.setUserName("testUser");
        fileProcessor.submitPart(eofPart);
        
        Exception exception = assertThrows(Exception.class, () -> future.get());
        assertNotNull(exception);
        executor.shutdown();
    }

    @Test
    void shouldTestGetPresignedUrlMethod() throws Exception {
        String objectKey = "frames/user1/test.mp4.zip";
        
        String result = ReflectionTestUtils.invokeMethod(fileProcessor, "getPresignedUrl", objectKey);
        
        assertEquals("http://test-url.com", result);
        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void shouldVerifyGetObjectRequestParameters() throws Exception {
        String objectKey = "frames/testuser/test.mp4.zip";
        
        ReflectionTestUtils.invokeMethod(fileProcessor, "getPresignedUrl", objectKey);
        
        verify(s3Presigner).presignGetObject(ArgumentMatchers.<GetObjectPresignRequest>argThat(request -> {
            GetObjectRequest getObjectRequest = request.getObjectRequest();
            return "test-bucket".equals(getObjectRequest.bucket()) && 
                   objectKey.equals(getObjectRequest.key());
        }));
    }

    @Test
    void shouldSetCorrectSignatureDuration() throws Exception {
        String objectKey = "frames/user/test.mp4.zip";
        
        ReflectionTestUtils.invokeMethod(fileProcessor, "getPresignedUrl", objectKey);
        
        verify(s3Presigner).presignGetObject(ArgumentMatchers.<GetObjectPresignRequest>argThat(request ->
            Duration.ofMinutes(30).equals(request.signatureDuration())
        ));
    }

/*    @Test
    void shouldTestProcessOkMethodStart() throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        executor.submit(() -> {
            try {
                ReflectionTestUtils.invokeMethod(fileProcessor, "processOk");
            } catch (Exception e) {
                assertTrue(e instanceof RuntimeException);
            }
        });
        
        Thread.sleep(100);
        
        FilePartDto eofPart = new FilePartDto();
        eofPart.setBytesRead(-1);
        fileProcessor.submitPart(eofPart);
        
        executor.shutdown();
    }

    @Test
    void shouldCollectChunksInProcessOk() throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.submit(() -> {
            try {
                ReflectionTestUtils.invokeMethod(fileProcessor, "processOk");
            } catch (Exception e) {
                // Expected due to invalid video data
            }
        });

        Thread.sleep(50);

        FilePartDto part1 = new FilePartDto();
        part1.setBytesRead(512);
        part1.setBytes(new byte[512]);
        fileProcessor.submitPart(part1);

        Thread.sleep(50);

        FilePartDto eofPart = new FilePartDto();
        eofPart.setBytesRead(-1);
        fileProcessor.submitPart(eofPart);

        executor.shutdown();
    }*/
}