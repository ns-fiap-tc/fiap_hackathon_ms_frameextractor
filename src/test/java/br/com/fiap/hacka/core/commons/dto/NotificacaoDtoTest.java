package br.com.fiap.hacka.core.commons.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificacaoDtoTest {

    @Test
    void shouldCreateNotificacaoDto() {
        String mensagem = "Test message";
        String url = "http://test.url";

        NotificacaoDto dto = new NotificacaoDto(mensagem, url);

        assertEquals(mensagem, dto.getMensagem());
        assertEquals(url, dto.getUrl());
    }

    @Test
    void shouldSetAndGetProperties() {
        NotificacaoDto dto = new NotificacaoDto();
        
        dto.setMensagem("Test message");
        dto.setUrl("http://test.url");

        assertEquals("Test message", dto.getMensagem());
        assertEquals("http://test.url", dto.getUrl());
    }

    @Test
    void shouldCreateEmptyNotificacaoDto() {
        NotificacaoDto dto = new NotificacaoDto();
        
        assertNull(dto.getMensagem());
        assertNull(dto.getUrl());
    }
}