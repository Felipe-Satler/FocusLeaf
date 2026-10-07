package br.com.fiap.focusleaf.conteudo.dto;

import br.com.fiap.focusleaf.conteudo.Conteudo;
import br.com.fiap.focusleaf.conteudo.EstimativaLeitura;

import java.time.Instant;

/**
 * Versão resumida usada na listagem: sem o texto dos blocos.
 */
public record ConteudoResumoResponse(
        String id,
        String titulo,
        String autor,
        String descricao,
        int totalBlocos,
        int tempoEstimadoMinutos,
        Instant criadoEm
) {

    public static ConteudoResumoResponse de(Conteudo conteudo) {
        int totalPalavras = conteudo.getBlocos().stream()
                .mapToInt(bloco -> EstimativaLeitura.contarPalavras(bloco.getTexto()))
                .sum();

        return new ConteudoResumoResponse(
                conteudo.getId(),
                conteudo.getTitulo(),
                conteudo.getAutor(),
                conteudo.getDescricao(),
                conteudo.getBlocos().size(),
                EstimativaLeitura.minutosEstimados(totalPalavras),
                conteudo.getCriadoEm()
        );
    }
}
