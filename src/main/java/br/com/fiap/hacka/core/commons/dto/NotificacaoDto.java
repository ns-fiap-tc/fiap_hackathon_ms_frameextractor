package br.com.fiap.hacka.core.commons.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class NotificacaoDto {
    @NotNull private String mensagem;
    @NotNull private String url;
}