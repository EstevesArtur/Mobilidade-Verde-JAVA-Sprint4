package br.com.fiap.mobilidadeverde.dao;

import org.springframework.stereotype.Repository;
import br.com.fiap.mobilidadeverde.conexao.ConexaoOracle;
import br.com.fiap.mobilidadeverde.model.Modal;
import br.com.fiap.mobilidadeverde.model.Operadora;
import br.com.fiap.mobilidadeverde.model.Usuario;
import br.com.fiap.mobilidadeverde.model.UsuarioOperadoraFavorita;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Acesso a dados de T_USUARIO_OPERADORA_FAVORITA.
 *
 * Esta e a tabela ASSOCIATIVA que resolve o relacionamento N:N
 * entre usuario e operadora, com atributos proprios
 * (ordem_preferencia e favoritado_em).
 *
 * Entidade com CRUD COMPLETO, e por um motivo tecnico concreto:
 * nenhuma chave estrangeira do esquema aponta para ela. E a unica
 * das oito tabelas onde o DELETE funciona sem exigir remocao previa
 * de dependentes, o que a torna o lugar natural para demonstrar a
 * operacao completa. Alem disso, favoritar e desfavoritar uma
 * operadora e exatamente o tipo de acao que o usuario faz e desfaz
 * o tempo todo no produto real.
 *
 * A leitura traz o grafo completo (usuario e operadora ja
 * instanciados) atraves de um JOIN das tres tabelas, evitando que a
 * camada superior receba objetos pela metade.
 */
@Repository
public class FavoritaDAO {

    // ------------------------------------------------------------
    // CREATE
    // ------------------------------------------------------------

    public UsuarioOperadoraFavorita inserir(UsuarioOperadoraFavorita favorita) {
        try (Connection conexao = ConexaoOracle.abrir()) {
            return inserir(conexao, favorita);
        } catch (SQLException e) {
            throw new DAOException(
                "Erro ao favoritar operadora. Cada operadora so pode ser "
                + "favoritada uma vez por usuario (UK_FAVORITA_USR_OPE).", e);
        }
    }

    public UsuarioOperadoraFavorita inserir(Connection conexao,
                                            UsuarioOperadoraFavorita favorita)
                                            throws SQLException {
        String sql = "INSERT INTO T_USUARIO_OPERADORA_FAVORITA "
            + "(id_favorita, id_usuario, id_operadora, ordem_preferencia, favoritado_em) "
            + "VALUES (?, ?, ?, ?, ?)";

        Long id = SequenceHelper.proximoValor(conexao, "SEQ_FAVORITA");
        favorita.setFavoritadoEm(SequenceHelper.agora());

        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, favorita.getUsuario().getId());
            ps.setLong(3, favorita.getOperadora().getId());
            ps.setInt(4, favorita.getOrdemPreferencia());
            ps.setTimestamp(5, SequenceHelper.paraTimestamp(favorita.getFavoritadoEm()));
            ps.executeUpdate();
        }

        favorita.setId(id);
        return favorita;
    }

    // ------------------------------------------------------------
    // READ
    // ------------------------------------------------------------

    public UsuarioOperadoraFavorita buscarPorId(Long idFavorita) {
        String sql = montarSelect() + " WHERE f.id_favorita = ?";

        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setLong(1, idFavorita);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? montar(rs) : null;
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao buscar favorita " + idFavorita, e);
        }
    }

    /**
     * Lista as operadoras favoritas do usuario na ordem de
     * preferencia que ele mesmo definiu. E essa ordem que alimenta
     * a sequencia dos cards de operadora na tela de resgate.
     */
    public List<UsuarioOperadoraFavorita> listarPorUsuario(Long idUsuario) {
        String sql = montarSelect()
            + " WHERE f.id_usuario = ? ORDER BY f.ordem_preferencia";

        List<UsuarioOperadoraFavorita> favoritas = new ArrayList<>();
        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setLong(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    favoritas.add(montar(rs));
                }
            }
            return favoritas;
        } catch (SQLException e) {
            throw new DAOException("Erro ao listar favoritas do usuario " + idUsuario, e);
        }
    }

    // ------------------------------------------------------------
    // UPDATE
    // ------------------------------------------------------------

    /**
     * Altera a posicao de uma operadora na lista de preferencias.
     * CK_FAVORITA_ORDEM exige valor maior ou igual a 1.
     */
    public boolean atualizarOrdem(Long idFavorita, int novaOrdem) {
        if (novaOrdem < 1) {
            throw new IllegalArgumentException(
                "Ordem de preferencia deve ser maior ou igual a 1 (CK_FAVORITA_ORDEM)");
        }
        String sql = "UPDATE T_USUARIO_OPERADORA_FAVORITA SET ordem_preferencia = ? "
            + "WHERE id_favorita = ?";

        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setInt(1, novaOrdem);
            ps.setLong(2, idFavorita);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DAOException("Erro ao atualizar ordem da favorita " + idFavorita, e);
        }
    }

    // ------------------------------------------------------------
    // DELETE
    // ------------------------------------------------------------

    /**
     * Desfavorita uma operadora. Nenhuma FK aponta para esta tabela,
     * entao a remocao e sempre possivel.
     */
    public boolean deletar(Long idFavorita) {
        String sql = "DELETE FROM T_USUARIO_OPERADORA_FAVORITA WHERE id_favorita = ?";

        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setLong(1, idFavorita);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DAOException("Erro ao deletar favorita " + idFavorita, e);
        }
    }

    public int deletarPorUsuario(Connection conexao, Long idUsuario) throws SQLException {
        String sql = "DELETE FROM T_USUARIO_OPERADORA_FAVORITA WHERE id_usuario = ?";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, idUsuario);
            return ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------
    // MAPEAMENTO
    // ------------------------------------------------------------

    private String montarSelect() {
        return "SELECT f.id_favorita, f.ordem_preferencia, f.favoritado_em, "
            + "u.id_usuario, u.nome AS usuario_nome, u.email, u.hash_senha, "
            + "u.cpf_hash, u.cidade AS usuario_cidade, u.criado_em, "
            + "o.id_operadora, o.nome AS operadora_nome, o.cidade AS operadora_cidade, "
            + "o.modal, o.valor_passagem_centavos, o.fator_co2_kg_km, "
            + "o.formato_codigo, o.ativo "
            + "FROM T_USUARIO_OPERADORA_FAVORITA f "
            + "INNER JOIN T_USUARIO u ON u.id_usuario = f.id_usuario "
            + "INNER JOIN T_OPERADORA_TRANSPORTE o ON o.id_operadora = f.id_operadora";
    }

    private UsuarioOperadoraFavorita montar(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setId(rs.getLong("id_usuario"));
        usuario.setNome(rs.getString("usuario_nome"));
        usuario.setEmail(rs.getString("email"));
        usuario.setHashSenha(rs.getString("hash_senha"));
        usuario.setCpfHash(rs.getString("cpf_hash"));
        usuario.setCidade(rs.getString("usuario_cidade"));
        usuario.setCriadoEm(SequenceHelper.paraLocalDateTime(rs.getTimestamp("criado_em")));

        Operadora operadora = new Operadora();
        operadora.setId(rs.getLong("id_operadora"));
        operadora.setNome(rs.getString("operadora_nome"));
        operadora.setCidade(rs.getString("operadora_cidade"));
        operadora.setModal(Modal.valueOf(rs.getString("modal")));
        operadora.setValorPassagemCentavos(rs.getInt("valor_passagem_centavos"));
        operadora.setFatorCo2KgKm(rs.getDouble("fator_co2_kg_km"));
        operadora.setFormatoCodigo(rs.getString("formato_codigo"));
        operadora.setAtivo(SequenceHelper.paraBoolean(rs.getString("ativo")));

        UsuarioOperadoraFavorita favorita = new UsuarioOperadoraFavorita();
        favorita.setId(rs.getLong("id_favorita"));
        favorita.setUsuario(usuario);
        favorita.setOperadora(operadora);
        favorita.setOrdemPreferencia(rs.getInt("ordem_preferencia"));
        favorita.setFavoritadoEm(
            SequenceHelper.paraLocalDateTime(rs.getTimestamp("favoritado_em")));
        return favorita;
    }
}
