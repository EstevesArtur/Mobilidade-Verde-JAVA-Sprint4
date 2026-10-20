package br.com.fiap.mobilidadeverde.dto;

/**
 * Corpo da requisicao POST /api/usuarios/{id}/favoritas.
 * Identifica a operadora que o usuario quer favoritar.
 */
public class FavoritaRequest {

    private Long idOperadora;

    public Long getIdOperadora() { return idOperadora; }
    public void setIdOperadora(Long idOperadora) { this.idOperadora = idOperadora; }
}
