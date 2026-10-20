package br.com.fiap.mobilidadeverde.model;

import java.util.Objects;

/**
 * Operadora de transporte parceira (TOP, SPTrans no MVP).
 * Espelha T_OPERADORA_TRANSPORTE.
 *
 * O campo {@code formatoCodigo} guarda o template do código do cupom
 * (ex: "TOP-XXXX-XXXX"). O CupomGenerator usa esse template para
 * gerar o código real.
 *
 * O campo {@code fatorCo2KgKm} é usado pelo CupomService para calcular
 * o CO2 evitado em cada viagem (vs. carro individual).
 */
public class Operadora {

    private Long id;
    private String nome;
    private String cidade;
    private Modal modal;
    private int valorPassagemCentavos;
    private double fatorCo2KgKm;
    private String formatoCodigo;
    private boolean ativo;

    public Operadora() {
    }

    public Operadora(Long id, String nome, String cidade, Modal modal,
                     int valorPassagemCentavos, double fatorCo2KgKm,
                     String formatoCodigo, boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.cidade = cidade;
        this.modal = modal;
        this.valorPassagemCentavos = valorPassagemCentavos;
        this.fatorCo2KgKm = fatorCo2KgKm;
        this.formatoCodigo = formatoCodigo;
        this.ativo = ativo;
    }

    // ----------------- getters / setters -----------------
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public Modal getModal() { return modal; }
    public void setModal(Modal modal) { this.modal = modal; }

    public int getValorPassagemCentavos() { return valorPassagemCentavos; }
    public void setValorPassagemCentavos(int valorPassagemCentavos) {
        this.valorPassagemCentavos = valorPassagemCentavos;
    }

    public double getFatorCo2KgKm() { return fatorCo2KgKm; }
    public void setFatorCo2KgKm(double fatorCo2KgKm) { this.fatorCo2KgKm = fatorCo2KgKm; }

    public String getFormatoCodigo() { return formatoCodigo; }
    public void setFormatoCodigo(String formatoCodigo) { this.formatoCodigo = formatoCodigo; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Operadora)) return false;
        Operadora other = (Operadora) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Operadora{id=" + id + ", nome='" + nome
            + "', modal=" + modal + ", ativo=" + ativo + "}";
    }
}
