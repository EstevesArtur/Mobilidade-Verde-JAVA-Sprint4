package br.com.fiap.mobilidadeverde.dto;

/**
 * Resultado de uma validação (anti-fraude, regras de negócio).
 * Imutável depois de criado.
 */
public class ResultadoValidacao {

    private final boolean ok;
    private final String motivo;

    public ResultadoValidacao(boolean ok, String motivo) {
        this.ok = ok;
        this.motivo = motivo;
    }

    public static ResultadoValidacao aprovado() {
        return new ResultadoValidacao(true, "OK");
    }

    public static ResultadoValidacao reprovado(String motivo) {
        return new ResultadoValidacao(false, motivo);
    }

    public boolean isOk() { return ok; }
    public String getMotivo() { return motivo; }
}
