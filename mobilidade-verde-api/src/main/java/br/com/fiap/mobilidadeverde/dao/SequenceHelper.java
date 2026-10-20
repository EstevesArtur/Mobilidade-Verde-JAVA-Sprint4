package br.com.fiap.mobilidadeverde.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Utilitarios compartilhados pela camada DAO.
 *
 * Concentra tres conversoes que se repetiriam em todos os DAOs:
 *
 * 1. SEQUENCES. O modelo usa o padrao documentado na
 *    disciplina de banco: as chaves primarias sao preenchidas
 *    explicitamente via NEXTVAL, sem trigger automatica. O id e
 *    obtido ANTES do INSERT, de modo que o objeto Java ja sai do
 *    metodo com a chave correta preenchida.
 *
 * 2. TIMESTAMP. Oracle armazena TIMESTAMP com seis casas decimais
 *    (microssegundos); LocalDateTime.now() em JDK moderno produz
 *    nove (nanossegundos). Gravar e reler truncaria o valor e
 *    quebraria qualquer comparacao de igualdade. Por isso todo
 *    instante gerado pela aplicacao e truncado no SEGUNDO antes de
 *    ser usado, garantindo que memoria e banco guardem exatamente
 *    o mesmo valor. Isso e o que mantem valido o HMAC do voucher,
 *    calculado sobre a data de geracao.
 *
 * 3. FLAG 'S'/'N'. O banco nao tem tipo booleano; usa CHAR(1) com
 *    CHECK IN ('S','N'). A traducao para boolean fica aqui.
 */
public final class SequenceHelper {

    private SequenceHelper() {
    }

    /**
     * Obtem o proximo valor de uma sequence do Oracle.
     *
     * @param conexao   conexao aberta
     * @param sequence  nome da sequence, por exemplo SEQ_VOUCHER
     * @return proximo valor da sequence
     */
    public static Long proximoValor(Connection conexao, String sequence) throws SQLException {
        String sql = "SELECT " + sequence + ".NEXTVAL FROM DUAL";
        try (PreparedStatement ps = conexao.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            throw new DAOException("Sequence " + sequence + " nao retornou valor");
        }
    }

    /**
     * Instante atual truncado no segundo.
     * Ver explicacao no cabecalho desta classe.
     */
    public static LocalDateTime agora() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    /** Converte Timestamp do JDBC em LocalDateTime, tolerando null. */
    public static LocalDateTime paraLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    /** Converte LocalDateTime em Timestamp do JDBC, tolerando null. */
    public static Timestamp paraTimestamp(LocalDateTime data) {
        return data == null ? null : Timestamp.valueOf(data);
    }

    /** boolean da aplicacao para CHAR(1) do banco. */
    public static String paraFlag(boolean valor) {
        return valor ? "S" : "N";
    }

    /** CHAR(1) do banco para boolean da aplicacao. */
    public static boolean paraBoolean(String flag) {
        return "S".equalsIgnoreCase(flag);
    }
}
