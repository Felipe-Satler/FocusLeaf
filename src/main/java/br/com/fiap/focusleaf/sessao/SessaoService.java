package br.com.fiap.focusleaf.sessao;

import br.com.fiap.focusleaf.conteudo.Conteudo;
import br.com.fiap.focusleaf.conteudo.ConteudoService;
import br.com.fiap.focusleaf.conteudo.EstimativaLeitura;
import br.com.fiap.focusleaf.erro.ConflitoException;
import br.com.fiap.focusleaf.erro.RecursoNaoEncontradoException;
import br.com.fiap.focusleaf.sessao.dto.IniciarSessaoRequest;
import br.com.fiap.focusleaf.sessao.dto.ProgressoResponse;
import br.com.fiap.focusleaf.sessao.dto.SessaoResponse;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class SessaoService {

    private final SessaoLeituraRepository repository;
    private final ConteudoService conteudoService;
    private final Clock relogio;

    public SessaoService(SessaoLeituraRepository repository, ConteudoService conteudoService, Clock relogio) {
        this.repository = repository;
        this.conteudoService = conteudoService;
        this.relogio = relogio;
    }

    /**
     * Abre uma sessão no bloco 1. Cada leitor pode ter só uma sessão aberta por conteúdo.
     */
    public SessaoResponse iniciar(IniciarSessaoRequest request) {
        String leitor = normalizarLeitor(request.leitor());
        Conteudo conteudo = conteudoService.obterConteudo(request.conteudoId());

        repository.findFirstByLeitorAndConteudoIdAndStatus(leitor, conteudo.getId(), StatusSessao.EM_ANDAMENTO)
                .ifPresent(aberta -> {
                    throw new ConflitoException("O leitor '" + leitor
                            + "' já tem uma sessão em andamento neste conteúdo (sessão " + aberta.getId()
                            + "). Conclua ou abandone essa sessão antes de iniciar outra.");
                });

        SessaoLeitura sessao = new SessaoLeitura();
        sessao.setConteudoId(conteudo.getId());
        sessao.setLeitor(leitor);
        sessao.setStatus(StatusSessao.EM_ANDAMENTO);
        sessao.setTotalBlocos(conteudo.getBlocos().size());
        sessao.setBlocoAtual(1);
        sessao.setIniciadaEm(relogio.instant());

        return SessaoResponse.de(repository.save(sessao), relogio.instant());
    }

    /**
     * Marca um bloco como lido. Repetir o mesmo bloco não altera nada.
     * Quando todos os blocos forem lidos, a sessão é concluída automaticamente.
     */
    public SessaoResponse concluirBloco(String sessaoId, int ordem) {
        SessaoLeitura sessao = obterSessao(sessaoId);
        exigirEmAndamento(sessao);

        if (ordem < 1 || ordem > sessao.getTotalBlocos()) {
            throw new RecursoNaoEncontradoException("Bloco " + ordem + " não existe nesta sessão (total de blocos: "
                    + sessao.getTotalBlocos() + ").");
        }

        if (!sessao.blocoJaConcluido(ordem)) {
            Instant agora = relogio.instant();
            sessao.getBlocosConcluidos().add(new BlocoConcluido(ordem, agora));
            sessao.setPalavrasLidas(sessao.getPalavrasLidas() + palavrasDoBloco(sessao.getConteudoId(), ordem));
            sessao.setBlocoAtual(Math.min(ordem + 1, sessao.getTotalBlocos()));

            if (sessao.getBlocosConcluidos().size() >= sessao.getTotalBlocos()) {
                sessao.setStatus(StatusSessao.CONCLUIDA);
                sessao.setFinalizadaEm(agora);
            }
            sessao = repository.save(sessao);
        }

        return SessaoResponse.de(sessao, relogio.instant());
    }

    /**
     * Encerra a sessão sem terminar a leitura e registra o ponto de abandono.
     */
    public SessaoResponse abandonar(String sessaoId) {
        SessaoLeitura sessao = obterSessao(sessaoId);
        exigirEmAndamento(sessao);

        sessao.setStatus(StatusSessao.ABANDONADA);
        sessao.setFinalizadaEm(relogio.instant());
        sessao.setBlocoAbandono(sessao.getBlocoAtual());

        return SessaoResponse.de(repository.save(sessao), relogio.instant());
    }

    public SessaoResponse buscar(String sessaoId) {
        return SessaoResponse.de(obterSessao(sessaoId), relogio.instant());
    }

    public List<SessaoResponse> listar(String leitor, String conteudoId) {
        String leitorNormalizado = normalizarLeitor(leitor);
        List<SessaoLeitura> sessoes = (conteudoId == null || conteudoId.isBlank())
                ? repository.findByLeitorOrderByIniciadaEmDesc(leitorNormalizado)
                : repository.findByLeitorAndConteudoIdOrderByIniciadaEmDesc(leitorNormalizado, conteudoId);

        Instant agora = relogio.instant();
        return sessoes.stream()
                .map(sessao -> SessaoResponse.de(sessao, agora))
                .toList();
    }

    public ProgressoResponse progresso(String leitor, String conteudoId) {
        String leitorNormalizado = normalizarLeitor(leitor);
        Conteudo conteudo = conteudoService.obterConteudo(conteudoId);
        List<SessaoLeitura> sessoes =
                repository.findByLeitorAndConteudoIdOrderByIniciadaEmDesc(leitorNormalizado, conteudoId);

        return ProgressoResponse.de(leitorNormalizado, conteudo, sessoes, relogio.instant());
    }

    private SessaoLeitura obterSessao(String sessaoId) {
        return repository.findById(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão não encontrada: " + sessaoId));
    }

    private static void exigirEmAndamento(SessaoLeitura sessao) {
        if (!sessao.estaEmAndamento()) {
            throw new ConflitoException("A sessão " + sessao.getId() + " já foi encerrada (status: "
                    + sessao.getStatus() + ").");
        }
    }

    private int palavrasDoBloco(String conteudoId, int ordem) {
        return conteudoService.obterConteudo(conteudoId).getBlocos().stream()
                .filter(bloco -> bloco.getOrdem() == ordem)
                .findFirst()
                .map(bloco -> EstimativaLeitura.contarPalavras(bloco.getTexto()))
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Bloco " + ordem + " não existe mais no conteúdo " + conteudoId + "."));
    }

    private static String normalizarLeitor(String leitor) {
        return leitor == null ? "" : leitor.strip().toLowerCase(Locale.ROOT);
    }
}
