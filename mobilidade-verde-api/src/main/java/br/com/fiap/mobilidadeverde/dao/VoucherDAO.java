package br.com.fiap.mobilidadeverde.dao;

import org.springframework.stereotype.Repository;
import br.com.fiap.mobilidadeverde.conexao.ConexaoOracle;
import br.com.fiap.mobilidadeverde.model.FaixaCupom;
import br.com.fiap.mobilidadeverde.model.Modal;
import br.com.fiap.mobilidadeverde.model.Operadora;
import br.com.fiap.mobilidadeverde.model.StatusVoucher;
import br.com.fiap.mobilidadeverde.model.Usuario;
import br.com.fiap.mobilidadeverde.model.Voucher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Acesso a dados de T_VOUCHER, o nucleo do produto.
 *
 * Toda leitura de voucher faz JOIN com usuario, operadora e faixa e
 * devolve o grafo completo. O motivo e pratico: o CupomService
 * precisa de voucher.getOperadora().getFatorCo2KgKm() para calcular
 * o CO2 evitado e de voucher.getFaixa().getValorCentavos() para
 * derivar o numero de passagens. Devolver o voucher com essas
 * referencias nulas produziria NullPointerException dentro da
 * regra de negocio.
 *
 * O CRUD aqui e parcial por decisao de projeto: existe Create, Read
 * e Update de status, mas nao ha Delete no fluxo de negocio. Cupom
 * emitido e documento financeiro; o ciclo de vida termina em
 * UTILIZADO, EXPIRADO ou CANCELADO, nunca em remocao. O unico
 * metodo de remocao existente e restrito a limpeza do cenario de
 * demonstracao e nao e chamado por nenhum service.
 */
@Repository
public class VoucherDAO {

    // ------------------------------------------------------------
    // CREATE
    // ------------------------------------------------------------

    public Voucher inserir(Voucher voucher) {
        try (Connection conexao = ConexaoOracle.abrir()) {
            return inserir(conexao, voucher);
        } catch (SQLException e) {
            throw new DAOException("Erro ao inserir voucher", e);
        }
    }

    public Voucher inserir(Connection conexao, Voucher voucher) throws SQLException {
        String sql = "INSERT INTO T_VOUCHER "
            + "(id_voucher, id_usuario, id_operadora, id_faixa, codigo, hash_validacao, "
            + "status, gerado_em, expira_em, utilizado_em) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Long id = SequenceHelper.proximoValor(conexao, "SEQ_VOUCHER");

        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, voucher.getUsuario().getId());
            ps.setLong(3, voucher.getOperadora().getId());
            ps.setLong(4, voucher.getFaixa().getId());
            ps.setString(5, voucher.getCodigo());
            ps.setString(6, voucher.getHashValidacao());
            ps.setString(7, voucher.getStatus().name());
            ps.setTimestamp(8, SequenceHelper.paraTimestamp(voucher.getGeradoEm()));
            ps.setTimestamp(9, SequenceHelper.paraTimestamp(voucher.getExpiraEm()));
            ps.setTimestamp(10, SequenceHelper.paraTimestamp(voucher.getUtilizadoEm()));
            ps.executeUpdate();
        }

        voucher.setId(id);
        return voucher;
    }

    // ------------------------------------------------------------
    // READ
    // ------------------------------------------------------------

    public Voucher buscarPorCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        String sql = montarSelect() + " WHERE v.codigo = ?";

        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? montar(rs) : null;
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao buscar voucher pelo codigo " + codigo, e);
        }
    }

    public List<Voucher> listarPorUsuario(Long idUsuario) {
        String sql = montarSelect() + " WHERE v.id_usuario = ? ORDER BY v.gerado_em DESC";

        List<Voucher> vouchers = new ArrayList<>();
        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setLong(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    vouchers.add(montar(rs));
                }
            }
            return vouchers;
        } catch (SQLException e) {
            throw new DAOException("Erro ao listar vouchers do usuario " + idUsuario, e);
        }
    }

    /**
     * Conta cupons de um usuario em determinado status.
     *
     * Esta e a consulta que sustenta a RN07 (limite antifraude).
     * O predicado (id_usuario, status) e exatamente a definicao do
     * indice IDX_VOUCHER_USR_STAT, entao o Oracle resolve por
     * acesso ao indice, sem varrer a tabela.
     */
    public int contarPorUsuarioEStatus(Long idUsuario, StatusVoucher status) {
        try (Connection conexao = ConexaoOracle.abrir()) {
            return contarPorUsuarioEStatus(conexao, idUsuario, status);
        } catch (SQLException e) {
            throw new DAOException("Erro ao contar vouchers do usuario " + idUsuario, e);
        }
    }

    public int contarPorUsuarioEStatus(Connection conexao, Long idUsuario,
                                       StatusVoucher status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM T_VOUCHER WHERE id_usuario = ? AND status = ?";

        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, idUsuario);
            ps.setString(2, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // ------------------------------------------------------------
    // UPDATE
    // ------------------------------------------------------------

    public boolean atualizarStatus(Voucher voucher) {
        try (Connection conexao = ConexaoOracle.abrir()) {
            return atualizarStatus(conexao, voucher);
        } catch (SQLException e) {
            throw new DAOException("Erro ao atualizar status do voucher "
                + voucher.getCodigo(), e);
        }
    }

    /**
     * Persiste a transicao de estado do cupom (RN04).
     * Participa da transacao quando recebe a conexao de fora.
     */
    public boolean atualizarStatus(Connection conexao, Voucher voucher) throws SQLException {
        String sql = "UPDATE T_VOUCHER SET status = ?, utilizado_em = ? WHERE id_voucher = ?";

        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setString(1, voucher.getStatus().name());
            ps.setTimestamp(2, SequenceHelper.paraTimestamp(voucher.getUtilizadoEm()));
            ps.setLong(3, voucher.getId());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Marca como EXPIRADO todos os cupons vencidos que ainda estao
     * em GERADO. Operacao em lote, resolvida inteiramente pelo banco
     * numa unica instrucao, sem trazer linha nenhuma para a JVM.
     *
     * @return quantidade de cupons expirados nesta execucao
     */
    public int expirarVencidos() {
        String sql = "UPDATE T_VOUCHER SET status = 'EXPIRADO' "
            + "WHERE status = 'GERADO' AND expira_em < SYSTIMESTAMP";

        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            return ps.executeUpdate();

        } catch (SQLException e) {
            throw new DAOException("Erro ao expirar cupons vencidos", e);
        }
    }

    // ------------------------------------------------------------
    // DELETE (restrito a limpeza de cenario de demonstracao)
    // ------------------------------------------------------------

    /**
     * Remove os vouchers de um usuario.
     *
     * NAO faz parte do fluxo de negocio e nao e chamado por nenhum
     * service. Existe apenas para que o AdminService consiga
     * reiniciar o cenario de demonstracao, o que torna a classe
     * executora e a classe de teste reexecutaveis sem violar as
     * chaves unicas do esquema.
     */
    public int deletarPorUsuario(Connection conexao, Long idUsuario) throws SQLException {
        String sql = "DELETE FROM T_VOUCHER WHERE id_usuario = ?";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, idUsuario);
            return ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------
    // MAPEAMENTO
    // ------------------------------------------------------------

    private String montarSelect() {
        return "SELECT v.id_voucher, v.codigo, v.hash_validacao, v.status, "
            + "v.gerado_em, v.expira_em, v.utilizado_em, "
            + "u.id_usuario, u.nome AS usuario_nome, u.email, u.hash_senha, "
            + "u.cpf_hash, u.cidade AS usuario_cidade, u.criado_em, "
            + "o.id_operadora, o.nome AS operadora_nome, o.cidade AS operadora_cidade, "
            + "o.modal, o.valor_passagem_centavos, o.fator_co2_kg_km, "
            + "o.formato_codigo, o.ativo, "
            + "fx.id_faixa, fx.valor_centavos, fx.pontos_necessarios, "
            + "fx.ativo AS faixa_ativo "
            + "FROM T_VOUCHER v "
            + "INNER JOIN T_USUARIO u ON u.id_usuario = v.id_usuario "
            + "INNER JOIN T_OPERADORA_TRANSPORTE o ON o.id_operadora = v.id_operadora "
            + "INNER JOIN T_FAIXA_CUPOM fx ON fx.id_faixa = v.id_faixa";
    }

    private Voucher montar(ResultSet rs) throws SQLException {
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

        FaixaCupom faixa = new FaixaCupom();
        faixa.setId(rs.getLong("id_faixa"));
        faixa.setValorCentavos(rs.getInt("valor_centavos"));
        faixa.setPontosNecessarios(rs.getInt("pontos_necessarios"));
        faixa.setAtivo(SequenceHelper.paraBoolean(rs.getString("faixa_ativo")));

        Voucher voucher = new Voucher();
        voucher.setId(rs.getLong("id_voucher"));
        voucher.setUsuario(usuario);
        voucher.setOperadora(operadora);
        voucher.setFaixa(faixa);
        voucher.setCodigo(rs.getString("codigo"));
        voucher.setHashValidacao(rs.getString("hash_validacao"));
        voucher.setStatus(StatusVoucher.valueOf(rs.getString("status")));
        voucher.setGeradoEm(SequenceHelper.paraLocalDateTime(rs.getTimestamp("gerado_em")));
        voucher.setExpiraEm(SequenceHelper.paraLocalDateTime(rs.getTimestamp("expira_em")));
        voucher.setUtilizadoEm(
            SequenceHelper.paraLocalDateTime(rs.getTimestamp("utilizado_em")));
        return voucher;
    }
}
