package br.com.fiap.mobilidadeverde.conexao;

import br.com.fiap.mobilidadeverde.dao.DAOException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Connection Factory da aplicacao.
 *
 * Centraliza a abertura de conexoes JDBC com o Oracle. Nenhuma outra
 * classe do projeto chama DriverManager diretamente: toda a camada DAO
 * passa por aqui.
 *
 * Esta classe reusa a DAOException do pacote dao porque as duas fazem
 * parte da mesma camada de persistencia: uma falha de conexao e, para
 * quem chama, o mesmo tipo de problema que uma falha de consulta, e
 * deve virar o mesmo status HTTP.
 *
 * MUDANCA EM RELACAO A SPRINT 3. Naquela entrega o usuario e a senha
 * ficavam fixos no codigo, porque a entrega era um .ZIP no Portal do
 * Aluno. Nesta sprint a entrega e um repositorio GitHub publico e a
 * aplicacao roda em servidor na nuvem, entao as credenciais passam a
 * ser lidas de variaveis de ambiente. Isso evita expor a senha no
 * repositorio e e o que permite configurar o acesso no servidor de
 * deploy sem recompilar a aplicacao.
 *
 * Variaveis esperadas no ambiente:
 *   ORACLE_URL      jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL
 *   ORACLE_USER     RM do aluno, em maiusculo
 *   ORACLE_PASSWORD senha do aluno
 *
 * Quando as variaveis nao estao definidas, valem os valores padrao
 * declarados abaixo, de modo que o projeto roda localmente sem nenhuma
 * configuracao adicional.
 */
public class ConexaoOracle {

    private static final String URL_PADRAO =
        "jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL";

    private static final String USUARIO_PADRAO = "RM569450";

    private static final String SENHA_PADRAO = "21012006";

    /** Classe utilitaria: nao deve ser instanciada. */
    private ConexaoOracle() {
    }

    /**
     * Le a variavel de ambiente informada e devolve o valor padrao
     * quando ela nao estiver definida ou estiver vazia.
     */
    private static String ler(String variavel, String valorPadrao) {
        String valor = System.getenv(variavel);
        if (valor == null || valor.isBlank()) {
            return valorPadrao;
        }
        return valor.trim();
    }

    public static String getUrl() {
        return ler("ORACLE_URL", URL_PADRAO);
    }

    public static String getUsuario() {
        return ler("ORACLE_USER", USUARIO_PADRAO);
    }

    private static String getSenha() {
        return ler("ORACLE_PASSWORD", SENHA_PADRAO);
    }

    /**
     * Abre uma nova conexao com o banco. Quem chama e responsavel por
     * fechar, de preferencia com try-with-resources.
     *
     * Uma falha aqui e de infraestrutura, nao de regra de negocio: por isso
     * sai como DAOException, que o TratadorDeErros traduz em HTTP 500. A
     * mensagem devolvida ao cliente nao repete a URL nem o usuario do banco;
     * esses dados ficam no endpoint de diagnostico /api/status.
     *
     * @return conexao aberta e pronta para uso
     * @throws DAOException se o driver nao estiver no classpath ou se o
     *                      banco recusar a conexao
     */
    public static Connection abrir() {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            return DriverManager.getConnection(getUrl(), getUsuario(), getSenha());

        } catch (ClassNotFoundException e) {
            throw new DAOException(
                "Driver JDBC do Oracle nao encontrado no classpath.", e);

        } catch (SQLException e) {
            throw new DAOException(
                "Falha ao conectar no banco de dados. Consulte /api/status. "
                + "Detalhe: " + e.getMessage(), e);
        }
    }

    /**
     * Mesma conexao de abrir(), mas com a mensagem completa de diagnostico
     * (URL e usuario). Usada apenas pelo endpoint /api/status.
     */
    private static Connection abrirParaDiagnostico() {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            return DriverManager.getConnection(getUrl(), getUsuario(), getSenha());

        } catch (ClassNotFoundException e) {
            throw new DAOException(
                "Driver JDBC do Oracle nao encontrado no classpath.", e);

        } catch (SQLException e) {
            throw new DAOException(
                "Falha ao conectar em " + getUrl() + " com o usuario "
                + getUsuario() + ". Verifique rede, usuario e senha. "
                + "Detalhe: " + e.getMessage(), e);
        }
    }

    /**
     * Fecha a conexao sem propagar excecao. Usado em blocos finally,
     * onde uma falha ao fechar nao deve mascarar o erro original.
     */
    public static void fechar(Connection conexao) {
        if (conexao == null) {
            return;
        }
        try {
            conexao.close();
        } catch (SQLException e) {
            System.err.println("Aviso: falha ao fechar a conexao - " + e.getMessage());
        }
    }

    /**
     * Desfaz a transacao corrente sem propagar excecao. Usado nos blocos
     * catch dos servicos que controlam transacao manual.
     */
    public static void desfazer(Connection conexao) {
        if (conexao == null) {
            return;
        }
        try {
            conexao.rollback();
        } catch (SQLException e) {
            System.err.println("Aviso: falha no rollback - " + e.getMessage());
        }
    }

    /**
     * Testa a conexao e devolve uma linha descrevendo o ambiente.
     * Consumido pelo endpoint de verificacao de saude da API.
     */
    public static String descreverAmbiente() {
        try (Connection conexao = abrirParaDiagnostico()) {
            return conexao.getMetaData().getDatabaseProductVersion()
                + " | usuario: " + conexao.getMetaData().getUserName();
        } catch (SQLException e) {
            throw new DAOException("Falha ao ler metadados da conexao", e);
        }
    }
}
