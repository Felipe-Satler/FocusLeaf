package br.com.fiap.focusleaf.conteudo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ConteudoRepository extends MongoRepository<Conteudo, String> {
}
