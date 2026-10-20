package br.com.fiap.mobilidadeverde.dto;

/**
 * Corpo das requisicoes POST e PUT de /api/usuarios.
 * A senha e o CPF chegam em texto e sao convertidos em hash antes de
 * serem gravados, de modo que o banco nunca guarda o valor original.
 */
public class UsuarioRequest {

    private String nome;
    private String email;
    private String senha;
    private String cpf;
    private String cidade;
    private Integer saldoInicial;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public Integer getSaldoInicial() { return saldoInicial; }
    public void setSaldoInicial(Integer saldoInicial) { this.saldoInicial = saldoInicial; }
}
