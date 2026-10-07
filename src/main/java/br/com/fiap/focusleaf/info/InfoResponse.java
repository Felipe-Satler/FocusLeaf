package br.com.fiap.focusleaf.info;

/**
 * Identificação da instância em execução.
 * Permite provar, nas evidências, qual ambiente (local, staging, producao),
 * qual versão e qual commit estão respondendo em cada URL.
 */
public record InfoResponse(
        String aplicacao,
        String ambiente,
        String versao,
        String commit
) {
}
