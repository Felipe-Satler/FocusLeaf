package br.com.fiap.focusleaf.conteudo;

import br.com.fiap.focusleaf.conteudo.dto.BlocoLeituraResponse;
import br.com.fiap.focusleaf.conteudo.dto.BlocoResponse;
import br.com.fiap.focusleaf.conteudo.dto.ConteudoRequest;
import br.com.fiap.focusleaf.conteudo.dto.ConteudoResponse;
import br.com.fiap.focusleaf.erro.RecursoNaoEncontradoException;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa só a camada HTTP: o serviço é simulado, então o banco não é usado.
 */
@WebMvcTest(ConteudoController.class)
class ConteudoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConteudoService service;

    @Test
    void cadastrarConteudoValidoDeveRetornar201ComLocation() throws Exception {
        when(service.criar(any(ConteudoRequest.class))).thenReturn(respostaExemplo());

        mockMvc.perform(post("/api/conteudos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Fotossíntese",
                                  "blocos": [ { "texto": "As plantas produzem energia." } ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/conteudos/c1")))
                .andExpect(jsonPath("$.id").value("c1"))
                .andExpect(jsonPath("$.totalBlocos").value(1));
    }

    @Test
    void cadastrarSemTituloESemBlocosDeveRetornar400ComOsCampos() throws Exception {
        mockMvc.perform(post("/api/conteudos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "titulo": "", "blocos": [] }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.campos.titulo").exists())
                .andExpect(jsonPath("$.campos.blocos").exists());

        verifyNoInteractions(service);
    }

    @Test
    void cadastrarComJsonInvalidoDeveRetornar400() throws Exception {
        mockMvc.perform(post("/api/conteudos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ isto não é json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(service);
    }

    @Test
    void buscarConteudoInexistenteDeveRetornar404() throws Exception {
        when(service.buscar("nao-existe"))
                .thenThrow(new RecursoNaoEncontradoException("Conteúdo não encontrado: nao-existe"));

        mockMvc.perform(get("/api/conteudos/nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.caminho").value("/api/conteudos/nao-existe"));
    }

    @Test
    void lerBlocoDeveRetornarBlocoComNavegacao() throws Exception {
        when(service.lerBloco("c1", 2)).thenReturn(new BlocoLeituraResponse(
                "c1", "Fotossíntese", 2, 3, null, "Texto do bloco dois.", 4, 1, 1, 3));

        mockMvc.perform(get("/api/conteudos/c1/blocos/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordem").value(2))
                .andExpect(jsonPath("$.totalBlocos").value(3))
                .andExpect(jsonPath("$.anterior").value(1))
                .andExpect(jsonPath("$.proximo").value(3));
    }

    @Test
    void ordemDoBlocoNaoNumericaDeveRetornar400() throws Exception {
        mockMvc.perform(get("/api/conteudos/c1/blocos/abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void excluirDeveRetornar204() throws Exception {
        mockMvc.perform(delete("/api/conteudos/c1"))
                .andExpect(status().isNoContent());

        verify(service).excluir("c1");
    }

    private static ConteudoResponse respostaExemplo() {
        Instant agora = Instant.parse("2026-10-07T12:00:00Z");
        return new ConteudoResponse(
                "c1", "Fotossíntese", null, null,
                1, 4, 1,
                List.of(new BlocoResponse(1, null, "As plantas produzem energia.", 4, 1)),
                agora, agora
        );
    }
}
