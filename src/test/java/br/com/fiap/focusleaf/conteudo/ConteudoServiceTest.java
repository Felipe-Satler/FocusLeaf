package br.com.fiap.focusleaf.conteudo;

import br.com.fiap.focusleaf.conteudo.dto.BlocoLeituraResponse;
import br.com.fiap.focusleaf.conteudo.dto.BlocoRequest;
import br.com.fiap.focusleaf.conteudo.dto.BlocoResponse;
import br.com.fiap.focusleaf.conteudo.dto.ConteudoRequest;
import br.com.fiap.focusleaf.conteudo.dto.ConteudoResponse;
import br.com.fiap.focusleaf.erro.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConteudoServiceTest {

    @Mock
    private ConteudoRepository repository;

    @InjectMocks
    private ConteudoService service;

    @Test
    void criarDeveNumerarBlocosNaOrdemRecebidaELimparEspacos() {
        when(repository.save(any(Conteudo.class))).thenAnswer(invocacao -> {
            Conteudo salvo = invocacao.getArgument(0);
            salvo.setId("c1");
            return salvo;
        });

        ConteudoRequest request = new ConteudoRequest("  Fotossíntese  ", "Ana", "   ", List.of(
                new BlocoRequest("Introdução", "As plantas produzem energia."),
                new BlocoRequest(null, "  A luz é captada pela clorofila.  ")
        ));

        ConteudoResponse resposta = service.criar(request);

        assertThat(resposta.id()).isEqualTo("c1");
        assertThat(resposta.titulo()).isEqualTo("Fotossíntese");
        assertThat(resposta.descricao()).isNull();
        assertThat(resposta.totalBlocos()).isEqualTo(2);
        assertThat(resposta.blocos()).extracting(BlocoResponse::ordem).containsExactly(1, 2);
        assertThat(resposta.blocos().get(1).texto()).isEqualTo("A luz é captada pela clorofila.");
        assertThat(resposta.totalPalavras()).isEqualTo(10);
        assertThat(resposta.tempoEstimadoMinutos()).isEqualTo(1);
        assertThat(resposta.criadoEm()).isNotNull().isEqualTo(resposta.atualizadoEm());
    }

    @Test
    void buscarConteudoInexistenteDeveLancarNaoEncontrado() {
        when(repository.findById("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar("x"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("x");
    }

    @Test
    void lerBlocoDeveInformarAnteriorEProximo() {
        when(repository.findById("c1")).thenReturn(Optional.of(conteudoComTresBlocos()));

        BlocoLeituraResponse primeiro = service.lerBloco("c1", 1);
        BlocoLeituraResponse meio = service.lerBloco("c1", 2);
        BlocoLeituraResponse ultimo = service.lerBloco("c1", 3);

        assertThat(primeiro.anterior()).isNull();
        assertThat(primeiro.proximo()).isEqualTo(2);
        assertThat(meio.anterior()).isEqualTo(1);
        assertThat(meio.proximo()).isEqualTo(3);
        assertThat(ultimo.anterior()).isEqualTo(2);
        assertThat(ultimo.proximo()).isNull();
        assertThat(ultimo.totalBlocos()).isEqualTo(3);
        assertThat(ultimo.texto()).isEqualTo("Terceiro bloco.");
    }

    @Test
    void lerBlocoForaDoIntervaloDeveLancarNaoEncontrado() {
        when(repository.findById("c1")).thenReturn(Optional.of(conteudoComTresBlocos()));

        assertThatThrownBy(() -> service.lerBloco("c1", 4)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.lerBloco("c1", 0)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void atualizarDeveSubstituirBlocosEManterDataDeCriacao() {
        Conteudo existente = conteudoComTresBlocos();
        Instant criadoEm = existente.getCriadoEm();
        when(repository.findById("c1")).thenReturn(Optional.of(existente));
        when(repository.save(any(Conteudo.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        ConteudoResponse resposta = service.atualizar("c1", new ConteudoRequest(
                "Novo título", null, null, List.of(new BlocoRequest(null, "Único bloco."))));

        assertThat(resposta.titulo()).isEqualTo("Novo título");
        assertThat(resposta.totalBlocos()).isEqualTo(1);
        assertThat(resposta.blocos().get(0).ordem()).isEqualTo(1);
        assertThat(resposta.criadoEm()).isEqualTo(criadoEm);
        assertThat(resposta.atualizadoEm()).isAfter(criadoEm);
    }

    @Test
    void excluirDeveRemoverConteudoExistente() {
        Conteudo existente = conteudoComTresBlocos();
        when(repository.findById("c1")).thenReturn(Optional.of(existente));

        service.excluir("c1");

        verify(repository).delete(existente);
    }

    @Test
    void excluirConteudoInexistenteNaoDeveApagarNada() {
        when(repository.findById("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.excluir("x")).isInstanceOf(RecursoNaoEncontradoException.class);
        verify(repository, never()).delete(any());
    }

    private static Conteudo conteudoComTresBlocos() {
        Conteudo conteudo = new Conteudo();
        conteudo.setId("c1");
        conteudo.setTitulo("Fotossíntese");
        conteudo.setBlocos(new ArrayList<>(List.of(
                new Bloco(1, "Início", "Primeiro bloco."),
                new Bloco(2, null, "Segundo bloco."),
                new Bloco(3, null, "Terceiro bloco.")
        )));
        conteudo.setCriadoEm(Instant.parse("2026-01-01T00:00:00Z"));
        conteudo.setAtualizadoEm(Instant.parse("2026-01-01T00:00:00Z"));
        return conteudo;
    }
}
