package br.com.fiap.focusleaf.conteudo;

import br.com.fiap.focusleaf.conteudo.dto.BlocoLeituraResponse;
import br.com.fiap.focusleaf.conteudo.dto.BlocoResponse;
import br.com.fiap.focusleaf.conteudo.dto.ConteudoRequest;
import br.com.fiap.focusleaf.conteudo.dto.ConteudoResponse;
import br.com.fiap.focusleaf.conteudo.dto.ConteudoResumoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/conteudos")
public class ConteudoController {

    private final ConteudoService service;

    public ConteudoController(ConteudoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ConteudoResponse> criar(@RequestBody @Valid ConteudoRequest request) {
        ConteudoResponse criado = service.criar(request);
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();
        return ResponseEntity.created(local).body(criado);
    }

    @GetMapping
    public List<ConteudoResumoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ConteudoResponse buscar(@PathVariable("id") String id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    public ConteudoResponse atualizar(@PathVariable("id") String id,
                                      @RequestBody @Valid ConteudoRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable("id") String id) {
        service.excluir(id);
    }

    @GetMapping("/{id}/blocos")
    public List<BlocoResponse> listarBlocos(@PathVariable("id") String id) {
        return service.listarBlocos(id);
    }

    @GetMapping("/{id}/blocos/{ordem}")
    public BlocoLeituraResponse lerBloco(@PathVariable("id") String id,
                                         @PathVariable("ordem") int ordem) {
        return service.lerBloco(id, ordem);
    }
}
