package br.com.fiap.focusleaf.conteudo.dto;

import br.com.fiap.focusleaf.conteudo.Bloco;
import br.com.fiap.focusleaf.conteudo.Conteudo;
import br.com.fiap.focusleaf.conteudo.EstimativaLeitura;

/**
 * Um único bloco para a leitura focada, com a navegação para o anterior e o próximo.
 * "anterior" e "proximo" ficam nulos no primeiro e no último bloco.
 */
public record BlocoLeituraResponse(
        String conteudoId,
        String tituloConteudo,
        int ordem,
        int totalBlocos,
        String titulo,
        String texto,
        int palavras,
        int tempoEstimadoMinutos,
        Integer anterior,
        Integer proximo
) {

    public static BlocoLeituraResponse de(Conteudo conteudo, Bloco bloco) {
        int total = conteudo.getBlocos().size();
        int ordem = bloco.getOrdem();
        int palavras = EstimativaLeitura.contarPalavras(bloco.getTexto());

        return new BlocoLeituraResponse(
                conteudo.getId(),
                conteudo.getTitulo(),
                ordem,
                total,
                bloco.getTitulo(),
                bloco.getTexto(),
                palavras,
                EstimativaLeitura.minutosEstimados(palavras),
                ordem > 1 ? ordem - 1 : null,
                ordem < total ? ordem + 1 : null
        );
    }
}
