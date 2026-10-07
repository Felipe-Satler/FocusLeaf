package br.com.fiap.focusleaf.sessao;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MetricasLeituraTest {

    private static final Instant INICIO = Instant.parse("2026-10-07T10:00:00Z");

    @Test
    void duracaoDeSessaoEncerradaUsaOHorarioDeTermino() {
        Instant fim = INICIO.plusSeconds(300);
        Instant agora = INICIO.plusSeconds(3600);

        assertThat(MetricasLeitura.duracaoSegundos(INICIO, fim, agora)).isEqualTo(300);
    }

    @Test
    void duracaoDeSessaoAbertaContaAteAgora() {
        Instant agora = INICIO.plusSeconds(150);

        assertThat(MetricasLeitura.duracaoSegundos(INICIO, null, agora)).isEqualTo(150);
    }

    @Test
    void velocidadeEmPalavrasPorMinuto() {
        assertThat(MetricasLeitura.velocidadePalavrasPorMinuto(400, 120)).isEqualTo(200);
        assertThat(MetricasLeitura.velocidadePalavrasPorMinuto(400, 0)).isZero();
        assertThat(MetricasLeitura.velocidadePalavrasPorMinuto(0, 120)).isZero();
    }

    @Test
    void percentualArredondaParaBaixoELimitaEm100() {
        assertThat(MetricasLeitura.percentual(2, 3)).isEqualTo(66);
        assertThat(MetricasLeitura.percentual(3, 3)).isEqualTo(100);
        assertThat(MetricasLeitura.percentual(0, 0)).isZero();
    }
}
