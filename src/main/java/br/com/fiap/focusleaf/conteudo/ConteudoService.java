package br.com.fiap.focusleaf.conteudo;

import br.com.fiap.focusleaf.conteudo.dto.BlocoLeituraResponse;
import br.com.fiap.focusleaf.conteudo.dto.BlocoRequest;
import br.com.fiap.focusleaf.conteudo.dto.BlocoResponse;
import br.com.fiap.focusleaf.conteudo.dto.ConteudoRequest;
import br.com.fiap.focusleaf.conteudo.dto.ConteudoResponse;
import br.com.fiap.focusleaf.conteudo.dto.ConteudoResumoResponse;
import br.com.fiap.focusleaf.erro.RecursoNaoEncontradoException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ConteudoService {

    private final ConteudoRepository repository;

    public ConteudoService(ConteudoRepository repository) {
        this.repository = repository;
    }

    public ConteudoResponse criar(ConteudoRequest request) {
        Conteudo conteudo = new Conteudo();
        aplicarDados(conteudo, request);

        Instant agora = Instant.now();
        conteudo.setCriadoEm(agora);
        conteudo.setAtualizadoEm(agora);

        return ConteudoResponse.de(repository.save(conteudo));
    }

    public List<ConteudoResumoResponse> listar() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "criadoEm")).stream()
                .map(ConteudoResumoResponse::de)
                .toList();
    }

    public ConteudoResponse buscar(String id) {
        return ConteudoResponse.de(obterConteudo(id));
    }

    /**
     * Substitui os dados e todos os blocos do conteúdo. A data de criação é mantida.
     */
    public ConteudoResponse atualizar(String id, ConteudoRequest request) {
        Conteudo conteudo = obterConteudo(id);
        aplicarDados(conteudo, request);
        conteudo.setAtualizadoEm(Instant.now());

        return ConteudoResponse.de(repository.save(conteudo));
    }

    public void excluir(String id) {
        repository.delete(obterConteudo(id));
    }

    public List<BlocoResponse> listarBlocos(String id) {
        return obterConteudo(id).getBlocos().stream()
                .map(BlocoResponse::de)
                .toList();
    }

    public BlocoLeituraResponse lerBloco(String id, int ordem) {
        Conteudo conteudo = obterConteudo(id);
        Bloco bloco = conteudo.getBlocos().stream()
                .filter(b -> b.getOrdem() == ordem)
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Bloco " + ordem + " não existe neste conteúdo (total de blocos: "
                                + conteudo.getBlocos().size() + ")."));

        return BlocoLeituraResponse.de(conteudo, bloco);
    }

    /**
     * Busca a entidade ou lança 404. Também será usado pelas sessões de leitura.
     */
    public Conteudo obterConteudo(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conteúdo não encontrado: " + id));
    }

    private void aplicarDados(Conteudo conteudo, ConteudoRequest request) {
        conteudo.setTitulo(request.titulo().strip());
        conteudo.setAutor(textoOpcional(request.autor()));
        conteudo.setDescricao(textoOpcional(request.descricao()));

        List<Bloco> blocos = new ArrayList<>();
        int ordem = 1;
        for (BlocoRequest blocoRequest : request.blocos()) {
            blocos.add(new Bloco(ordem++, textoOpcional(blocoRequest.titulo()), blocoRequest.texto().strip()));
        }
        conteudo.setBlocos(blocos);
    }

    private static String textoOpcional(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.strip();
    }
}
