package br.com.fiap.mobilidadeverde.dao;

/**
 * Excecao propria da camada de acesso a dados.
 *
 * Existe para que as camadas superiores (service, app, teste) nao
 * precisem importar java.sql.SQLException. O erro tecnico original
 * fica preservado como causa, permitindo diagnostico sem vazar
 * detalhe de JDBC para o dominio.
 */
public class DAOException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DAOException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

    public DAOException(String mensagem) {
        super(mensagem);
    }
}
