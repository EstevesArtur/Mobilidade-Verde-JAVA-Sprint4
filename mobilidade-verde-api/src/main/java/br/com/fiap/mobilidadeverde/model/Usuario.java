package br.com.fiap.mobilidadeverde.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Usuário da plataforma SoulUp.
 * Espelha T_USUARIO.
 *
 * Observação importante: o saldo de pontos NÃO mora aqui.
 * Saldo é responsabilidade da {@link Carteira} (relação 1:1),
 * espelhando a separacao definida no modelo relacional.
 */
public class Usuario {

    private Long id;
    private String nome;
    private String email;
    private String hashSenha;
    private String cpfHash;
    private String cidade;
    private LocalDateTime criadoEm;

    public Usuario() {
    }

    public Usuario(Long id, String nome, String email,
                   String hashSenha, String cpfHash, String cidade) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.hashSenha = hashSenha;
        this.cpfHash = cpfHash;
        this.cidade = cidade;
        this.criadoEm = LocalDateTime.now();
    }

    // ----------------- getters / setters -----------------
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getHashSenha() { return hashSenha; }
    public void setHashSenha(String hashSenha) { this.hashSenha = hashSenha; }

    public String getCpfHash() { return cpfHash; }
    public void setCpfHash(String cpfHash) { this.cpfHash = cpfHash; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario)) return false;
        Usuario other = (Usuario) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Usuario{id=" + id + ", nome='" + nome + "', email='" + email + "'}";
    }
}
