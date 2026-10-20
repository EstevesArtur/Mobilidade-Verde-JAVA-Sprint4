package br.com.fiap.mobilidadeverde.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Log append-only de movimentação de pontos.
 * Espelha T_TRANSACAO_PONTOS.
 *
 * Toda alteração de saldo na Carteira gera um registro aqui.
 * O voucher associado é opcional (origem AJUSTE_MANUAL ou OUTROS
 * pode não ter voucher relacionado).
 */
public class TransacaoPontos {

    private Long id;
    private Usuario usuario;
    private TipoTransacao tipo;
    private int pontos;
    private OrigemTransacao origem;
    private Voucher voucher;
    private String descricao;
    private String ipOrigem;
    private LocalDateTime dataTransacao;

    public TransacaoPontos() {
    }

    public TransacaoPontos(Long id, Usuario usuario, TipoTransacao tipo,
                           int pontos, OrigemTransacao origem, Voucher voucher,
                           String descricao, String ipOrigem) {
        this.id = id;
        this.usuario = usuario;
        this.tipo = tipo;
        this.pontos = pontos;
        this.origem = origem;
        this.voucher = voucher;
        this.descricao = descricao;
        this.ipOrigem = ipOrigem;
        this.dataTransacao = LocalDateTime.now();
    }

    // ----------------- getters / setters -----------------
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public TipoTransacao getTipo() { return tipo; }
    public void setTipo(TipoTransacao tipo) { this.tipo = tipo; }

    public int getPontos() { return pontos; }
    public void setPontos(int pontos) { this.pontos = pontos; }

    public OrigemTransacao getOrigem() { return origem; }
    public void setOrigem(OrigemTransacao origem) { this.origem = origem; }

    public Voucher getVoucher() { return voucher; }
    public void setVoucher(Voucher voucher) { this.voucher = voucher; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getIpOrigem() { return ipOrigem; }
    public void setIpOrigem(String ipOrigem) { this.ipOrigem = ipOrigem; }

    public LocalDateTime getDataTransacao() { return dataTransacao; }
    public void setDataTransacao(LocalDateTime dataTransacao) {
        this.dataTransacao = dataTransacao;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransacaoPontos)) return false;
        TransacaoPontos other = (TransacaoPontos) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        String sinal = tipo == TipoTransacao.CREDITO ? "+" : "-";
        return "TransacaoPontos{id=" + id
            + ", " + sinal + pontos + " pts"
            + ", origem=" + origem
            + ", desc='" + descricao + "'}";
    }
}
