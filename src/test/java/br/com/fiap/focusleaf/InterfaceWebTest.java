package br.com.fiap.focusleaf;

import br.com.fiap.focusleaf.info.InfoController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Garante que a interface (arquivos em src/main/resources/static) é servida pela aplicação
 * e que o endpoint de identificação responde por HTTP. Útil também como base de smoke test.
 */
@WebMvcTest(InfoController.class)
class InterfaceWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void paginaDaInterfaceDeveSerServida() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>FocusLeaf</title>")));
    }

    @Test
    void scriptEEstilosDaInterfaceDevemSerServidos() throws Exception {
        mockMvc.perform(get("/js/app.js")).andExpect(status().isOk());
        mockMvc.perform(get("/css/estilos.css")).andExpect(status().isOk());
    }

    @Test
    void infoDeveIdentificarAAplicacao() throws Exception {
        mockMvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aplicacao").value("focusleaf"));
    }
}
