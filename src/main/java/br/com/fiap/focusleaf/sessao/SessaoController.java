package br.com.fiap.focusleaf.sessao;

import br.com.fiap.focusleaf.sessao.dto.IniciarSessaoRequest;
import br.com.fiap.focusleaf.sessao.dto.SessaoResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/sessoes")
public class SessaoController {

    private final SessaoService service;

    public SessaoController(SessaoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SessaoResponse> iniciar(@RequestBody @Valid IniciarSessaoRequest request) {
        SessaoResponse criada = service.iniciar(request);
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();
        return ResponseEntity.created(local).body(criada);
    }

    @GetMapping
    public List<SessaoResponse> listar(@RequestParam("leitor") String leitor,
                                       @RequestParam(value = "conteudoId", required = false) String conteudoId) {
        return service.listar(leitor, conteudoId);
    }

    @GetMapping("/{id}")
    public SessaoResponse buscar(@PathVariable("id") String id) {
        return service.buscar(id);
    }

    @PostMapping("/{id}/blocos/{ordem}/concluir")
    public SessaoResponse concluirBloco(@PathVariable("id") String id,
                                        @PathVariable("ordem") int ordem) {
        return service.concluirBloco(id, ordem);
    }

    @PostMapping("/{id}/abandonar")
    public SessaoResponse abandonar(@PathVariable("id") String id) {
        return service.abandonar(id);
    }
}
