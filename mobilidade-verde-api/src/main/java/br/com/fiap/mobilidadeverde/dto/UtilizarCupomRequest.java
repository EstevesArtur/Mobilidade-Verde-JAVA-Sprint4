package br.com.fiap.mobilidadeverde.dto;

/**
 * Corpo da requisicao POST /api/vouchers/{codigo}/utilizar.
 * Informa qual operadora esta validando o cupom no embarque.
 */
public class UtilizarCupomRequest {

    private Long idOperadora;

    public Long getIdOperadora() { return idOperadora; }
    public void setIdOperadora(Long idOperadora) { this.idOperadora = idOperadora; }
}
