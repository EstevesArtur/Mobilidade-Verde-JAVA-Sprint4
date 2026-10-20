package br.com.fiap.mobilidadeverde.dto;

/**
 * Uma linha do ranking de impacto ambiental.
 *
 * Alimenta o painel de impacto da plataforma. Corresponde a consulta
 * C04 entregue na disciplina de Building Relational Database.
 */
public class RankingImpactoDTO {

    private final String nome;
    private final String cidade;
    private final int viagens;
    private final double kmPercorridos;
    private final double co2EvitadoKg;
    private final double arvoresAnoEquivalente;

    public RankingImpactoDTO(String nome, String cidade, int viagens,
                             double kmPercorridos, double co2EvitadoKg,
                             double arvoresAnoEquivalente) {
        this.nome = nome;
        this.cidade = cidade;
        this.viagens = viagens;
        this.kmPercorridos = kmPercorridos;
        this.co2EvitadoKg = co2EvitadoKg;
        this.arvoresAnoEquivalente = arvoresAnoEquivalente;
    }

    public String getNome() { return nome; }
    public String getCidade() { return cidade; }
    public int getViagens() { return viagens; }
    public double getKmPercorridos() { return kmPercorridos; }
    public double getCo2EvitadoKg() { return co2EvitadoKg; }
    public double getArvoresAnoEquivalente() { return arvoresAnoEquivalente; }
}
