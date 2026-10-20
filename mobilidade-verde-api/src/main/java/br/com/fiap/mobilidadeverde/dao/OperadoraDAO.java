package br.com.fiap.mobilidadeverde.dao;

import org.springframework.stereotype.Repository;
import br.com.fiap.mobilidadeverde.conexao.ConexaoOracle;
import br.com.fiap.mobilidadeverde.dto.DesempenhoOperadoraDTO;
import br.com.fiap.mobilidadeverde.model.Modal;
import br.com.fiap.mobilidadeverde.model.Operadora;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Acesso a dados de T_OPERADORA_TRANSPORTE.
 *
 * Entidade com CRUD COMPLETO. E o catalogo de operadoras parceiras,
 * dado administrado pela Prospera e nao pelo usuario final, o que
 * torna as quatro operacoes legitimas no produto real: cadastrar
 * uma nova parceira, consultar, corrigir a tarifa e remover uma
 * parceria que nunca chegou a emitir cupom.
 *
 * O enum Modal vira VARCHAR2 no banco, validado por
 * CK_OPERADORA_MODAL. A flag ativo vira CHAR(1) 'S'/'N'.
 */
@Repository
public class OperadoraDAO {

    private static final String COLUNAS =
        "id_operadora, nome, cidade, modal, valor_passagem_centavos, "
        + "fator_co2_kg_km, formato_codigo, ativo";

    // ------------------------------------------------------------
    // CREATE
    // ------------------------------------------------------------

    public Operadora inserir(Operadora operadora) {
        try (Connection conexao = ConexaoOracle.abrir()) {
            return inserir(conexao, operadora);
        } catch (SQLException e) {
            throw new DAOException("Erro ao inserir operadora " + operadora.getNome(), e);
        }
    }

    public Operadora inserir(Connection conexao, Operadora operadora) throws SQLException {
        String sql = "INSERT INTO T_OPERADORA_TRANSPORTE (" + COLUNAS + ") "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Long id = SequenceHelper.proximoValor(conexao, "SEQ_OPERADORA");

        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setString(2, operadora.getNome());
            ps.setString(3, operadora.getCidade());
            ps.setString(4, operadora.getModal().name());
            ps.setInt(5, operadora.getValorPassagemCentavos());
            ps.setDouble(6, operadora.getFatorCo2KgKm());
            ps.setString(7, operadora.getFormatoCodigo());
            ps.setString(8, SequenceHelper.paraFlag(operadora.isAtivo()));
            ps.executeUpdate();
        }

        operadora.setId(id);
        return operadora;
    }

    // ------------------------------------------------------------
    // READ
    // ------------------------------------------------------------

    public Operadora buscarPorId(Long idOperadora) {
        String sql = "SELECT " + COLUNAS + " FROM T_OPERADORA_TRANSPORTE WHERE id_operadora = ?";

        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setLong(1, idOperadora);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? montar(rs) : null;
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao buscar operadora " + idOperadora, e);
        }
    }

    public Operadora buscarPorNome(String nome) {
        String sql = "SELECT " + COLUNAS + " FROM T_OPERADORA_TRANSPORTE WHERE nome = ?";

        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setString(1, nome);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? montar(rs) : null;
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao buscar operadora por nome " + nome, e);
        }
    }

    /** Somente operadoras ativas: e o catalogo que o usuario enxerga (RN06). */
    public List<Operadora> listarAtivas() {
        return listar("SELECT " + COLUNAS
            + " FROM T_OPERADORA_TRANSPORTE WHERE ativo = 'S' ORDER BY nome");
    }

    /** Todas, inclusive desativadas: visao administrativa. */
    public List<Operadora> listarTodas() {
        return listar("SELECT " + COLUNAS + " FROM T_OPERADORA_TRANSPORTE ORDER BY nome");
    }

    private List<Operadora> listar(String sql) {
        List<Operadora> operadoras = new ArrayList<>();
        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                operadoras.add(montar(rs));
            }
            return operadoras;
        } catch (SQLException e) {
            throw new DAOException("Erro ao listar operadoras", e);
        }
    }

    // ------------------------------------------------------------
    // UPDATE
    // ------------------------------------------------------------

    public boolean atualizar(Operadora operadora) {
        String sql = "UPDATE T_OPERADORA_TRANSPORTE SET nome = ?, cidade = ?, modal = ?, "
            + "valor_passagem_centavos = ?, fator_co2_kg_km = ?, formato_codigo = ?, ativo = ? "
            + "WHERE id_operadora = ?";

        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setString(1, operadora.getNome());
            ps.setString(2, operadora.getCidade());
            ps.setString(3, operadora.getModal().name());
            ps.setInt(4, operadora.getValorPassagemCentavos());
            ps.setDouble(5, operadora.getFatorCo2KgKm());
            ps.setString(6, operadora.getFormatoCodigo());
            ps.setString(7, SequenceHelper.paraFlag(operadora.isAtivo()));
            ps.setLong(8, operadora.getId());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DAOException("Erro ao atualizar operadora " + operadora.getId(), e);
        }
    }

    // ------------------------------------------------------------
    // DELETE
    // ------------------------------------------------------------

    /**
     * Remove a operadora. Se ela ja emitiu cupom ou foi favoritada,
     * a FK impede a exclusao (ORA-02292) e o metodo propaga a falha
     * como DAOException. Essa e a protecao correta: desativar uma
     * parceria encerrada e papel do UPDATE em ativo = 'N' (RN06),
     * nao do DELETE.
     */
    public boolean deletar(Long idOperadora) {
        String sql = "DELETE FROM T_OPERADORA_TRANSPORTE WHERE id_operadora = ?";

        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setLong(1, idOperadora);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DAOException("Erro ao deletar operadora " + idOperadora
                + ". Operadoras com cupom emitido nao podem ser removidas.", e);
        }
    }

    // ------------------------------------------------------------
    // MAPEAMENTO
    // ------------------------------------------------------------

    private Operadora montar(ResultSet rs) throws SQLException {
        Operadora operadora = new Operadora();
        operadora.setId(rs.getLong("id_operadora"));
        operadora.setNome(rs.getString("nome"));
        operadora.setCidade(rs.getString("cidade"));
        operadora.setModal(Modal.valueOf(rs.getString("modal")));
        operadora.setValorPassagemCentavos(rs.getInt("valor_passagem_centavos"));
        operadora.setFatorCo2KgKm(rs.getDouble("fator_co2_kg_km"));
        operadora.setFormatoCodigo(rs.getString("formato_codigo"));
        operadora.setAtivo(SequenceHelper.paraBoolean(rs.getString("ativo")));
        return operadora;
    }

    /**
     * Relatorio de desempenho das operadoras parceiras.
     *
     * Reproduz a consulta C05 da disciplina de Building Relational
     * Database. O LEFT JOIN mantem na lista as operadoras que ainda nao
     * receberam nenhum resgate, e a coluna ativo expoe a RN06: uma
     * operadora desativada deixa de ser ofertada, mas seus cupons
     * historicos continuam integros.
     */
    public List<DesempenhoOperadoraDTO> desempenho() {
        String sql = "SELECT o.nome AS operadora, o.modal, o.cidade, o.ativo, "
            + "COUNT(v.id_voucher) AS cupons_emitidos, "
            + "SUM(CASE WHEN v.status = 'UTILIZADO' THEN 1 ELSE 0 END) AS utilizados, "
            + "SUM(CASE WHEN v.status = 'EXPIRADO' THEN 1 ELSE 0 END) AS expirados, "
            + "ROUND(100 * SUM(CASE WHEN v.status = 'UTILIZADO' THEN 1 ELSE 0 END) "
            + "/ NULLIF(COUNT(v.id_voucher), 0), 1) AS taxa_uso_pct "
            + "FROM T_OPERADORA_TRANSPORTE o "
            + "LEFT JOIN T_VOUCHER v ON v.id_operadora = o.id_operadora "
            + "GROUP BY o.id_operadora, o.nome, o.modal, o.cidade, o.ativo "
            + "ORDER BY cupons_emitidos DESC, o.nome";

        List<DesempenhoOperadoraDTO> relatorio = new ArrayList<>();
        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                relatorio.add(new DesempenhoOperadoraDTO(
                    rs.getString("operadora"),
                    rs.getString("modal"),
                    rs.getString("cidade"),
                    SequenceHelper.paraBoolean(rs.getString("ativo")),
                    rs.getInt("cupons_emitidos"),
                    rs.getInt("utilizados"),
                    rs.getInt("expirados"),
                    rs.getDouble("taxa_uso_pct")));
            }
            return relatorio;
        } catch (SQLException e) {
            throw new DAOException("Erro ao montar o relatorio de desempenho", e);
        }
    }

}
