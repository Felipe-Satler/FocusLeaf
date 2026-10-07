package br.com.fiap.focusleaf.sessao;

import java.time.Instant;

/**
 * Registro de um bloco lido até o fim dentro de uma sessão.
 */
public class BlocoConcluido {

    private int ordem;
    private Instant concluidoEm;

    public BlocoConcluido() {
    }

    public BlocoConcluido(int ordem, Instant concluidoEm) {
        this.ordem = ordem;
        this.concluidoEm = concluidoEm;
    }

    public int getOrdem() {
        return ordem;
    }

    public void setOrdem(int ordem) {
        this.ordem = ordem;
    }

    public Instant getConcluidoEm() {
        return concluidoEm;
    }

    public void setConcluidoEm(Instant concluidoEm) {
        this.concluidoEm = concluidoEm;
    }
}
