package br.com.fiap.mobilidadeverde.resource;

import br.com.fiap.mobilidadeverde.dao.FaixaCupomDAO;
import br.com.fiap.mobilidadeverde.exception.RecursoNaoEncontradoException;
import br.com.fiap.mobilidadeverde.model.FaixaCupom;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ================================================================
 * RECURSO: FAIXAS DE CUPOM
 *
 * Catalogo de valores fixos de cupom. Nao existe valor livre: essa e
 * a RN03, e manter os valores como dado, e nao como constante em
 * codigo, e o que permite ajustar a cotacao sem novo deploy.
 *
 * GET /api/faixas         lista as faixas ativas
 * GET /api/faixas/todas   inclui as cotacoes historicas desativadas
 * GET /api/faixas/{id}    detalhe de uma faixa
 * ================================================================
 */
@RestController
@RequestMapping("/api/faixas")
public class FaixaResource {

    private final FaixaCupomDAO faixaDAO;

    public FaixaResource(FaixaCupomDAO faixaDAO) {
        this.faixaDAO = faixaDAO;
    }

    /**
     * Faixas que o usuario pode escolher na tela de resgate.
     *
     * @return 200 com as faixas ativas
     */
    @GetMapping
    public ResponseEntity<List<FaixaCupom>> listar() {
        return ResponseEntity.ok(faixaDAO.listarAtivas());
    }

    /**
     * Inclui as cotacoes historicas, que ficam desativadas mas
     * permanecem no banco porque ha cupons antigos apontando para elas.
     *
     * @return 200 com todas as faixas
     */
    @GetMapping("/todas")
    public ResponseEntity<List<FaixaCupom>> listarTodas() {
        return ResponseEntity.ok(faixaDAO.listarTodas());
    }

    /**
     * @return 200 com a faixa, ou 404 se o id nao existir
     */
    @GetMapping("/{id}")
    public ResponseEntity<FaixaCupom> buscarPorId(@PathVariable Long id) {
        FaixaCupom faixa = faixaDAO.buscarPorId(id);
        if (faixa == null) {
            throw new RecursoNaoEncontradoException("Faixa nao encontrada. Id: " + id);
        }
        return ResponseEntity.ok(faixa);
    }
}
