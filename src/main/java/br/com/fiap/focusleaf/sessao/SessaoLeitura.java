package br.com.fiap.focusleaf.sessao;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Uma sessão de leitura de um leitor em um conteúdo.
 * Guarda os blocos concluídos e as palavras lidas para calcular as métricas de foco:
 * tempo de leitura, ponto de abandono e velocidade.
 */
@Document(collection = "sessoes_leitura")
public class SessaoLeitura {

    @Id
    private String id;
    private String conteudoId;
    private String leitor;
    private StatusSessao status;

    /** Quantidade de blocos do conteúdo no momento em que a sessão começou. */
    private int totalBlocos;

    /** Bloco em que o leitor está: o seguinte ao último concluído. */
    private int blocoAtual;

    private List<BlocoConcluido> blocosConcluidos = new ArrayList<>();
    private int palavrasLidas;
    private Instant iniciadaEm;
    private Instant finalizadaEm;

    /** Bloco em que o leitor parou, preenchido apenas quando a sessão é abandonada. */
    private Integer blocoAbandono;

    public SessaoLeitura() {
    }

    public boolean estaEmAndamento() {
        return status == StatusSessao.EM_ANDAMENTO;
    }

    public boolean blocoJaConcluido(int ordem) {
        return blocosConcluidos.stream().anyMatch(bloco -> bloco.getOrdem() == ordem);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConteudoId() {
        return conteudoId;
    }

    public void setConteudoId(String conteudoId) {
        this.conteudoId = conteudoId;
    }

    public String getLeitor() {
        return leitor;
    }

    public void setLeitor(String leitor) {
        this.leitor = leitor;
    }

    public StatusSessao getStatus() {
        return status;
    }

    public void setStatus(StatusSessao status) {
        this.status = status;
    }

    public int getTotalBlocos() {
        return totalBlocos;
    }

    public void setTotalBlocos(int totalBlocos) {
        this.totalBlocos = totalBlocos;
    }

    public int getBlocoAtual() {
        return blocoAtual;
    }

    public void setBlocoAtual(int blocoAtual) {
        this.blocoAtual = blocoAtual;
    }

    public List<BlocoConcluido> getBlocosConcluidos() {
        return blocosConcluidos;
    }

    public void setBlocosConcluidos(List<BlocoConcluido> blocosConcluidos) {
        this.blocosConcluidos = blocosConcluidos;
    }

    public int getPalavrasLidas() {
        return palavrasLidas;
    }

    public void setPalavrasLidas(int palavrasLidas) {
        this.palavrasLidas = palavrasLidas;
    }

    public Instant getIniciadaEm() {
        return iniciadaEm;
    }

    public void setIniciadaEm(Instant iniciadaEm) {
        this.iniciadaEm = iniciadaEm;
    }

    public Instant getFinalizadaEm() {
        return finalizadaEm;
    }

    public void setFinalizadaEm(Instant finalizadaEm) {
        this.finalizadaEm = finalizadaEm;
    }

    public Integer getBlocoAbandono() {
        return blocoAbandono;
    }

    public void setBlocoAbandono(Integer blocoAbandono) {
        this.blocoAbandono = blocoAbandono;
    }
}
