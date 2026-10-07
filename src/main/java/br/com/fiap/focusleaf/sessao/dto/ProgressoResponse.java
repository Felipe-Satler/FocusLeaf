package br.com.fiap.focusleaf.sessao.dto;

import br.com.fiap.focusleaf.conteudo.Conteudo;
import br.com.fiap.focusleaf.sessao.BlocoConcluido;
import br.com.fiap.focusleaf.sessao.MetricasLeitura;
import br.com.fiap.focusleaf.sessao.SessaoLeitura;
import br.com.fiap.focusleaf.sessao.StatusSessao;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Progresso acumulado de um leitor em um conteúdo, somando todas as sessões.
 * "proximoBloco" é o primeiro bloco ainda não lido (nulo quando todos foram lidos).
 */
public record ProgressoResponse(
        String leitor,
        String conteudoId,
        String tituloConteudo,
        int totalBlocos,
        List<Integer> blocosConcluidos,
        int percentualConcluido,
        Integer proximoBloco,
        int totalSessoes,
        int sessoesConcluidas,
        int sessoesAbandonadas,
        String sessaoEmAndamentoId,
        int palavrasLidas,
        long tempoTotalSegundos,
        int velocidadeMediaPalavrasPorMinuto
) {

    public static ProgressoResponse de(String leitor, Conteudo conteudo, List<SessaoLeitura> sessoes, Instant agora) {
        int total = conteudo.getBlocos().size();

        Set<Integer> concluidos = new TreeSet<>();
        for (SessaoLeitura sessao : sessoes) {
            for (BlocoConcluido bloco : sessao.getBlocosConcluidos()) {
                if (bloco.getOrdem() >= 1 && bloco.getOrdem() <= total) {
                    concluidos.add(bloco.getOrdem());
                }
            }
        }

        Integer proximo = null;
        for (int ordem = 1; ordem <= total; ordem++) {
            if (!concluidos.contains(ordem)) {
                proximo = ordem;
                break;
            }
        }

        int palavras = sessoes.stream().mapToInt(SessaoLeitura::getPalavrasLidas).sum();
        long tempoTotal = sessoes.stream()
                .mapToLong(s -> MetricasLeitura.duracaoSegundos(s.getIniciadaEm(), s.getFinalizadaEm(), agora))
                .sum();

        String emAndamentoId = sessoes.stream()
                .filter(SessaoLeitura::estaEmAndamento)
                .map(SessaoLeitura::getId)
                .findFirst()
                .orElse(null);

        return new ProgressoResponse(
                leitor,
                conteudo.getId(),
                conteudo.getTitulo(),
                total,
                List.copyOf(concluidos),
                MetricasLeitura.percentual(concluidos.size(), total),
                proximo,
                sessoes.size(),
                contar(sessoes, StatusSessao.CONCLUIDA),
                contar(sessoes, StatusSessao.ABANDONADA),
                emAndamentoId,
                palavras,
                tempoTotal,
                MetricasLeitura.velocidadePalavrasPorMinuto(palavras, tempoTotal)
        );
    }

    private static int contar(List<SessaoLeitura> sessoes, StatusSessao status) {
        return (int) sessoes.stream().filter(s -> s.getStatus() == status).count();
    }
}
