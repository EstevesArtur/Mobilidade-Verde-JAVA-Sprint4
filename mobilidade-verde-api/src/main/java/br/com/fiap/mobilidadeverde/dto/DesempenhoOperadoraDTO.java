package br.com.fiap.mobilidadeverde.dto;

/**
 * Uma linha do relatorio de desempenho das operadoras parceiras.
 *
 * Mostra quantos cupons cada operadora emitiu e quantos viraram viagem.
 * Corresponde a consulta C05 entregue na disciplina de Building
 * Relational Database.
 */
public class DesempenhoOperadoraDTO {

    private final String operadora;
    private final String modal;
    private final String cidade;
    private final boolean ativo;
    private final int cuponsEmitidos;
    private final int utilizados;
    private final int expirados;
    private final double taxaUsoPercentual;

    public DesempenhoOperadoraDTO(String operadora, String modal, String cidade,
                                  boolean ativo, int cuponsEmitidos, int utilizados,
                                  int expirados, double taxaUsoPercentual) {
        this.operadora = operadora;
        this.modal = modal;
        this.cidade = cidade;
        this.ativo = ativo;
        this.cuponsEmitidos = cuponsEmitidos;
        this.utilizados = utilizados;
        this.expirados = expirados;
        this.taxaUsoPercentual = taxaUsoPercentual;
    }

    public String getOperadora() { return operadora; }
    public String getModal() { return modal; }
    public String getCidade() { return cidade; }
    public boolean isAtivo() { return ativo; }
    public int getCuponsEmitidos() { return cuponsEmitidos; }
    public int getUtilizados() { return utilizados; }
    public int getExpirados() { return expirados; }
    public double getTaxaUsoPercentual() { return taxaUsoPercentual; }
}
