package br.com.fiap.focusleaf.sessao;

import br.com.fiap.focusleaf.sessao.dto.ProgressoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/progresso")
public class ProgressoController {

    private final SessaoService service;

    public ProgressoController(SessaoService service) {
        this.service = service;
    }

    @GetMapping
    public ProgressoResponse progresso(@RequestParam("leitor") String leitor,
                                       @RequestParam("conteudoId") String conteudoId) {
        return service.progresso(leitor, conteudoId);
    }
}
