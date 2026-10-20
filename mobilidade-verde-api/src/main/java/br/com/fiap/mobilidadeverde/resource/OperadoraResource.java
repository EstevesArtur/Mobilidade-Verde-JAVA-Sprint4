package br.com.fiap.mobilidadeverde.resource;

import br.com.fiap.mobilidadeverde.bo.OperadoraBO;
import br.com.fiap.mobilidadeverde.dao.OperadoraDAO;
import br.com.fiap.mobilidadeverde.dto.DesempenhoOperadoraDTO;
import br.com.fiap.mobilidadeverde.model.Operadora;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ================================================================
 * RECURSO: OPERADORAS DE TRANSPORTE
 *
 * Catalogo de operadoras parceiras. Entidade com CRUD completo.
 *
 * GET    /api/operadoras             lista as ativas (RN06)
 * GET    /api/operadoras/todas       lista todas, visao administrativa
 * GET    /api/operadoras/desempenho  relatorio de aproveitamento
 * GET    /api/operadoras/{id}        detalhe de uma operadora
 * POST   /api/operadoras             cadastra uma parceira
 * PUT    /api/operadoras/{id}        atualiza os dados
 * DELETE /api/operadoras/{id}        remove do catalogo
 * ================================================================
 */
@RestController
@RequestMapping("/api/operadoras")
public class OperadoraResource {

    private final OperadoraBO operadoraBO;
    private final OperadoraDAO operadoraDAO;

    public OperadoraResource(OperadoraBO operadoraBO, OperadoraDAO operadoraDAO) {
        this.operadoraBO = operadoraBO;
        this.operadoraDAO = operadoraDAO;
    }

    /**
     * Catalogo que o usuario final enxerga: apenas operadoras ativas.
     * E este endpoint que alimenta os cards da tela de resgate.
     *
     * @return 200 com a lista de operadoras ativas
     */
    @GetMapping
    public ResponseEntity<List<Operadora>> listar() {
        return ResponseEntity.ok(operadoraBO.listarOperadoras());
    }

    /**
     * Visao administrativa: inclui as operadoras desativadas, que
     * permanecem no banco para preservar os cupons ja emitidos.
     *
     * @return 200 com todas as operadoras
     */
    @GetMapping("/todas")
    public ResponseEntity<List<Operadora>> listarTodas() {
        return ResponseEntity.ok(operadoraBO.listarTodas());
    }

    /**
     * Relatorio de desempenho: cupons emitidos, utilizados, expirados e
     * taxa de aproveitamento por operadora.
     *
     * @return 200 com o relatorio
     */
    @GetMapping("/desempenho")
    public ResponseEntity<List<DesempenhoOperadoraDTO>> desempenho() {
        return ResponseEntity.ok(operadoraDAO.desempenho());
    }

    /**
     * @return 200 com a operadora, ou 404 se o id nao existir
     */
    @GetMapping("/{id}")
    public ResponseEntity<Operadora> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(operadoraBO.buscarPorId(id));
    }

    /**
     * @return 201 com a operadora criada, ou 400 se os dados forem invalidos
     */
    @PostMapping
    public ResponseEntity<Operadora> cadastrar(@RequestBody Operadora operadora) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(operadoraBO.cadastrar(operadora));
    }

    /**
     * @return 200 com a operadora atualizada, 400 se invalida, 404 se nao existir
     */
    @PutMapping("/{id}")
    public ResponseEntity<Operadora> atualizar(@PathVariable Long id,
                                               @RequestBody Operadora operadora) {
        return ResponseEntity.ok(operadoraBO.atualizar(id, operadora));
    }

    /**
     * Remove a operadora. Falha com 500 quando ela ja emitiu cupom: a
     * chave estrangeira protege o historico, e o caminho correto para
     * encerrar uma parceria e desativar, nao apagar.
     *
     * @return 204 sem corpo, ou 404 se o id nao existir
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        operadoraBO.remover(id);
        return ResponseEntity.noContent().build();
    }
}
