package br.com.fiap.mobilidadeverde.util;

import br.com.fiap.mobilidadeverde.model.Operadora;

import java.security.SecureRandom;

/**
 * Gerador de código de cupom.
 *
 * Lê o "formatoCodigo" da operadora (ex: "TOP-XXXX-XXXX") e substitui
 * cada 'X' por um caractere alfanumérico aleatório.
 *
 * Usa SecureRandom (JDK puro) — em produção é melhor que Random comum
 * porque dificulta previsão estatística de códigos.
 */
public class CupomGenerator {

    private static final String ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private CupomGenerator() {
    }

    /**
     * Gera um código respeitando o template de {@link Operadora#getFormatoCodigo()}.
     * Cada 'X' (maiúsculo) do template é substituído por um caractere aleatório
     * do alfabeto (A-Z + 0-9). Qualquer outro caractere é mantido como está.
     *
     * Exemplo: "TOP-XXXX-XXXX" → "TOP-A7B3-X9K2".
     */
    public static String gerar(Operadora operadora) {
        if (operadora == null || operadora.getFormatoCodigo() == null) {
            throw new IllegalArgumentException("Operadora sem formato de código");
        }
        String template = operadora.getFormatoCodigo();
        StringBuilder sb = new StringBuilder(template.length());
        for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);
            if (c == 'X') {
                sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
