package br.com.fiap.focusleaf.erro;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converte exceções em respostas JSON padronizadas (ErroResponse).
 */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> recursoNaoEncontrado(RecursoNaoEncontradoException ex,
                                                             HttpServletRequest requisicao) {
        return resposta(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage(), requisicao, null);
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErroResponse> conflito(ConflitoException ex, HttpServletRequest requisicao) {
        return resposta(HttpStatus.CONFLICT, "Conflito", ex.getMessage(), requisicao, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroResponse> parametroAusente(MissingServletRequestParameterException ex,
                                                         HttpServletRequest requisicao) {
        return resposta(HttpStatus.BAD_REQUEST, "Requisição inválida",
                "O parâmetro '" + ex.getParameterName() + "' é obrigatório.", requisicao, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> dadosInvalidos(MethodArgumentNotValidException ex,
                                                       HttpServletRequest requisicao) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(erro -> campos.putIfAbsent(erro.getField(), erro.getDefaultMessage()));

        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Um ou mais campos estão inválidos.", requisicao, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException ex,
                                                      HttpServletRequest requisicao) {
        return resposta(HttpStatus.BAD_REQUEST, "Requisição inválida",
                "O corpo da requisição está ausente ou não é um JSON válido.", requisicao, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tipoInvalido(MethodArgumentTypeMismatchException ex,
                                                     HttpServletRequest requisicao) {
        return resposta(HttpStatus.BAD_REQUEST, "Requisição inválida",
                "Valor inválido para o parâmetro '" + ex.getName() + "'.", requisicao, null);
    }

    private ResponseEntity<ErroResponse> resposta(HttpStatus status, String erro, String mensagem,
                                                  HttpServletRequest requisicao, Map<String, String> campos) {
        ErroResponse corpo = new ErroResponse(
                status.value(),
                erro,
                mensagem,
                requisicao.getRequestURI(),
                Instant.now(),
                campos
        );
        return ResponseEntity.status(status).body(corpo);
    }
}
