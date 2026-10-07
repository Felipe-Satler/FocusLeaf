package br.com.fiap.focusleaf.info;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InfoControllerTest {

    @Test
    void deveRetornarIdentificacaoDaInstancia() {
        var controller = new InfoController("focusleaf", "staging", "0.1.0", "abc1234");

        InfoResponse resposta = controller.info();

        assertThat(resposta.aplicacao()).isEqualTo("focusleaf");
        assertThat(resposta.ambiente()).isEqualTo("staging");
        assertThat(resposta.versao()).isEqualTo("0.1.0");
        assertThat(resposta.commit()).isEqualTo("abc1234");
    }
}
