package br.com.fiap.mobilidadeverde.dao;

import org.springframework.stereotype.Repository;
import br.com.fiap.mobilidadeverde.conexao.ConexaoOracle;
import br.com.fiap.mobilidadeverde.model.FaixaCupom;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Acesso a dados de T_FAIXA_CUPOM.
 *
 * As faixas sao o catalogo de valores fixos de cupom
 * (R$ 2, R$ 5, R$ 10 e R$ 20). Nao existe valor livre: essa e a
 * RN03, e manter os valores como DADO, e nao como constante em
 * codigo, e o que permite a Prospera mudar a cotacao sem novo
 * deploy.
 */
@Repository
public class FaixaCupomDAO {

    private static final String COLUNAS =
        "id_faixa, valor_centavos, pontos_necessarios, ativo";

    // ------------------------------------------------------------
    // CREATE
    // ------------------------------------------------------------

    public FaixaCupom inserir(FaixaCupom faixa) {
        try (Connection conexao = ConexaoOracle.abrir()) {
            return inserir(conexao, faixa);
        } catch (SQLException e) {
            throw new DAOException("Erro ao inserir faixa de cupom", e);
        }
    }

    public FaixaCupom inserir(Connection conexao, FaixaCupom faixa) throws SQLException {
        String sql = "INSERT INTO T_FAIXA_CUPOM (" + COLUNAS + ") VALUES (?, ?, ?, ?)";

        Long id = SequenceHelper.proximoValor(conexao, "SEQ_FAIXA");

        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setInt(2, faixa.getValorCentavos());
            ps.setInt(3, faixa.getPontosNecessarios());
            ps.setString(4, SequenceHelper.paraFlag(faixa.isAtivo()));
            ps.executeUpdate();
        }

        faixa.setId(id);
        return faixa;
    }

    // ------------------------------------------------------------
    // READ
    // ------------------------------------------------------------

    public FaixaCupom buscarPorId(Long idFaixa) {
        String sql = "SELECT " + COLUNAS + " FROM T_FAIXA_CUPOM WHERE id_faixa = ?";

        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setLong(1, idFaixa);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? montar(rs) : null;
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao buscar faixa " + idFaixa, e);
        }
    }

    /**
     * Busca pela faixa de um valor especifico em centavos.
     * Usado na carga idempotente do catalogo: evita duplicar faixas
     * a cada execucao da classe executora ou da classe de teste.
     */
    public FaixaCupom buscarPorValor(int valorCentavos) {
        String sql = "SELECT " + COLUNAS + " FROM T_FAIXA_CUPOM WHERE valor_centavos = ?";

        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setInt(1, valorCentavos);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? montar(rs) : null;
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao buscar faixa por valor " + valorCentavos, e);
        }
    }

    public List<FaixaCupom> listarAtivas() {
        String sql = "SELECT " + COLUNAS
            + " FROM T_FAIXA_CUPOM WHERE ativo = 'S' ORDER BY valor_centavos";

        List<FaixaCupom> faixas = new ArrayList<>();
        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                faixas.add(montar(rs));
            }
            return faixas;
        } catch (SQLException e) {
            throw new DAOException("Erro ao listar faixas ativas", e);
        }
    }

    // ------------------------------------------------------------
    // UPDATE
    // ------------------------------------------------------------

    public boolean atualizar(FaixaCupom faixa) {
        String sql = "UPDATE T_FAIXA_CUPOM SET valor_centavos = ?, pontos_necessarios = ?, "
            + "ativo = ? WHERE id_faixa = ?";

        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setInt(1, faixa.getValorCentavos());
            ps.setInt(2, faixa.getPontosNecessarios());
            ps.setString(3, SequenceHelper.paraFlag(faixa.isAtivo()));
            ps.setLong(4, faixa.getId());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DAOException("Erro ao atualizar faixa " + faixa.getId(), e);
        }
    }

    // ------------------------------------------------------------
    // MAPEAMENTO
    // ------------------------------------------------------------

    private FaixaCupom montar(ResultSet rs) throws SQLException {
        FaixaCupom faixa = new FaixaCupom();
        faixa.setId(rs.getLong("id_faixa"));
        faixa.setValorCentavos(rs.getInt("valor_centavos"));
        faixa.setPontosNecessarios(rs.getInt("pontos_necessarios"));
        faixa.setAtivo(SequenceHelper.paraBoolean(rs.getString("ativo")));
        return faixa;
    }

    /** Todas as faixas, inclusive as cotacoes historicas desativadas. */
    public List<FaixaCupom> listarTodas() {
        String sql = "SELECT " + COLUNAS + " FROM T_FAIXA_CUPOM ORDER BY valor_centavos";

        List<FaixaCupom> faixas = new ArrayList<>();
        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                faixas.add(montar(rs));
            }
            return faixas;
        } catch (SQLException e) {
            throw new DAOException("Erro ao listar todas as faixas", e);
        }
    }

}
