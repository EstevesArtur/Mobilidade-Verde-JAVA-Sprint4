package br.com.fiap.mobilidadeverde.dto;

import br.com.fiap.mobilidadeverde.model.Usuario;

/**
 * Representacao publica do usuario.
 *
 * Existe para que hash_senha e cpf_hash NUNCA saiam da API. A entidade
 * Usuario carrega esses campos porque eles existem na tabela, mas eles
 * sao dado sensivel e nao devem trafegar ate o front-end.
 */
public class UsuarioResponse {

    private Long id;
    private String nome;
    private String email;
    private String cidade;
    private Integer saldoPontos;

    public UsuarioResponse(Usuario usuario, Integer saldoPontos) {
        this.id = usuario.getId();
        this.nome = usuario.getNome();
        this.email = usuario.getEmail();
        this.cidade = usuario.getCidade();
        this.saldoPontos = saldoPontos;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getCidade() { return cidade; }
    public Integer getSaldoPontos() { return saldoPontos; }
}
