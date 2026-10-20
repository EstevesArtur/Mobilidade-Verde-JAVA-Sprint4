package br.com.fiap.mobilidadeverde.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utilitário de hash usando o JDK puro:
 *   - SHA-256 (MessageDigest) para o cpf_hash exigido pela LGPD (decisão 3.3 do PDF de banco).
 *   - HMAC-SHA256 (javax.crypto.Mac) para o hash_validacao do Voucher
 *     (decisão 3.6 do PDF de banco — chave compartilhada SoulUp ↔ operadora).
 *
 * Sem dependência externa: nem Apache Commons, nem Bouncy Castle.
 */
public class HashUtil {

    // construtor privado — classe utilitária
    private HashUtil() {
    }

    /**
     * Devolve o hash SHA-256 do texto, em hexadecimal (64 caracteres).
     * Uso: cpf_hash (LGPD), fingerprints simples.
     */
    public static String sha256(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(texto.getBytes(StandardCharsets.UTF_8));
            return paraHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 é garantido pelo JDK; se cair aqui, o ambiente está quebrado.
            throw new IllegalStateException("SHA-256 indisponível no JDK", e);
        }
    }

    /**
     * Devolve o HMAC-SHA256 da mensagem, em hexadecimal (64 caracteres),
     * usando a chave compartilhada informada.
     *
     * Em produção, a chave é o segredo combinado entre SoulUp e a operadora
     * (TOP, SPTrans). No MVP, usamos uma chave mockada constante — basta
     * trocar por variável de ambiente / secret manager na Sprint 3/4.
     */
    public static String hmacSha256(String mensagem, String chave) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec spec = new SecretKeySpec(
                chave.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(spec);
            byte[] bytes = mac.doFinal(mensagem.getBytes(StandardCharsets.UTF_8));
            return paraHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("HmacSHA256 indisponível no JDK", e);
        } catch (java.security.InvalidKeyException e) {
            throw new IllegalStateException("Chave HMAC inválida", e);
        }
    }

    private static String paraHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) sb.append('0');
            sb.append(hex);
        }
        return sb.toString();
    }
}
