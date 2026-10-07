package br.com.fiap.focusleaf.conteudo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EstimativaLeituraTest {

    @Test
    void deveContarPalavrasIgnorandoEspacosExtrasEQuebrasDeLinha() {
        assertThat(EstimativaLeitura.contarPalavras("  um   dois\ntrês\t quatro  ")).isEqualTo(4);
    }

    @Test
    void textoVazioOuNuloTemZeroPalavras() {
        assertThat(EstimativaLeitura.contarPalavras(null)).isZero();
        assertThat(EstimativaLeitura.contarPalavras("   ")).isZero();
    }

    @Test
    void deveArredondarTempoEstimadoParaCima() {
        assertThat(EstimativaLeitura.minutosEstimados(0)).isZero();
        assertThat(EstimativaLeitura.minutosEstimados(1)).isEqualTo(1);
        assertThat(EstimativaLeitura.minutosEstimados(200)).isEqualTo(1);
        assertThat(EstimativaLeitura.minutosEstimados(201)).isEqualTo(2);
    }
}
