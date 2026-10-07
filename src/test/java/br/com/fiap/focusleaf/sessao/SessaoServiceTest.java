package br.com.fiap.focusleaf.sessao;

import br.com.fiap.focusleaf.conteudo.Bloco;
import br.com.fiap.focusleaf.conteudo.Conteudo;
import br.com.fiap.focusleaf.conteudo.ConteudoService;
import br.com.fiap.focusleaf.erro.ConflitoException;
import br.com.fiap.focusleaf.erro.RecursoNaoEncontradoException;
import br.com.fiap.focusleaf.sessao.dto.IniciarSessaoRequest;
import br.com.fiap.focusleaf.sessao.dto.ProgressoResponse;
import br.com.fiap.focusleaf.sessao.dto.SessaoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Usa um relógio fixo: a sessão começa às 10:00 e "agora" é sempre 10:02 (120 segundos depois).
 * Cada bloco do conteúdo de teste tem exatamente 100 palavras.
 */
@ExtendWith(MockitoExtension.class)
class SessaoServiceTest {

    private static final Instant INICIO = Instant.parse("2026-10-07T10:00:00Z");
    private static final Instant AGORA = INICIO.plusSeconds(120);

    @Mock
    private SessaoLeituraRepository repository;

    @Mock
    private ConteudoService conteudoService;

    private SessaoService service;

    @BeforeEach
    void configurar() {
        service = new SessaoService(repository, conteudoService, Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    @Test
    void iniciarDeveAbrirSessaoNoPrimeiroBlocoComLeitorEmMinusculas() {
        when(conteudoService.obterConteudo("c1")).thenReturn(conteudoComTresBlocos());
        when(repository.findFirstByLeitorAndConteudoIdAndStatus("ana", "c1", StatusSessao.EM_ANDAMENTO))
                .thenReturn(Optional.empty());
        when(repository.save(any(SessaoLeitura.class))).thenAnswer(invocacao -> {
            SessaoLeitura salva = invocacao.getArgument(0);
            salva.setId("s1");
            return salva;
        });

        SessaoResponse resposta = service.iniciar(new IniciarSessaoRequest("c1", "  Ana "));

        assertThat(resposta.id()).isEqualTo("s1");
        assertThat(resposta.leitor()).isEqualTo("ana");
        assertThat(resposta.status()).isEqualTo(StatusSessao.EM_ANDAMENTO);
        assertThat(resposta.blocoAtual()).isEqualTo(1);
        assertThat(resposta.totalBlocos()).isEqualTo(3);
        assertThat(resposta.blocosConcluidos()).isEmpty();
        assertThat(resposta.iniciadaEm()).isEqualTo(AGORA);
        assertThat(resposta.duracaoSegundos()).isZero();
    }

    @Test
    void iniciarComSessaoJaAbertaDeveLancarConflito() {
        when(conteudoService.obterConteudo("c1")).thenReturn(conteudoComTresBlocos());
        when(repository.findFirstByLeitorAndConteudoIdAndStatus("ana", "c1", StatusSessao.EM_ANDAMENTO))
                .thenReturn(Optional.of(sessaoAberta()));

        assertThatThrownBy(() -> service.iniciar(new IniciarSessaoRequest("c1", "ana")))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("s1");
        verify(repository, never()).save(any());
    }

    @Test
    void iniciarComConteudoInexistenteDeveLancarNaoEncontrado() {
        when(conteudoService.obterConteudo("x"))
                .thenThrow(new RecursoNaoEncontradoException("Conteúdo não encontrado: x"));

        assertThatThrownBy(() -> service.iniciar(new IniciarSessaoRequest("x", "ana")))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void concluirBlocoDeveSomarPalavrasEAvancarParaOProximo() {
        when(repository.findById("s1")).thenReturn(Optional.of(sessaoAberta()));
        when(conteudoService.obterConteudo("c1")).thenReturn(conteudoComTresBlocos());
        when(repository.save(any(SessaoLeitura.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        SessaoResponse resposta = service.concluirBloco("s1", 1);

        assertThat(resposta.blocosConcluidos()).containsExactly(1);
        assertThat(resposta.palavrasLidas()).isEqualTo(100);
        assertThat(resposta.blocoAtual()).isEqualTo(2);
        assertThat(resposta.status()).isEqualTo(StatusSessao.EM_ANDAMENTO);
        assertThat(resposta.percentualConcluido()).isEqualTo(33);
        assertThat(resposta.duracaoSegundos()).isEqualTo(120);
        assertThat(resposta.velocidadePalavrasPorMinuto()).isEqualTo(50);
    }

    @Test
    void concluirOMesmoBlocoDuasVezesNaoDuplicaPalavras() {
        SessaoLeitura sessao = sessaoAberta();
        sessao.getBlocosConcluidos().add(new BlocoConcluido(1, INICIO));
        sessao.setPalavrasLidas(100);
        sessao.setBlocoAtual(2);
        when(repository.findById("s1")).thenReturn(Optional.of(sessao));

        SessaoResponse resposta = service.concluirBloco("s1", 1);

        assertThat(resposta.blocosConcluidos()).containsExactly(1);
        assertThat(resposta.palavrasLidas()).isEqualTo(100);
        verify(repository, never()).save(any());
        verifyNoInteractions(conteudoService);
    }

    @Test
    void concluirOUltimoBlocoPendenteDeveConcluirASessao() {
        SessaoLeitura sessao = sessaoAberta();
        sessao.getBlocosConcluidos().add(new BlocoConcluido(1, INICIO.plusSeconds(40)));
        sessao.getBlocosConcluidos().add(new BlocoConcluido(2, INICIO.plusSeconds(80)));
        sessao.setPalavrasLidas(200);
        sessao.setBlocoAtual(3);
        when(repository.findById("s1")).thenReturn(Optional.of(sessao));
        when(conteudoService.obterConteudo("c1")).thenReturn(conteudoComTresBlocos());
        when(repository.save(any(SessaoLeitura.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        SessaoResponse resposta = service.concluirBloco("s1", 3);

        assertThat(resposta.status()).isEqualTo(StatusSessao.CONCLUIDA);
        assertThat(resposta.finalizadaEm()).isEqualTo(AGORA);
        assertThat(resposta.percentualConcluido()).isEqualTo(100);
        assertThat(resposta.palavrasLidas()).isEqualTo(300);
        assertThat(resposta.velocidadePalavrasPorMinuto()).isEqualTo(150);
    }

    @Test
    void concluirBlocoEmSessaoEncerradaDeveLancarConflito() {
        SessaoLeitura sessao = sessaoAberta();
        sessao.setStatus(StatusSessao.ABANDONADA);
        when(repository.findById("s1")).thenReturn(Optional.of(sessao));

        assertThatThrownBy(() -> service.concluirBloco("s1", 2)).isInstanceOf(ConflitoException.class);
    }

    @Test
    void concluirBlocoForaDoIntervaloDeveLancarNaoEncontrado() {
        when(repository.findById("s1")).thenReturn(Optional.of(sessaoAberta()));

        assertThatThrownBy(() -> service.concluirBloco("s1", 4))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void abandonarDeveRegistrarOPontoDeAbandono() {
        SessaoLeitura sessao = sessaoAberta();
        sessao.getBlocosConcluidos().add(new BlocoConcluido(1, INICIO.plusSeconds(60)));
        sessao.setPalavrasLidas(100);
        sessao.setBlocoAtual(2);
        when(repository.findById("s1")).thenReturn(Optional.of(sessao));
        when(repository.save(any(SessaoLeitura.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        SessaoResponse resposta = service.abandonar("s1");

        assertThat(resposta.status()).isEqualTo(StatusSessao.ABANDONADA);
        assertThat(resposta.blocoAbandono()).isEqualTo(2);
        assertThat(resposta.finalizadaEm()).isEqualTo(AGORA);
        assertThat(resposta.duracaoSegundos()).isEqualTo(120);
        assertThat(resposta.velocidadePalavrasPorMinuto()).isEqualTo(50);
    }

    @Test
    void progressoDeveSomarTodasAsSessoesDoLeitor() {
        SessaoLeitura abandonada = sessaoAberta();
        abandonada.setStatus(StatusSessao.ABANDONADA);
        abandonada.getBlocosConcluidos().add(new BlocoConcluido(1, INICIO.plusSeconds(20)));
        abandonada.getBlocosConcluidos().add(new BlocoConcluido(2, INICIO.plusSeconds(40)));
        abandonada.setPalavrasLidas(200);
        abandonada.setFinalizadaEm(INICIO.plusSeconds(60));

        SessaoLeitura aberta = sessaoAberta();
        aberta.setId("s2");
        aberta.setIniciadaEm(INICIO.plusSeconds(60));
        aberta.getBlocosConcluidos().add(new BlocoConcluido(1, INICIO.plusSeconds(90)));
        aberta.setPalavrasLidas(100);

        when(conteudoService.obterConteudo("c1")).thenReturn(conteudoComTresBlocos());
        when(repository.findByLeitorAndConteudoIdOrderByIniciadaEmDesc("ana", "c1"))
                .thenReturn(List.of(aberta, abandonada));

        ProgressoResponse progresso = service.progresso("Ana", "c1");

        assertThat(progresso.leitor()).isEqualTo("ana");
        assertThat(progresso.blocosConcluidos()).containsExactly(1, 2);
        assertThat(progresso.percentualConcluido()).isEqualTo(66);
        assertThat(progresso.proximoBloco()).isEqualTo(3);
        assertThat(progresso.totalSessoes()).isEqualTo(2);
        assertThat(progresso.sessoesAbandonadas()).isEqualTo(1);
        assertThat(progresso.sessoesConcluidas()).isZero();
        assertThat(progresso.sessaoEmAndamentoId()).isEqualTo("s2");
        assertThat(progresso.palavrasLidas()).isEqualTo(300);
        assertThat(progresso.tempoTotalSegundos()).isEqualTo(120);
        assertThat(progresso.velocidadeMediaPalavrasPorMinuto()).isEqualTo(150);
    }

    @Test
    void listarSemConteudoDeveTrazerTodasAsSessoesDoLeitor() {
        when(repository.findByLeitorOrderByIniciadaEmDesc("ana")).thenReturn(List.of(sessaoAberta()));

        List<SessaoResponse> sessoes = service.listar(" ANA ", null);

        assertThat(sessoes).hasSize(1);
        verify(repository, never()).findByLeitorAndConteudoIdOrderByIniciadaEmDesc(anyString(), anyString());
    }

    private static Conteudo conteudoComTresBlocos() {
        String cemPalavras = "palavra ".repeat(100);
        Conteudo conteudo = new Conteudo();
        conteudo.setId("c1");
        conteudo.setTitulo("Fotossíntese");
        conteudo.setBlocos(new ArrayList<>(List.of(
                new Bloco(1, null, cemPalavras),
                new Bloco(2, null, cemPalavras),
                new Bloco(3, null, cemPalavras)
        )));
        return conteudo;
    }

    private static SessaoLeitura sessaoAberta() {
        SessaoLeitura sessao = new SessaoLeitura();
        sessao.setId("s1");
        sessao.setConteudoId("c1");
        sessao.setLeitor("ana");
        sessao.setStatus(StatusSessao.EM_ANDAMENTO);
        sessao.setTotalBlocos(3);
        sessao.setBlocoAtual(1);
        sessao.setIniciadaEm(INICIO);
        return sessao;
    }
}
