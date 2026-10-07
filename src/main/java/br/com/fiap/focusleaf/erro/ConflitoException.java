package br.com.fiap.focusleaf.erro;

/**
 * Lançada quando a operação conflita com o estado atual (ex.: sessão já encerrada). Vira HTTP 409.
 */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
