package br.com.fiap.mobilidadeverde.exception;

/**
 * Lancada quando o recurso pedido nao existe no banco.
 * A camada Resource traduz esta excecao em HTTP 404.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
