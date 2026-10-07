package br.com.fiap.focusleaf.conteudo.dto;

import br.com.fiap.focusleaf.conteudo.Conteudo;
import br.com.fiap.focusleaf.conteudo.EstimativaLeitura;

import java.time.Instant;
import java.util.List;

/**
 * Conteúdo completo, com todos os blocos.
 */
public record ConteudoResponse(
        String id,
        String titulo,
        String autor,
        String descricao,
        int totalBlocos,
        int totalPalavras,
        int tempoEstimadoMinutos,
        List<BlocoResponse> blocos,
        Instant criadoEm,
        Instant atualizadoEm
) {

    public static ConteudoResponse de(Conteudo conteudo) {
        List<BlocoResponse> blocos = conteudo.getBlocos().stream()
                .map(BlocoResponse::de)
                .toList();
        int totalPalavras = blocos.stream().mapToInt(BlocoResponse::palavras).sum();

        return new ConteudoResponse(
                conteudo.getId(),
                conteudo.getTitulo(),
                conteudo.getAutor(),
                conteudo.getDescricao(),
                blocos.size(),
                totalPalavras,
                EstimativaLeitura.minutosEstimados(totalPalavras),
                blocos,
                conteudo.getCriadoEm(),
                conteudo.getAtualizadoEm()
        );
    }
}
