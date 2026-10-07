package br.com.fiap.focusleaf.conteudo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Dados para cadastrar ou substituir um conteúdo.
 * Os blocos são numerados automaticamente na ordem em que chegam (1, 2, 3...).
 */
public record ConteudoRequest(

        @NotBlank(message = "O título é obrigatório")
        @Size(max = 200, message = "O título deve ter no máximo 200 caracteres")
        String titulo,

        @Size(max = 120, message = "O autor deve ter no máximo 120 caracteres")
        String autor,

        @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres")
        String descricao,

        @NotEmpty(message = "O conteúdo precisa ter pelo menos um bloco")
        @Size(max = 300, message = "O conteúdo pode ter no máximo 300 blocos")
        List<@NotNull(message = "O bloco não pode ser nulo") @Valid BlocoRequest> blocos
) {
}
