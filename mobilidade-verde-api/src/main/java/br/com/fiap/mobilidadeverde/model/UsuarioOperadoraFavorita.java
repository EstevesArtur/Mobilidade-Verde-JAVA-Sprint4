package br.com.fiap.mobilidadeverde.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Tabela associativa N:N entre Usuario e Operadora, com atributos próprios.
 * Espelha T_USUARIO_OPERADORA_FAVORITA.
 *
 * É o N:N exigido pela disciplina de Banco — o atributo "ordemPreferencia"
 * dá sentido funcional ao relacionamento (operadora favorita do usuário,
 * ranqueada).
 */
public class UsuarioOperadoraFavorita {

    private Long id;
    private Usuario usuario;
    private Operadora operadora;
    private int ordemPreferencia;
    private LocalDateTime favoritadoEm;

    public UsuarioOperadoraFavorita() {
    }

    public UsuarioOperadoraFavorita(Long id, Usuario usuario,
                                    Operadora operadora, int ordemPreferencia) {
        this.id = id;
        this.usuario = usuario;
        this.operadora = operadora;
        this.ordemPreferencia = ordemPreferencia;
        this.favoritadoEm = LocalDateTime.now();
    }

    // ----------------- getters / setters -----------------
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Operadora getOperadora() { return operadora; }
    public void setOperadora(Operadora operadora) { this.operadora = operadora; }

    public int getOrdemPreferencia() { return ordemPreferencia; }
    public void setOrdemPreferencia(int ordemPreferencia) {
        this.ordemPreferencia = ordemPreferencia;
    }

    public LocalDateTime getFavoritadoEm() { return favoritadoEm; }
    public void setFavoritadoEm(LocalDateTime favoritadoEm) {
        this.favoritadoEm = favoritadoEm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsuarioOperadoraFavorita)) return false;
        UsuarioOperadoraFavorita other = (UsuarioOperadoraFavorita) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "UsuarioOperadoraFavorita{usuario="
            + (usuario != null ? usuario.getNome() : "null")
            + ", operadora=" + (operadora != null ? operadora.getNome() : "null")
            + ", ordem=" + ordemPreferencia + "}";
    }
}
