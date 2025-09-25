package br.com.fiap.hacka.frameextractorservice.app.queue.impl;

import br.com.fiap.hacka.core.commons.dto.FilePartDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileProcessorTest {

    @Mock
    private FilePartConsumer parent;
    
    @Mock
    private S3Client s3Client;
    
    @Mock
    private S3Presigner s3Presigner;

    private FileProcessor fileProcessor;
    private final String fileName = "test.mp4";
    private final int frameInterval = 5;
    private final String bucketName = "test-bucket";

    @BeforeEach
    void setUp() {
        fileProcessor = new FileProcessor(fileName, parent, frameInterval, s3Client, s3Presigner, bucketName);
    }

    @Test
    void shouldSubmitPart() {
        FilePartDto filePartDto = createFilePartDto();
        
        fileProcessor.submitPart(filePartDto);
        
        // No exception should be thrown
        assertNotNull(fileProcessor);
    }

    @Test
    void shouldGetZipFile() {
        File zipFile = fileProcessor.getZipFile();
        
        assertNotNull(zipFile);
        assertTrue(zipFile.getName().endsWith(".zip"));
        assertTrue(zipFile.getName().contains(fileName));
    }

    @Test
    void shouldCreateZipFileDirectory() {
        File zipFile = fileProcessor.getZipFile();
        
        assertTrue(zipFile.getParentFile().exists());
    }

    private FilePartDto createFilePartDto() {
        FilePartDto dto = new FilePartDto();
        dto.setFileName(fileName);
        dto.setUserName("user1");
        dto.setBytesRead(1024);
        dto.setBytes(new byte[1024]);
        return dto;
    }
}