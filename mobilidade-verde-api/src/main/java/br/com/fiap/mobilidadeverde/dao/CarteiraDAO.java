package br.com.fiap.mobilidadeverde.dao;

import org.springframework.stereotype.Repository;
import br.com.fiap.mobilidadeverde.conexao.ConexaoOracle;
import br.com.fiap.mobilidadeverde.model.Carteira;
import br.com.fiap.mobilidadeverde.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Acesso a dados de T_CARTEIRA.
 *
 * A carteira guarda o saldo de Pontos ECOA e tem relacao 1:1 com o
 * usuario (RN01), garantida no banco por UK_CARTEIRA_USUARIO.
 *
 * ATENCAO A UMA DIFERENCA IMPORTANTE EM RELACAO A SPRINT 1+2:
 * quando o repositorio era em memoria, chamar carteira.debitar()
 * ja alterava o objeto guardado na lista, e a mudanca "persistia"
 * de graca. Com banco de dados isso nao acontece: apos qualquer
 * debito ou credito e obrigatorio chamar atualizarSaldo(), senao a
 * alteracao vive apenas em memoria e some na proxima leitura.
 */
@Repository
public class CarteiraDAO {

    // ------------------------------------------------------------
    // CREATE
    // ------------------------------------------------------------

    public Carteira inserir(Carteira carteira) {
        try (Connection conexao = ConexaoOracle.abrir()) {
            return inserir(conexao, carteira);
        } catch (SQLException e) {
            throw new DAOException("Erro ao inserir carteira", e);
        }
    }

    public Carteira inserir(Connection conexao, Carteira carteira) throws SQLException {
        String sql = "INSERT INTO T_CARTEIRA "
            + "(id_carteira, id_usuario, saldo_pontos, atualizado_em) VALUES (?, ?, ?, ?)";

        Long id = SequenceHelper.proximoValor(conexao, "SEQ_CARTEIRA");
        carteira.setAtualizadoEm(SequenceHelper.agora());

        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, carteira.getUsuario().getId());
            ps.setInt(3, carteira.getSaldoPontos());
            ps.setTimestamp(4, SequenceHelper.paraTimestamp(carteira.getAtualizadoEm()));
            ps.executeUpdate();
        }

        carteira.setId(id);
        return carteira;
    }

    // ------------------------------------------------------------
    // READ
    // ------------------------------------------------------------

    /**
     * Recupera a carteira de um usuario ja com o objeto Usuario
     * associado, para que as camadas superiores nao recebam um
     * grafo pela metade.
     */
    public Carteira buscarPorUsuario(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) {
            return null;
        }
        String sql = "SELECT id_carteira, id_usuario, saldo_pontos, atualizado_em "
            + "FROM T_CARTEIRA WHERE id_usuario = ?";

        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setLong(1, usuario.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Carteira carteira = new Carteira();
                carteira.setId(rs.getLong("id_carteira"));
                carteira.setUsuario(usuario);
                carteira.setSaldoPontos(rs.getInt("saldo_pontos"));
                carteira.setAtualizadoEm(
                    SequenceHelper.paraLocalDateTime(rs.getTimestamp("atualizado_em")));
                return carteira;
            }
        } catch (SQLException e) {
            throw new DAOException(
                "Erro ao buscar carteira do usuario " + usuario.getId(), e);
        }
    }

    public List<Carteira> listarTodas() {
        String sql = "SELECT c.id_carteira, c.saldo_pontos, c.atualizado_em, "
            + "u.id_usuario, u.nome, u.email, u.hash_senha, u.cpf_hash, u.cidade, u.criado_em "
            + "FROM T_CARTEIRA c INNER JOIN T_USUARIO u ON u.id_usuario = c.id_usuario "
            + "ORDER BY u.nome";

        List<Carteira> carteiras = new ArrayList<>();
        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Usuario usuario = new Usuario();
                usuario.setId(rs.getLong("id_usuario"));
                usuario.setNome(rs.getString("nome"));
                usuario.setEmail(rs.getString("email"));
                usuario.setHashSenha(rs.getString("hash_senha"));
                usuario.setCpfHash(rs.getString("cpf_hash"));
                usuario.setCidade(rs.getString("cidade"));
                usuario.setCriadoEm(
                    SequenceHelper.paraLocalDateTime(rs.getTimestamp("criado_em")));

                Carteira carteira = new Carteira();
                carteira.setId(rs.getLong("id_carteira"));
                carteira.setUsuario(usuario);
                carteira.setSaldoPontos(rs.getInt("saldo_pontos"));
                carteira.setAtualizadoEm(
                    SequenceHelper.paraLocalDateTime(rs.getTimestamp("atualizado_em")));
                carteiras.add(carteira);
            }
            return carteiras;
        } catch (SQLException e) {
            throw new DAOException("Erro ao listar carteiras", e);
        }
    }

    // ------------------------------------------------------------
    // UPDATE
    // ------------------------------------------------------------

    public boolean atualizarSaldo(Carteira carteira) {
        try (Connection conexao = ConexaoOracle.abrir()) {
            return atualizarSaldo(conexao, carteira);
        } catch (SQLException e) {
            throw new DAOException("Erro ao atualizar saldo da carteira", e);
        }
    }

    /**
     * Grava o saldo corrente do objeto no banco.
     * Participa da transacao do ConversorService quando recebe a
     * conexao de fora.
     */
    public boolean atualizarSaldo(Connection conexao, Carteira carteira) throws SQLException {
        String sql = "UPDATE T_CARTEIRA SET saldo_pontos = ?, atualizado_em = ? "
            + "WHERE id_carteira = ?";

        carteira.setAtualizadoEm(SequenceHelper.agora());

        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setInt(1, carteira.getSaldoPontos());
            ps.setTimestamp(2, SequenceHelper.paraTimestamp(carteira.getAtualizadoEm()));
            ps.setLong(3, carteira.getId());
            return ps.executeUpdate() > 0;
        }
    }

    // ------------------------------------------------------------
    // DELETE
    // ------------------------------------------------------------

    public boolean deletarPorUsuario(Connection conexao, Long idUsuario) throws SQLException {
        String sql = "DELETE FROM T_CARTEIRA WHERE id_usuario = ?";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, idUsuario);
            return ps.executeUpdate() > 0;
        }
    }
}
