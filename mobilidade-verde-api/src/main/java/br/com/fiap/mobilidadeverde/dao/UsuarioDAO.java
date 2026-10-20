package br.com.fiap.mobilidadeverde.dao;

import org.springframework.stereotype.Repository;
import br.com.fiap.mobilidadeverde.conexao.ConexaoOracle;
import br.com.fiap.mobilidadeverde.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Acesso a dados de T_USUARIO.
 *
 * Esta e uma das tres entidades com CRUD COMPLETO (Create, Read,
 * Update e Delete), conforme decisao registrada na documentacao.
 *
 * Observacao sobre o Delete: T_USUARIO e referenciada por
 * T_CARTEIRA, T_VOUCHER, T_TRANSACAO_PONTOS e
 * T_USUARIO_OPERADORA_FAVORITA. Apagar um usuario com historico
 * exige remover as dependencias antes, na ordem inversa das FKs.
 * Quem orquestra isso e o AdminService.
 */
@Repository
public class UsuarioDAO {

    // ------------------------------------------------------------
    // CREATE
    // ------------------------------------------------------------

    /**
     * Insere um usuario abrindo e fechando sua propria conexao.
     *
     * @param usuario usuario a inserir; o id e preenchido pela sequence
     * @return o mesmo objeto, agora com idUsuario preenchido
     */
    public Usuario inserir(Usuario usuario) {
        try (Connection conexao = ConexaoOracle.abrir()) {
            return inserir(conexao, usuario);
        } catch (SQLException e) {
            throw new DAOException("Erro ao inserir usuario", e);
        }
    }

    /**
     * Insere um usuario reaproveitando uma conexao existente.
     * Usado quando a operacao participa de uma transacao maior.
     */
    public Usuario inserir(Connection conexao, Usuario usuario) throws SQLException {
        String sql = "INSERT INTO T_USUARIO "
            + "(id_usuario, nome, email, hash_senha, cpf_hash, cidade, criado_em) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        Long id = SequenceHelper.proximoValor(conexao, "SEQ_USUARIO");
        // O instante de criacao e o do INSERT, sempre truncado no segundo
        // para que memoria e banco guardem exatamente o mesmo valor.
        usuario.setCriadoEm(SequenceHelper.agora());

        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setString(2, usuario.getNome());
            ps.setString(3, usuario.getEmail());
            ps.setString(4, usuario.getHashSenha());
            ps.setString(5, usuario.getCpfHash());
            ps.setString(6, usuario.getCidade());
            ps.setTimestamp(7, Timestamp.valueOf(usuario.getCriadoEm()));
            ps.executeUpdate();
        }

        usuario.setId(id);
        return usuario;
    }

    // ------------------------------------------------------------
    // READ
    // ------------------------------------------------------------

    public Usuario buscarPorId(Long idUsuario) {
        String sql = "SELECT id_usuario, nome, email, hash_senha, cpf_hash, cidade, criado_em "
            + "FROM T_USUARIO WHERE id_usuario = ?";

        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setLong(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? montar(rs) : null;
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao buscar usuario por id " + idUsuario, e);
        }
    }

    public Usuario buscarPorEmail(String email) {
        String sql = "SELECT id_usuario, nome, email, hash_senha, cpf_hash, cidade, criado_em "
            + "FROM T_USUARIO WHERE email = ?";

        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? montar(rs) : null;
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao buscar usuario por email " + email, e);
        }
    }

    public List<Usuario> listarTodos() {
        String sql = "SELECT id_usuario, nome, email, hash_senha, cpf_hash, cidade, criado_em "
            + "FROM T_USUARIO ORDER BY nome";

        List<Usuario> usuarios = new ArrayList<>();
        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                usuarios.add(montar(rs));
            }
            return usuarios;
        } catch (SQLException e) {
            throw new DAOException("Erro ao listar usuarios", e);
        }
    }

    // ------------------------------------------------------------
    // UPDATE
    // ------------------------------------------------------------

    /**
     * Atualiza os dados cadastrais do usuario.
     * hash_senha e cpf_hash nao sao alterados por este metodo:
     * credencial e documento tem fluxo proprio de alteracao.
     *
     * @return true se alguma linha foi alterada
     */
    public boolean atualizar(Usuario usuario) {
        String sql = "UPDATE T_USUARIO SET nome = ?, email = ?, cidade = ? WHERE id_usuario = ?";

        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setString(1, usuario.getNome());
            ps.setString(2, usuario.getEmail());
            ps.setString(3, usuario.getCidade());
            ps.setLong(4, usuario.getId());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DAOException("Erro ao atualizar usuario " + usuario.getId(), e);
        }
    }

    // ------------------------------------------------------------
    // DELETE
    // ------------------------------------------------------------

    /**
     * Remove o usuario. Falha com ORA-02292 se ainda existirem
     * registros dependentes, o que e o comportamento correto:
     * a FK protege o historico financeiro contra exclusao acidental.
     *
     * @return true se alguma linha foi removida
     */
    public boolean deletar(Long idUsuario) {
        try (Connection conexao = ConexaoOracle.abrir()) {
            return deletar(conexao, idUsuario);
        } catch (SQLException e) {
            throw new DAOException("Erro ao deletar usuario " + idUsuario, e);
        }
    }

    public boolean deletar(Connection conexao, Long idUsuario) throws SQLException {
        String sql = "DELETE FROM T_USUARIO WHERE id_usuario = ?";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, idUsuario);
            return ps.executeUpdate() > 0;
        }
    }

    // ------------------------------------------------------------
    // MAPEAMENTO
    // ------------------------------------------------------------

    /** Converte a linha corrente do ResultSet em objeto de dominio. */
    private Usuario montar(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setId(rs.getLong("id_usuario"));
        usuario.setNome(rs.getString("nome"));
        usuario.setEmail(rs.getString("email"));
        usuario.setHashSenha(rs.getString("hash_senha"));
        usuario.setCpfHash(rs.getString("cpf_hash"));
        usuario.setCidade(rs.getString("cidade"));
        usuario.setCriadoEm(SequenceHelper.paraLocalDateTime(rs.getTimestamp("criado_em")));
        return usuario;
    }
}
