package br.com.fiap.focusleaf.sessao;

import java.time.Duration;
import java.time.Instant;

/**
 * Cálculos das métricas de foco a partir dos dados registrados nas sessões.
 */
public final class MetricasLeitura {

    private MetricasLeitura() {
    }

    /**
     * Duração da sessão em segundos. Para sessões abertas, conta até o instante "agora".
     */
    public static long duracaoSegundos(Instant inicio, Instant fim, Instant agora) {
        if (inicio == null) {
            return 0;
        }
        Instant termino = (fim != null) ? fim : agora;
        return Math.max(0, Duration.between(inicio, termino).getSeconds());
    }

    public static int velocidadePalavrasPorMinuto(int palavras, long segundos) {
        if (palavras <= 0 || segundos <= 0) {
            return 0;
        }
        return (int) Math.round(palavras * 60.0 / segundos);
    }

    public static int percentual(int concluidos, int total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.min(100, (concluidos * 100L) / total);
    }
}
