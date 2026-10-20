package br.com.fiap.mobilidadeverde.model;

import java.util.Objects;

/**
 * Faixa de cupom disponível para resgate (R$ 2, 5, 10, 20).
 * Espelha T_FAIXA_CUPOM.
 *
 * pontosNecessarios = valorCentavos no MVP (100 pts = R$ 1,00).
 */
public class FaixaCupom {

    private Long id;
    private int valorCentavos;
    private int pontosNecessarios;
    private boolean ativo;

    public FaixaCupom() {
    }

    public FaixaCupom(Long id, int valorCentavos, int pontosNecessarios, boolean ativo) {
        this.id = id;
        this.valorCentavos = valorCentavos;
        this.pontosNecessarios = pontosNecessarios;
        this.ativo = ativo;
    }

    // ----------------- getters / setters -----------------
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getValorCentavos() { return valorCentavos; }
    public void setValorCentavos(int valorCentavos) { this.valorCentavos = valorCentavos; }

    public int getPontosNecessarios() { return pontosNecessarios; }
    public void setPontosNecessarios(int pontosNecessarios) {
        this.pontosNecessarios = pontosNecessarios;
    }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FaixaCupom)) return false;
        FaixaCupom other = (FaixaCupom) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "FaixaCupom{id=" + id
            + ", valor=R$ " + String.format("%.2f", valorCentavos / 100.0)
            + ", pontos=" + pontosNecessarios + "}";
    }
}
