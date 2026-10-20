package br.com.fiap.mobilidadeverde.util;

import java.util.regex.Pattern;

/**
 * Validadores reaproveitáveis — espelham as funções do simulador Python.
 *
 * Estão aqui para facilitar evolução para uma camada de cadastro de usuário
 * (Sprint 3/4). No fluxo do main já temos cobertura para garantir que
 * os métodos compilam e funcionam.
 */
public class ValidadorUtil {

    // RFC 5322 simplificada — não tenta cobrir todos os casos exóticos.
    private static final Pattern PADRAO_EMAIL =
        Pattern.compile("^[\\w.\\-]+@[\\w.\\-]+\\.\\w+$");

    private ValidadorUtil() {
    }

    /**
     * Valida formato básico de e-mail.
     */
    public static boolean validarEmail(String email) {
        if (email == null) return false;
        return PADRAO_EMAIL.matcher(email).matches();
    }

    /**
     * Valida CPF pelos dígitos verificadores.
     * Algoritmo idêntico ao do simulador Python (módulo 11).
     */
    public static boolean validarCpf(String cpf) {
        if (cpf == null) return false;
        // remove tudo que não é dígito
        String numeros = cpf.replaceAll("\\D", "");
        if (numeros.length() != 11) return false;
        // rejeita CPFs com todos os dígitos iguais (ex: 11111111111)
        if (numeros.chars().distinct().count() == 1) return false;

        for (int i = 9; i <= 10; i++) {
            int soma = 0;
            for (int j = 0; j < i; j++) {
                soma += (numeros.charAt(j) - '0') * (i + 1 - j);
            }
            int digitoCalculado = ((soma * 10) % 11) % 10;
            int digitoEsperado = numeros.charAt(i) - '0';
            if (digitoCalculado != digitoEsperado) return false;
        }
        return true;
    }
}
