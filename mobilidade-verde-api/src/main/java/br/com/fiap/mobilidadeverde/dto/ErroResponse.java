package br.com.fiap.mobilidadeverde.dto;

import java.time.LocalDateTime;

/**
 * Corpo padrao devolvido pela API quando ocorre um erro.
 * Garante que o front receba sempre o mesmo formato, independente do
 * tipo de falha.
 */
public class ErroResponse {

    private final int status;
    private final String erro;
    private final String mensagem;
    private final LocalDateTime momento;

    public ErroResponse(int status, String erro, String mensagem) {
        this.status = status;
        this.erro = erro;
        this.mensagem = mensagem;
        this.momento = LocalDateTime.now();
    }

    public int getStatus() { return status; }
    public String getErro() { return erro; }
    public String getMensagem() { return mensagem; }
    public LocalDateTime getMomento() { return momento; }
}
