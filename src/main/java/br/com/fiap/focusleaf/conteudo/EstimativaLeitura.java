package br.com.fiap.focusleaf.conteudo;

/**
 * Cálculos simples de tamanho e tempo de leitura.
 * Usa 200 palavras por minuto, um ritmo conservador para leitura de estudo.
 */
public final class EstimativaLeitura {

    public static final int PALAVRAS_POR_MINUTO = 200;

    private EstimativaLeitura() {
    }

    public static int contarPalavras(String texto) {
        if (texto == null || texto.isBlank()) {
            return 0;
        }
        return texto.strip().split("\\s+").length;
    }

    public static int minutosEstimados(int palavras) {
        if (palavras <= 0) {
            return 0;
        }
        return (int) Math.ceil(palavras / (double) PALAVRAS_POR_MINUTO);
    }
}
