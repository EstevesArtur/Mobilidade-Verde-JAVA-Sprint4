package br.com.fiap.mobilidadeverde.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Carteira de Pontos ECOA do usuário.
 * Espelha T_CARTEIRA (relação 1:1 com Usuario).
 *
 * Os pontos vêm pré-acumulados via SoulUp (geração fora do escopo desta feature).
 */
public class Carteira {

    private Long id;
    private Usuario usuario;
    private int saldoPontos;
    private LocalDateTime atualizadoEm;

    public Carteira() {
    }

    public Carteira(Long id, Usuario usuario, int saldoPontos) {
        this.id = id;
        this.usuario = usuario;
        this.saldoPontos = saldoPontos;
        this.atualizadoEm = LocalDateTime.now();
    }

    /**
     * Debita pontos da carteira. Lança exceção se o saldo for insuficiente.
     * Encapsula a invariante CK_CARTEIRA_SALDO (saldo >= 0).
     */
    public void debitar(int pontos) {
        if (pontos <= 0) {
            throw new IllegalArgumentException("Pontos a debitar deve ser positivo");
        }
        if (this.saldoPontos < pontos) {
            throw new IllegalStateException(
                "Saldo insuficiente: " + this.saldoPontos + " < " + pontos);
        }
        this.saldoPontos -= pontos;
        this.atualizadoEm = LocalDateTime.now();
    }

    /**
     * Credita pontos na carteira (usado, por exemplo, para estornos).
     */
    public void creditar(int pontos) {
        if (pontos <= 0) {
            throw new IllegalArgumentException("Pontos a creditar deve ser positivo");
        }
        this.saldoPontos += pontos;
        this.atualizadoEm = LocalDateTime.now();
    }

    // ----------------- getters / setters -----------------
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public int getSaldoPontos() { return saldoPontos; }
    public void setSaldoPontos(int saldoPontos) { this.saldoPontos = saldoPontos; }

    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Carteira)) return false;
        Carteira other = (Carteira) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Carteira{id=" + id
            + ", usuario=" + (usuario != null ? usuario.getNome() : "null")
            + ", saldoPontos=" + saldoPontos + "}";
    }
}
