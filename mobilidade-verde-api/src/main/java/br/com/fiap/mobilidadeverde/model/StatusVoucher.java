package br.com.fiap.mobilidadeverde.model;

/**
 * Estados possíveis de um Voucher no seu ciclo de vida.
 * Espelha o CHECK CK_VOUCHER_STATUS da tabela T_VOUCHER.
 *
 *   GERADO    -> recém-criado, pode ser usado
 *   UTILIZADO -> embarque realizado, viagem registrada
 *   EXPIRADO  -> passou da janela de 24h sem uso
 *   CANCELADO -> estornado manualmente (não usado no MVP)
 */
public enum StatusVoucher {
    GERADO,
    UTILIZADO,
    EXPIRADO,
    CANCELADO
}
