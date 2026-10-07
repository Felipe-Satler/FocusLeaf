package br.com.fiap.focusleaf.erro;

/**
 * Lançada quando um conteúdo, bloco ou sessão não existe. Vira HTTP 404.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
