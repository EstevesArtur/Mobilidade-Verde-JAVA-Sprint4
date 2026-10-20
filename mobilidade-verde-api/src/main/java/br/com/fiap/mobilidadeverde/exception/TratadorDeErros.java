package br.com.fiap.mobilidadeverde.exception;

import br.com.fiap.mobilidadeverde.dao.DAOException;
import br.com.fiap.mobilidadeverde.dto.ErroResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * ================================================================
 * TRATAMENTO CENTRAL DE EXCECOES
 *
 * Captura os erros lancados por qualquer Resource e os traduz em
 * respostas HTTP com o status adequado e um corpo JSON padronizado.
 *
 * Sem esta classe, qualquer falha viraria um HTTP 500 generico com
 * stack trace, o front nao conseguiria distinguir "nao encontrado"
 * de "saldo insuficiente", e o usuario veria uma tela de erro sem
 * explicacao.
 *
 * Mapeamento adotado:
 *   404 Not Found   - o recurso pedido nao existe
 *   400 Bad Request - dado invalido enviado pelo cliente
 *   409 Conflict    - regra de negocio impede a operacao
 *                     (saldo insuficiente, limite antifraude,
 *                      cupom ja utilizado, cupom expirado)
 *   500 Internal    - falha de infraestrutura ou de banco
 * ================================================================
 */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException e) {
        return montar(HttpStatus.NOT_FOUND, "Recurso nao encontrado", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> requisicaoInvalida(IllegalArgumentException e) {
        return montar(HttpStatus.BAD_REQUEST, "Requisicao invalida", e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErroResponse> regraDeNegocio(IllegalStateException e) {
        return montar(HttpStatus.CONFLICT, "Operacao nao permitida", e.getMessage());
    }

    @ExceptionHandler(DAOException.class)
    public ResponseEntity<ErroResponse> falhaDeBanco(DAOException e) {
        return montar(HttpStatus.INTERNAL_SERVER_ERROR,
            "Falha no acesso ao banco de dados", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> falhaInesperada(Exception e) {
        return montar(HttpStatus.INTERNAL_SERVER_ERROR,
            "Erro interno", e.getMessage());
    }

    private ResponseEntity<ErroResponse> montar(HttpStatus status, String erro, String mensagem) {
        return ResponseEntity.status(status)
            .body(new ErroResponse(status.value(), erro, mensagem));
    }
}
