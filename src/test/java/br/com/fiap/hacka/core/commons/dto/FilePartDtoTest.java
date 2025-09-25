package br.com.fiap.hacka.core.commons.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FilePartDtoTest {

    @Test
    void shouldCreateFilePartDto() {
        String fileName = "test.mp4";
        String userName = "user1";
        int bytesRead = 1024;
        byte[] bytes = new byte[bytesRead];
        String webhookUrl = "http://test.webhook.url";

        FilePartDto dto = new FilePartDto(fileName, bytesRead, bytes, true, userName, webhookUrl, null, null);

        assertEquals(fileName, dto.getFileName());
        assertEquals(userName, dto.getUserName());
        assertEquals(bytesRead, dto.getBytesRead());
        assertEquals(bytes, dto.getBytes());
        assertTrue(dto.isFirstChunk());
        assertEquals(webhookUrl, dto.getWebhookUrl());
    }

    @Test
    void shouldSetAndGetProperties() {
        FilePartDto dto = new FilePartDto();
        
        dto.setFileName("test.mp4");
        dto.setUserName("user1");
        dto.setBytesRead(1024);
        dto.setBytes(new byte[1024]);
        dto.setFirstChunk(true);
        dto.setWebhookUrl("http://test.webhook.url");
        dto.setFrameFilePath("/tmp/frames");
        dto.setFileUrl("http://file.url");

        assertEquals("test.mp4", dto.getFileName());
        assertEquals("user1", dto.getUserName());
        assertEquals(1024, dto.getBytesRead());
        assertNotNull(dto.getBytes());
        assertTrue(dto.isFirstChunk());
        assertEquals("http://test.webhook.url", dto.getWebhookUrl());
        assertEquals("/tmp/frames", dto.getFrameFilePath());
        assertEquals("http://file.url", dto.getFileUrl());
    }

    @Test
    void shouldHaveCorrectChunkSize() {
        assertEquals(1024 * 1024, FilePartDto.CHUNK_SIZE);
    }
}