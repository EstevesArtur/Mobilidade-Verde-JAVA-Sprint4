package br.com.fiap.mobilidadeverde.dto;

/**
 * DTO com o impacto ambiental acumulado de um usuario.
 *
 * Os campos foram alinhados com o tipo Impacto do front-end React
 * (src/types/impacto.ts), de modo que na Sprint 4, quando este DTO
 * virar resposta JSON da API, a tela nao precise de conversao.
 */
public class ImpactoDTO {

    private final int qtdViagens;
    private final double kmEstimado;
    private final double co2EvitadoKg;
    private final double arvoresAno;
    private final double arvoresDia;
    private final double kmCarroEvitado;

    public ImpactoDTO(int qtdViagens, double kmEstimado, double co2EvitadoKg,
                      double arvoresAno, double arvoresDia, double kmCarroEvitado) {
        this.qtdViagens = qtdViagens;
        this.kmEstimado = kmEstimado;
        this.co2EvitadoKg = co2EvitadoKg;
        this.arvoresAno = arvoresAno;
        this.arvoresDia = arvoresDia;
        this.kmCarroEvitado = kmCarroEvitado;
    }

    public int getQtdViagens() { return qtdViagens; }
    public double getKmEstimado() { return kmEstimado; }
    public double getCo2EvitadoKg() { return co2EvitadoKg; }

    /** Arvores necessarias para absorver esse CO2 ao longo de um ano. */
    public double getArvoresAno() { return arvoresAno; }

    /** Arvores necessarias para absorver esse CO2 em um unico dia. */
    public double getArvoresDia() { return arvoresDia; }

    public double getKmCarroEvitado() { return kmCarroEvitado; }
}
