package br.com.fiap.mobilidadeverde.dao;

import org.springframework.stereotype.Repository;
import br.com.fiap.mobilidadeverde.conexao.ConexaoOracle;
import br.com.fiap.mobilidadeverde.dto.RankingImpactoDTO;
import br.com.fiap.mobilidadeverde.model.Viagem;
import br.com.fiap.mobilidadeverde.model.Voucher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Acesso a dados de T_VIAGEM.
 *
 * A viagem registra o uso efetivo de um cupom e o CO2 evitado.
 * UK_VIAGEM_VOUCHER garante a cardinalidade 1:0..1 com o voucher:
 * um cupom gera no maximo uma viagem, o que e parte da RN04 (uso
 * unico).
 *
 * O calculo do impacto acumulado NAO percorre a lista em Java: usa
 * SUM e COUNT no proprio banco. Com centenas de milhares de
 * usuarios, trazer todas as viagens para a JVM apenas para somar
 * uma coluna seria desperdicio de memoria e de rede.
 */
@Repository
public class ViagemDAO {

    // ------------------------------------------------------------
    // CREATE
    // ------------------------------------------------------------

    public Viagem inserir(Viagem viagem) {
        try (Connection conexao = ConexaoOracle.abrir()) {
            return inserir(conexao, viagem);
        } catch (SQLException e) {
            throw new DAOException("Erro ao inserir viagem", e);
        }
    }

    public Viagem inserir(Connection conexao, Viagem viagem) throws SQLException {
        String sql = "INSERT INTO T_VIAGEM "
            + "(id_viagem, id_voucher, km_estimado, co2_evitado_g, registrado_em) "
            + "VALUES (?, ?, ?, ?, ?)";

        Long id = SequenceHelper.proximoValor(conexao, "SEQ_VIAGEM");
        viagem.setRegistradoEm(SequenceHelper.agora());

        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, viagem.getVoucher().getId());
            ps.setDouble(3, viagem.getKmEstimado());
            ps.setInt(4, viagem.getCo2EvitadoG());
            ps.setTimestamp(5, SequenceHelper.paraTimestamp(viagem.getRegistradoEm()));
            ps.executeUpdate();
        }

        viagem.setId(id);
        return viagem;
    }

    // ------------------------------------------------------------
    // READ
    // ------------------------------------------------------------

    /**
     * Lista as viagens de um usuario. O voucher associado vem com
     * apenas id e codigo preenchidos: e o suficiente para exibir o
     * historico, e evita repetir o JOIN completo de tres tabelas
     * que o VoucherDAO ja oferece quando o detalhe e necessario.
     */
    public List<Viagem> listarPorUsuario(Long idUsuario) {
        String sql = "SELECT vi.id_viagem, vi.km_estimado, vi.co2_evitado_g, "
            + "vi.registrado_em, v.id_voucher, v.codigo "
            + "FROM T_VIAGEM vi "
            + "INNER JOIN T_VOUCHER v ON v.id_voucher = vi.id_voucher "
            + "WHERE v.id_usuario = ? ORDER BY vi.registrado_em DESC";

        List<Viagem> viagens = new ArrayList<>();
        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setLong(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Voucher voucher = new Voucher();
                    voucher.setId(rs.getLong("id_voucher"));
                    voucher.setCodigo(rs.getString("codigo"));

                    Viagem viagem = new Viagem();
                    viagem.setId(rs.getLong("id_viagem"));
                    viagem.setVoucher(voucher);
                    viagem.setKmEstimado(rs.getDouble("km_estimado"));
                    viagem.setCo2EvitadoG(rs.getInt("co2_evitado_g"));
                    viagem.setRegistradoEm(
                        SequenceHelper.paraLocalDateTime(rs.getTimestamp("registrado_em")));
                    viagens.add(viagem);
                }
            }
            return viagens;
        } catch (SQLException e) {
            throw new DAOException("Erro ao listar viagens do usuario " + idUsuario, e);
        }
    }

    /**
     * Agrega no banco o total de viagens, de quilometros e de CO2
     * evitado de um usuario.
     *
     * @return vetor de tres posicoes: [0] quantidade de viagens,
     *         [1] soma de km (com duas casas), [2] soma de CO2 em gramas
     */
    public double[] totalizarPorUsuario(Long idUsuario) {
        String sql = "SELECT COUNT(*) AS qtd, "
            + "NVL(SUM(vi.km_estimado), 0) AS km, "
            + "NVL(SUM(vi.co2_evitado_g), 0) AS co2 "
            + "FROM T_VIAGEM vi "
            + "INNER JOIN T_VOUCHER v ON v.id_voucher = vi.id_voucher "
            + "WHERE v.id_usuario = ?";

        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setLong(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new double[] {
                        rs.getInt("qtd"), rs.getDouble("km"), rs.getDouble("co2")
                    };
                }
                return new double[] { 0, 0, 0 };
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao totalizar viagens do usuario " + idUsuario, e);
        }
    }

    // ------------------------------------------------------------
    // DELETE (restrito a limpeza de cenario de demonstracao)
    // ------------------------------------------------------------

    public int deletarPorUsuario(Connection conexao, Long idUsuario) throws SQLException {
        String sql = "DELETE FROM T_VIAGEM WHERE id_voucher IN "
            + "(SELECT id_voucher FROM T_VOUCHER WHERE id_usuario = ?)";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, idUsuario);
            return ps.executeUpdate();
        }
    }

    /**
     * Ranking de impacto ambiental por usuario.
     *
     * Reproduz a consulta C04 da disciplina de Building Relational
     * Database: soma o CO2 que cada usuario deixou de emitir e traduz o
     * resultado em arvores-ano, usando a absorcao de referencia de
     * 21,77 kg de CO2 por arvore ao ano. A agregacao inteira e feita
     * pelo banco.
     */
    public List<RankingImpactoDTO> rankingImpacto() {
        String sql = "SELECT u.nome, u.cidade, "
            + "COUNT(vg.id_viagem) AS viagens, "
            + "SUM(vg.km_estimado) AS km_percorridos, "
            + "ROUND(SUM(vg.co2_evitado_g) / 1000, 3) AS co2_evitado_kg, "
            + "ROUND(SUM(vg.co2_evitado_g) / 1000 / 21.77, 4) AS arvores_ano_equiv "
            + "FROM T_USUARIO u "
            + "INNER JOIN T_VOUCHER v ON v.id_usuario = u.id_usuario "
            + "INNER JOIN T_VIAGEM vg ON vg.id_voucher = v.id_voucher "
            + "GROUP BY u.id_usuario, u.nome, u.cidade "
            + "ORDER BY co2_evitado_kg DESC";

        List<RankingImpactoDTO> ranking = new ArrayList<>();
        try (Connection conexao = ConexaoOracle.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                ranking.add(new RankingImpactoDTO(
                    rs.getString("nome"),
                    rs.getString("cidade"),
                    rs.getInt("viagens"),
                    rs.getDouble("km_percorridos"),
                    rs.getDouble("co2_evitado_kg"),
                    rs.getDouble("arvores_ano_equiv")));
            }
            return ranking;
        } catch (SQLException e) {
            throw new DAOException("Erro ao montar o ranking de impacto ambiental", e);
        }
    }

}
