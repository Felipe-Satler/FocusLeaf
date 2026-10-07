package br.com.fiap.focusleaf.erro;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Formato padrão das respostas de erro da API.
 * "campos" só aparece em erros de validação.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(
        int status,
        String erro,
        String mensagem,
        String caminho,
        Instant dataHora,
        Map<String, String> campos
) {
}
