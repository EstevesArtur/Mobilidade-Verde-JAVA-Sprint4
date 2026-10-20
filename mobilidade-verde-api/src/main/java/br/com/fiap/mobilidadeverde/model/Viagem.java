package br.com.fiap.mobilidadeverde.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Viagem efetivamente realizada com um Voucher utilizado.
 * Espelha T_VIAGEM (relação 1:1 com Voucher).
 *
 * O CO2 evitado é calculado pelo CupomService no momento de
 * marcar o cupom como UTILIZADO, comparando o fator da operadora
 * com o fator de carro individual.
 */
public class Viagem {

    private Long id;
    private Voucher voucher;
    private double kmEstimado;
    private int co2EvitadoG;
    private LocalDateTime registradoEm;

    public Viagem() {
    }

    public Viagem(Long id, Voucher voucher, double kmEstimado, int co2EvitadoG) {
        this.id = id;
        this.voucher = voucher;
        this.kmEstimado = kmEstimado;
        this.co2EvitadoG = co2EvitadoG;
        this.registradoEm = LocalDateTime.now();
    }

    // ----------------- getters / setters -----------------
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Voucher getVoucher() { return voucher; }
    public void setVoucher(Voucher voucher) { this.voucher = voucher; }

    public double getKmEstimado() { return kmEstimado; }
    public void setKmEstimado(double kmEstimado) { this.kmEstimado = kmEstimado; }

    public int getCo2EvitadoG() { return co2EvitadoG; }
    public void setCo2EvitadoG(int co2EvitadoG) { this.co2EvitadoG = co2EvitadoG; }

    public LocalDateTime getRegistradoEm() { return registradoEm; }
    public void setRegistradoEm(LocalDateTime registradoEm) { this.registradoEm = registradoEm; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Viagem)) return false;
        Viagem other = (Viagem) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Viagem{id=" + id
            + ", km=" + kmEstimado
            + ", co2EvitadoG=" + co2EvitadoG + "}";
    }
}
