package br.com.fiap.focusleaf.conteudo.dto;

import br.com.fiap.focusleaf.conteudo.Bloco;
import br.com.fiap.focusleaf.conteudo.EstimativaLeitura;

public record BlocoResponse(
        int ordem,
        String titulo,
        String texto,
        int palavras,
        int tempoEstimadoMinutos
) {

    public static BlocoResponse de(Bloco bloco) {
        int palavras = EstimativaLeitura.contarPalavras(bloco.getTexto());
        return new BlocoResponse(
                bloco.getOrdem(),
                bloco.getTitulo(),
                bloco.getTexto(),
                palavras,
                EstimativaLeitura.minutosEstimados(palavras)
        );
    }
}
