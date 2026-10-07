package br.com.fiap.focusleaf.info;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/info")
public class InfoController {

    private final InfoResponse info;

    public InfoController(
            @Value("${spring.application.name}") String aplicacao,
            @Value("${focusleaf.ambiente}") String ambiente,
            @Value("${focusleaf.versao}") String versao,
            @Value("${focusleaf.commit}") String commit) {
        this.info = new InfoResponse(aplicacao, ambiente, versao, commit);
    }

    @GetMapping
    public InfoResponse info() {
        return info;
    }
}
