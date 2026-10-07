package br.com.fiap.focusleaf.sessao;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface SessaoLeituraRepository extends MongoRepository<SessaoLeitura, String> {

    List<SessaoLeitura> findByLeitorOrderByIniciadaEmDesc(String leitor);

    List<SessaoLeitura> findByLeitorAndConteudoIdOrderByIniciadaEmDesc(String leitor, String conteudoId);

    Optional<SessaoLeitura> findFirstByLeitorAndConteudoIdAndStatus(String leitor, String conteudoId,
                                                                     StatusSessao status);
}
