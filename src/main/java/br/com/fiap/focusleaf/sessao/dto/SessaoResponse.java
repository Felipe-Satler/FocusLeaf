package br.com.fiap.focusleaf.sessao.dto;

import br.com.fiap.focusleaf.sessao.BlocoConcluido;
import br.com.fiap.focusleaf.sessao.MetricasLeitura;
import br.com.fiap.focusleaf.sessao.SessaoLeitura;
import br.com.fiap.focusleaf.sessao.StatusSessao;

import java.time.Instant;
import java.util.List;

/**
 * Sessão com as métricas já calculadas.
 * Em sessões abertas, a duração é contada até o momento da consulta.
 */
public record SessaoResponse(
        String id,
        String conteudoId,
        String leitor,
        StatusSessao status,
        int blocoAtual,
        int totalBlocos,
        List<Integer> blocosConcluidos,
        int percentualConcluido,
        int palavrasLidas,
        long duracaoSegundos,
        int velocidadePalavrasPorMinuto,
        Integer blocoAbandono,
        Instant iniciadaEm,
        Instant finalizadaEm
) {

    public static SessaoResponse de(SessaoLeitura sessao, Instant agora) {
        List<Integer> concluidos = sessao.getBlocosConcluidos().stream()
                .map(BlocoConcluido::getOrdem)
                .sorted()
                .toList();
        long duracao = MetricasLeitura.duracaoSegundos(sessao.getIniciadaEm(), sessao.getFinalizadaEm(), agora);

        return new SessaoResponse(
                sessao.getId(),
                sessao.getConteudoId(),
                sessao.getLeitor(),
                sessao.getStatus(),
                sessao.getBlocoAtual(),
                sessao.getTotalBlocos(),
                concluidos,
                MetricasLeitura.percentual(concluidos.size(), sessao.getTotalBlocos()),
                sessao.getPalavrasLidas(),
                duracao,
                MetricasLeitura.velocidadePalavrasPorMinuto(sessao.getPalavrasLidas(), duracao),
                sessao.getBlocoAbandono(),
                sessao.getIniciadaEm(),
                sessao.getFinalizadaEm()
        );
    }
}
