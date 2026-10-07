package br.com.fiap.focusleaf.sessao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * O leitor é identificado por um apelido (não há login no MVP).
 * O apelido é guardado em minúsculas, então "Ana" e "ana" são o mesmo leitor.
 */
public record IniciarSessaoRequest(

        @NotBlank(message = "O conteúdo é obrigatório")
        String conteudoId,

        @NotBlank(message = "O leitor é obrigatório")
        @Size(max = 60, message = "O leitor deve ter no máximo 60 caracteres")
        String leitor
) {
}
