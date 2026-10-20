package br.com.fiap.mobilidadeverde.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Cupom de transporte gerado a partir do resgate de pontos.
 * Núcleo do produto. Espelha T_VOUCHER.
 *
 * Ciclo de vida pelo campo status (enum {@link StatusVoucher}):
 *   GERADO → UTILIZADO   (usuário embarcou)
 *   GERADO → EXPIRADO    (não usado em 24h)
 *   GERADO → CANCELADO   (estorno manual — não usado no MVP)
 */
public class Voucher {

    private Long id;
    private Usuario usuario;
    private Operadora operadora;
    private FaixaCupom faixa;
    private String codigo;
    private String hashValidacao;
    private StatusVoucher status;
    private LocalDateTime geradoEm;
    private LocalDateTime expiraEm;
    private LocalDateTime utilizadoEm;

    public Voucher() {
    }

    public Voucher(Long id, Usuario usuario, Operadora operadora,
                   FaixaCupom faixa, String codigo, String hashValidacao,
                   LocalDateTime geradoEm, LocalDateTime expiraEm) {
        this.id = id;
        this.usuario = usuario;
        this.operadora = operadora;
        this.faixa = faixa;
        this.codigo = codigo;
        this.hashValidacao = hashValidacao;
        this.status = StatusVoucher.GERADO;
        this.geradoEm = geradoEm;
        this.expiraEm = expiraEm;
    }

    /**
     * Indica se o cupom passou da janela de validade.
     * Lê o relógio do sistema — chamado pelo CupomService durante o uso.
     */
    public boolean estaExpirado() {
        return LocalDateTime.now().isAfter(this.expiraEm);
    }

    // ----------------- getters / setters -----------------
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Operadora getOperadora() { return operadora; }
    public void setOperadora(Operadora operadora) { this.operadora = operadora; }

    public FaixaCupom getFaixa() { return faixa; }
    public void setFaixa(FaixaCupom faixa) { this.faixa = faixa; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getHashValidacao() { return hashValidacao; }
    public void setHashValidacao(String hashValidacao) { this.hashValidacao = hashValidacao; }

    public StatusVoucher getStatus() { return status; }
    public void setStatus(StatusVoucher status) { this.status = status; }

    public LocalDateTime getGeradoEm() { return geradoEm; }
    public void setGeradoEm(LocalDateTime geradoEm) { this.geradoEm = geradoEm; }

    public LocalDateTime getExpiraEm() { return expiraEm; }
    public void setExpiraEm(LocalDateTime expiraEm) { this.expiraEm = expiraEm; }

    public LocalDateTime getUtilizadoEm() { return utilizadoEm; }
    public void setUtilizadoEm(LocalDateTime utilizadoEm) { this.utilizadoEm = utilizadoEm; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Voucher)) return false;
        Voucher other = (Voucher) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Voucher{codigo='" + codigo + "', status=" + status
            + ", operadora=" + (operadora != null ? operadora.getNome() : "null")
            + ", valor=R$ "
            + (faixa != null ? String.format("%.2f", faixa.getValorCentavos() / 100.0) : "?")
            + "}";
    }
}
