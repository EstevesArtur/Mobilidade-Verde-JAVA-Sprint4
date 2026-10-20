package br.com.fiap.mobilidadeverde.dao;

import org.springframework.stereotype.Repository;
import br.com.fiap.mobilidadeverde.conexao.ConexaoOracle;
import br.com.fiap.mobilidadeverde.model.OrigemTransacao;
import br.com.fiap.mobilidadeverde.model.TipoTransacao;
import br.com.fiap.mobilidadeverde.model.TransacaoPontos;
import br.com.fiap.mobilidadeverde.model.Usuario;
import br.com.fiap.mobilidadeverde.model.Voucher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Acesso a dados de T_TRANSACAO_PONTOS, o log de auditoria.
 *
 * ESTE DAO E DELIBERADAMENTE APPEND-ONLY. Nao existe metodo de
 * update, e o unico delete e restrito a limpeza do cenario de
 * demonstracao. Isso materializa a RN05: toda movimentacao de
 * pontos e auditada e imutavel.
 *
 * E por essa razao que o CRUD completo exigido pela rubrica foi
 * aplicado a T_OPERADORA_TRANSPORTE, T_USUARIO e
 * T_USUARIO_OPERADORA_FAVORITA, e nao a esta tabela: expor UPDATE e
 * DELETE de registro de auditoria contradiria a regra de negocio
 * que o proprio grupo defendeu na modelagem.
 *
 * A coluna id_voucher e nulavel de proposito: creditos vindos da
 * plataforma SoulUp e ajustes manuais existem sem cupom associado.
 */
@Repository
public class TransacaoDAO {

    // ------------------------------------------------------------
    // CREATE (unica operacao de escrita do fluxo de negocio)
    // ------------------------------------------------------------

    public TransacaoPontos inserir(TransacaoPontos transacao) {
        try (Connection conexao = ConexaoOracle.abrir()) {
            return inserir(conexao, transacao);
        } catch (SQLException e) {
            throw new DAOException("Erro ao registrar transacao de pontos", e);
        }
    }

    public TransacaoPontos inserir(Connection conexao, TransacaoPontos transacao)
            throws SQLException {
        String sql = "INSERT INTO T_TRANSACAO_PONTOS "
            + "(id_transacao, id_usuario, tipo, pontos, origem, id_voucher, "
            + "descricao, ip_origem, data_transacao) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Long id = SequenceHelper.proximoValor(conexao, "SEQ_TRANSACAO");
        if (transacao.getDataTransacao() == null) {
            transacao.setDataTransacao(SequenceHelper.agora());
        }

        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, transacao.getUsuario().getId());
            ps.setString(3, transacao.getTipo().name());
            ps.setInt(4, transacao.getPontos());
            ps.setString(5, transacao.getOrigem().name());

            if (transacao.getVoucher() == null || transacao.getVoucher().getId() == null) {
                ps.setNull(6, Types.NUMERIC);
            } else {
                ps.setLong(6, transacao.getVoucher().getId());
            }

            ps.setString(7, transacao.getDescricao());
            ps.setString(8, transacao.getIpOrigem());
            ps.setTimestamp(9, SequenceHelper.paraTimestamp(transacao.getDataTransacao()));
            ps.executeUpdate();
        }

        transacao.setId(id);
        return transacao;
    }

    // ------------------------------------------------------------
    // READ
    // ------------------------------------------------------------

    /**
     * Extrato do usuario, do mais recente para o mais antigo.
     * A ordenacao por (id_usuario, data_transacao DESC) e coberta
     * pelo indice IDX_TRANS_USR_DATA.
     */
    public List<TransacaoPontos> listarPorUsuario(Usuario usuario) {
        String sql = "SELECT t.id_transacao, t.tipo, t.pontos, t.origem, t.id_voucher, "
            + "t.descricao, t.ip_origem, t.data_transacao, v.codigo "
            + "FROM T_TRANSACAO_PONTOS t "
            + "LEFT JOIN T_VOUCHER v ON v.id_voucher = t.id_voucher "
            + "WHERE t.id_usuario = ? ORDER BY t.data_transacao DESC, t.id_transacao DESC";

        List<TransacaoPontos> transacoes = new ArrayList<>();
        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setLong(1, usuario.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    transacoes.add(montar(rs, usuario));
                }
            }
            return transacoes;
        } catch (SQLException e) {
            throw new DAOException(
                "Erro ao listar transacoes do usuario " + usuario.getId(), e);
        }
    }

    /** Soma dos pontos movimentados por tipo (CREDITO ou DEBITO). */
    public int somarPontosPorTipo(Long idUsuario, TipoTransacao tipo) {
        String sql = "SELECT NVL(SUM(pontos), 0) FROM T_TRANSACAO_PONTOS "
            + "WHERE id_usuario = ? AND tipo = ?";

        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setLong(1, idUsuario);
            ps.setString(2, tipo.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao somar pontos do usuario " + idUsuario, e);
        }
    }

    // ------------------------------------------------------------
    // DELETE (restrito a limpeza de cenario de demonstracao)
    // ------------------------------------------------------------

    /**
     * Remove as transacoes de um usuario.
     *
     * NAO faz parte do fluxo de negocio e nao e chamado por nenhum
     * service de dominio. Existe unicamente para permitir que o
     * AdminService reinicie o cenario de demonstracao. Em producao,
     * a blindagem correta da imutabilidade e por GRANT restritivo
     * no schema, conforme registrado na documentacao de banco.
     */
    public int deletarPorUsuario(Connection conexao, Long idUsuario) throws SQLException {
        String sql = "DELETE FROM T_TRANSACAO_PONTOS WHERE id_usuario = ?";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, idUsuario);
            return ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------
    // MAPEAMENTO
    // ------------------------------------------------------------

    private TransacaoPontos montar(ResultSet rs, Usuario usuario) throws SQLException {
        Voucher voucher = null;
        long idVoucher = rs.getLong("id_voucher");
        if (!rs.wasNull()) {
            voucher = new Voucher();
            voucher.setId(idVoucher);
            voucher.setCodigo(rs.getString("codigo"));
        }

        TransacaoPontos transacao = new TransacaoPontos();
        transacao.setId(rs.getLong("id_transacao"));
        transacao.setUsuario(usuario);
        transacao.setTipo(TipoTransacao.valueOf(rs.getString("tipo")));
        transacao.setPontos(rs.getInt("pontos"));
        transacao.setOrigem(OrigemTransacao.valueOf(rs.getString("origem")));
        transacao.setVoucher(voucher);
        transacao.setDescricao(rs.getString("descricao"));
        transacao.setIpOrigem(rs.getString("ip_origem"));
        transacao.setDataTransacao(
            SequenceHelper.paraLocalDateTime(rs.getTimestamp("data_transacao")));
        return transacao;
    }
}
