package br.com.fiap.hacka.core.commons.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class FilePartDto {

    public static final int CHUNK_SIZE = 1024 * 1024; // 1 MB

    private String fileName;
    private int bytesRead;
    private byte[] bytes;
    boolean firstChunk;
    private String userName;
    private String webhookUrl;
    private String frameFilePath;
    private String fileUrl;
}