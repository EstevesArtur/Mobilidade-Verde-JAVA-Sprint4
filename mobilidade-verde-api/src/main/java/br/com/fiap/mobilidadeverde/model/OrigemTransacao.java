package br.com.fiap.mobilidadeverde.model;

/**
 * Origem do movimento de pontos.
 * Espelha o CHECK CK_TRANSACAO_ORIGEM da tabela T_TRANSACAO_PONTOS.
 */
public enum OrigemTransacao {
    VOUCHER_GERACAO,
    ESTORNO,
    AJUSTE_MANUAL,
    OUTROS
}
