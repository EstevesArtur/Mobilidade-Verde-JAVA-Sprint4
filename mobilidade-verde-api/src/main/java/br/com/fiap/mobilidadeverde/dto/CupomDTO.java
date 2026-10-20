package br.com.fiap.mobilidadeverde.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * DTO de saída do ConversorService.
 * Carrega o que o "front" precisaria mostrar para o usuário após o resgate,
 * sem expor a entidade Voucher inteira.
 */
public class CupomDTO {

    private static final DateTimeFormatter FORMATO_DATA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Locale BR = Locale.forLanguageTag("pt-BR");

    private final String codigo;
    private final String nomeOperadora;
    private final int valorCentavos;
    private final int pontosDebitados;
    private final LocalDateTime expiraEm;

    public CupomDTO(String codigo, String nomeOperadora, int valorCentavos,
                    int pontosDebitados, LocalDateTime expiraEm) {
        this.codigo = codigo;
        this.nomeOperadora = nomeOperadora;
        this.valorCentavos = valorCentavos;
        this.pontosDebitados = pontosDebitados;
        this.expiraEm = expiraEm;
    }

    public String getCodigo() { return codigo; }
    public String getNomeOperadora() { return nomeOperadora; }
    public int getValorCentavos() { return valorCentavos; }
    public int getPontosDebitados() { return pontosDebitados; }
    public LocalDateTime getExpiraEm() { return expiraEm; }

    public String formatarValor() {
        return String.format(BR, "R$ %.2f", valorCentavos / 100.0);
    }

    public String formatarExpiraEm() {
        return expiraEm.format(FORMATO_DATA);
    }
}
