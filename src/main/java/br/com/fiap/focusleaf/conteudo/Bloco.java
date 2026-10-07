package br.com.fiap.focusleaf.conteudo;

/**
 * Trecho de um conteúdo. Fica embutido no documento do Conteudo, no MongoDB.
 * A ordem começa em 1 e é atribuída automaticamente pelo serviço.
 */
public class Bloco {

    private int ordem;
    private String titulo;
    private String texto;

    public Bloco() {
    }

    public Bloco(int ordem, String titulo, String texto) {
        this.ordem = ordem;
        this.titulo = titulo;
        this.texto = texto;
    }

    public int getOrdem() {
        return ordem;
    }

    public void setOrdem(int ordem) {
        this.ordem = ordem;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }
}
