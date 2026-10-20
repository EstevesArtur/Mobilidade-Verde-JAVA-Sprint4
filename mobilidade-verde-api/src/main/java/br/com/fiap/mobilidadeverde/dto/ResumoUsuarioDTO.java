package br.com.fiap.mobilidadeverde.dto;

import java.util.Locale;

/**
 * DTO com um resumo da situação do usuário (nome + saldo).
 * Pensado para a tela inicial do app.
 */
public class ResumoUsuarioDTO {

    private static final Locale BR = Locale.forLanguageTag("pt-BR");

    private final String nomeUsuario;
    private final int saldoPontos;

    public ResumoUsuarioDTO(String nomeUsuario, int saldoPontos) {
        this.nomeUsuario = nomeUsuario;
        this.saldoPontos = saldoPontos;
    }

    public String getNomeUsuario() { return nomeUsuario; }
    public int getSaldoPontos() { return saldoPontos; }

    public String formatarValorReais() {
        // 100 pts = R$ 1,00
        return String.format(BR, "R$ %.2f", saldoPontos / 100.0);
    }
}
