package br.com.fiap.focusleaf.conteudo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BlocoRequest(

        @Size(max = 200, message = "O título do bloco deve ter no máximo 200 caracteres")
        String titulo,

        @NotBlank(message = "O texto do bloco é obrigatório")
        @Size(max = 10000, message = "O texto do bloco deve ter no máximo 10000 caracteres")
        String texto
) {
}
