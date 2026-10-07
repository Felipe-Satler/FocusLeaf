package br.com.fiap.focusleaf.sessao;

import br.com.fiap.focusleaf.erro.ConflitoException;
import br.com.fiap.focusleaf.sessao.dto.IniciarSessaoRequest;
import br.com.fiap.focusleaf.sessao.dto.ProgressoResponse;
import br.com.fiap.focusleaf.sessao.dto.SessaoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa só a camada HTTP de sessões e progresso: o serviço é simulado, então o banco não é usado.
 */
@WebMvcTest({SessaoController.class, ProgressoController.class})
class SessaoControllerTest {

    private static final Instant INICIO = Instant.parse("2026-10-07T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SessaoService service;

    @Test
    void iniciarSessaoDeveRetornar201ComLocation() throws Exception {
        when(service.iniciar(any(IniciarSessaoRequest.class)))
                .thenReturn(sessao(StatusSessao.EM_ANDAMENTO, List.of(), 1, null));

        mockMvc.perform(post("/api/sessoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "conteudoId": "c1", "leitor": "ana" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/sessoes/s1")))
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.blocoAtual").value(1));
    }

    @Test
    void iniciarSemLeitorDeveRetornar400() throws Exception {
        mockMvc.perform(post("/api/sessoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "conteudoId": "c1" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.leitor").exists());

        verifyNoInteractions(service);
    }

    @Test
    void iniciarComSessaoJaAbertaDeveRetornar409() throws Exception {
        when(service.iniciar(any(IniciarSessaoRequest.class)))
                .thenThrow(new ConflitoException("Sessão em andamento já existe."));

        mockMvc.perform(post("/api/sessoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "conteudoId": "c1", "leitor": "ana" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void concluirBlocoDeveRetornarSessaoAtualizada() throws Exception {
        when(service.concluirBloco("s1", 1))
                .thenReturn(sessao(StatusSessao.EM_ANDAMENTO, List.of(1), 2, null));

        mockMvc.perform(post("/api/sessoes/s1/blocos/1/concluir"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocosConcluidos[0]").value(1))
                .andExpect(jsonPath("$.blocoAtual").value(2));
    }

    @Test
    void abandonarDeveRetornarPontoDeAbandono() throws Exception {
        when(service.abandonar("s1"))
                .thenReturn(sessao(StatusSessao.ABANDONADA, List.of(1), 2, 2));

        mockMvc.perform(post("/api/sessoes/s1/abandonar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABANDONADA"))
                .andExpect(jsonPath("$.blocoAbandono").value(2));
    }

    @Test
    void listarSemLeitorDeveRetornar400() throws Exception {
        mockMvc.perform(get("/api/sessoes"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(service);
    }

    @Test
    void progressoDeveRetornarResumoDoLeitor() throws Exception {
        when(service.progresso("ana", "c1")).thenReturn(new ProgressoResponse(
                "ana", "c1", "Fotossíntese", 3, List.of(1, 2), 66, 3,
                2, 0, 1, "s2", 300, 120, 150));

        mockMvc.perform(get("/api/progresso")
                        .param("leitor", "ana")
                        .param("conteudoId", "c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.percentualConcluido").value(66))
                .andExpect(jsonPath("$.proximoBloco").value(3))
                .andExpect(jsonPath("$.sessaoEmAndamentoId").value("s2"));
    }

    private static SessaoResponse sessao(StatusSessao status, List<Integer> concluidos,
                                         int blocoAtual, Integer blocoAbandono) {
        Instant fim = (status == StatusSessao.EM_ANDAMENTO) ? null : INICIO.plusSeconds(120);
        return new SessaoResponse(
                "s1", "c1", "ana", status,
                blocoAtual, 3, concluidos, concluidos.size() * 100 / 3,
                concluidos.size() * 100, 120, concluidos.size() * 50,
                blocoAbandono, INICIO, fim
        );
    }
}
