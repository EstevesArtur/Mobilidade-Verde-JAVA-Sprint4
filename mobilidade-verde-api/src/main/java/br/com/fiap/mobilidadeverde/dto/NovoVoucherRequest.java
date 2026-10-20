package br.com.fiap.mobilidadeverde.dto;

/**
 * Corpo da requisicao POST /api/vouchers.
 * Identifica quem resgata, em qual operadora e em qual faixa de valor.
 */
public class NovoVoucherRequest {

    private Long idUsuario;
    private Long idOperadora;
    private Long idFaixa;

    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }

    public Long getIdOperadora() { return idOperadora; }
    public void setIdOperadora(Long idOperadora) { this.idOperadora = idOperadora; }

    public Long getIdFaixa() { return idFaixa; }
    public void setIdFaixa(Long idFaixa) { this.idFaixa = idFaixa; }
}
