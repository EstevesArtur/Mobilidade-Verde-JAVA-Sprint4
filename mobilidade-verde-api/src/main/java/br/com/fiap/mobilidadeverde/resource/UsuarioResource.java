package br.com.fiap.mobilidadeverde.resource;

import br.com.fiap.mobilidadeverde.bo.ImpactoBO;
import br.com.fiap.mobilidadeverde.bo.OperadoraBO;
import br.com.fiap.mobilidadeverde.bo.UsuarioBO;
import br.com.fiap.mobilidadeverde.dao.TransacaoDAO;
import br.com.fiap.mobilidadeverde.dao.ViagemDAO;
import br.com.fiap.mobilidadeverde.dao.VoucherDAO;
import br.com.fiap.mobilidadeverde.dto.FavoritaRequest;
import br.com.fiap.mobilidadeverde.dto.ImpactoDTO;
import br.com.fiap.mobilidadeverde.dto.RankingImpactoDTO;
import br.com.fiap.mobilidadeverde.dto.UsuarioRequest;
import br.com.fiap.mobilidadeverde.dto.UsuarioResponse;
import br.com.fiap.mobilidadeverde.model.Operadora;
import br.com.fiap.mobilidadeverde.model.TransacaoPontos;
import br.com.fiap.mobilidadeverde.model.Usuario;
import br.com.fiap.mobilidadeverde.model.UsuarioOperadoraFavorita;
import br.com.fiap.mobilidadeverde.model.Voucher;
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

import java.util.ArrayList;
import java.util.List;

/**
 * ================================================================
 * RECURSO: USUARIOS
 *
 * Cadastro do cliente e tudo que pende dele. Entidade com CRUD
 * completo.
 *
 * GET    /api/usuarios                      lista os usuarios
 * GET    /api/usuarios/ranking-impacto      ranking de CO2 evitado
 * GET    /api/usuarios/{id}                 detalhe com saldo
 * POST   /api/usuarios                      cadastra
 * PUT    /api/usuarios/{id}                 atualiza
 * DELETE /api/usuarios/{id}                 remove com dependencias
 * GET    /api/usuarios/{id}/vouchers        cupons do usuario
 * GET    /api/usuarios/{id}/transacoes      extrato de pontos
 * GET    /api/usuarios/{id}/impacto         impacto ambiental
 * GET    /api/usuarios/{id}/favoritas       operadoras favoritas
 * POST   /api/usuarios/{id}/favoritas       favorita uma operadora
 * DELETE /api/usuarios/{id}/favoritas/{idF} desfavorita
 * ================================================================
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioResource {

    private final UsuarioBO usuarioBO;
    private final OperadoraBO operadoraBO;
    private final ImpactoBO impactoBO;
    private final VoucherDAO voucherDAO;
    private final TransacaoDAO transacaoDAO;
    private final ViagemDAO viagemDAO;

    public UsuarioResource(UsuarioBO usuarioBO, OperadoraBO operadoraBO,
                           ImpactoBO impactoBO, VoucherDAO voucherDAO,
                           TransacaoDAO transacaoDAO, ViagemDAO viagemDAO) {
        this.usuarioBO = usuarioBO;
        this.operadoraBO = operadoraBO;
        this.impactoBO = impactoBO;
        this.voucherDAO = voucherDAO;
        this.transacaoDAO = transacaoDAO;
        this.viagemDAO = viagemDAO;
    }

    // ------------------------------------------------------------
    // CRUD
    // ------------------------------------------------------------

    /**
     * @return 200 com a lista de usuarios, sem dados sensiveis
     */
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        List<UsuarioResponse> resposta = new ArrayList<>();
        for (Usuario usuario : usuarioBO.listar()) {
            resposta.add(new UsuarioResponse(usuario, usuarioBO.consultarSaldo(usuario)));
        }
        return ResponseEntity.ok(resposta);
    }

    /**
     * Detalhe do usuario com o saldo de Pontos ECOA. E este endpoint
     * que alimenta o topo da tela de carteira.
     *
     * @return 200 com o usuario, ou 404 se o id nao existir
     */
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long id) {
        Usuario usuario = usuarioBO.buscarPorId(id);
        return ResponseEntity.ok(
            new UsuarioResponse(usuario, usuarioBO.consultarSaldo(usuario)));
    }

    /**
     * @return 201 com o usuario criado, ou 400 se e-mail ou CPF forem invalidos
     */
    @PostMapping
    public ResponseEntity<UsuarioResponse> cadastrar(@RequestBody UsuarioRequest requisicao) {
        Usuario usuario = usuarioBO.cadastrar(requisicao);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new UsuarioResponse(usuario, usuarioBO.consultarSaldo(usuario)));
    }

    /**
     * @return 200 com o usuario atualizado, 400 se invalido, 404 se nao existir
     */
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> atualizar(@PathVariable Long id,
                                                     @RequestBody UsuarioRequest requisicao) {
        Usuario usuario = usuarioBO.atualizar(id, requisicao);
        return ResponseEntity.ok(
            new UsuarioResponse(usuario, usuarioBO.consultarSaldo(usuario)));
    }

    /**
     * Remove o usuario e todas as suas dependencias, na ordem inversa
     * das chaves estrangeiras.
     *
     * @return 204 sem corpo, ou 404 se o id nao existir
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        usuarioBO.remover(id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------
    // SUBRECURSOS
    // ------------------------------------------------------------

    /**
     * Cupons do usuario, do mais recente para o mais antigo.
     *
     * @return 200 com a lista, ou 404 se o usuario nao existir
     */
    @GetMapping("/{id}/vouchers")
    public ResponseEntity<List<Voucher>> vouchers(@PathVariable Long id) {
        usuarioBO.buscarPorId(id);
        return ResponseEntity.ok(voucherDAO.listarPorUsuario(id));
    }

    /**
     * Extrato de Pontos ECOA: todo credito e todo debito, auditados.
     *
     * @return 200 com o extrato, ou 404 se o usuario nao existir
     */
    @GetMapping("/{id}/transacoes")
    public ResponseEntity<List<TransacaoPontos>> transacoes(@PathVariable Long id) {
        Usuario usuario = usuarioBO.buscarPorId(id);
        return ResponseEntity.ok(transacaoDAO.listarPorUsuario(usuario));
    }

    /**
     * Impacto ambiental acumulado, com as equivalencias em arvores e
     * em quilometros de carro evitados.
     *
     * @return 200 com o impacto, ou 404 se o usuario nao existir
     */
    @GetMapping("/{id}/impacto")
    public ResponseEntity<ImpactoDTO> impacto(@PathVariable Long id) {
        Usuario usuario = usuarioBO.buscarPorId(id);
        return ResponseEntity.ok(impactoBO.calcularImpactoAcumulado(usuario));
    }

    /**
     * Ranking de impacto ambiental entre todos os usuarios.
     *
     * @return 200 com o ranking
     */
    @GetMapping("/ranking-impacto")
    public ResponseEntity<List<RankingImpactoDTO>> rankingImpacto() {
        return ResponseEntity.ok(viagemDAO.rankingImpacto());
    }

    // ------------------------------------------------------------
    // RELACIONAMENTO N:N - OPERADORAS FAVORITAS
    // ------------------------------------------------------------

    /**
     * Operadoras favoritas do usuario, na ordem de preferencia que ele
     * definiu. E esta ordem que posiciona os cards na tela de resgate.
     *
     * @return 200 com as favoritas, ou 404 se o usuario nao existir
     */
    @GetMapping("/{id}/favoritas")
    public ResponseEntity<List<UsuarioOperadoraFavorita>> favoritas(@PathVariable Long id) {
        Usuario usuario = usuarioBO.buscarPorId(id);
        return ResponseEntity.ok(operadoraBO.listarFavoritas(usuario));
    }

    /**
     * Favorita uma operadora, na proxima posicao livre da lista.
     *
     * @return 201 com a favorita criada, 404 se usuario ou operadora
     *         nao existirem, 500 se a operadora ja estiver favoritada
     *         (UK_FAVORITA_USR_OPE)
     */
    @PostMapping("/{id}/favoritas")
    public ResponseEntity<UsuarioOperadoraFavorita> favoritar(
            @PathVariable Long id, @RequestBody FavoritaRequest requisicao) {

        Usuario usuario = usuarioBO.buscarPorId(id);
        Operadora operadora = operadoraBO.buscarPorId(requisicao.getIdOperadora());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(operadoraBO.favoritar(usuario, operadora));
    }

    /**
     * Desfavorita uma operadora.
     *
     * @return 204 sem corpo, ou 404 se o usuario nao existir
     */
    @DeleteMapping("/{id}/favoritas/{idFavorita}")
    public ResponseEntity<Void> desfavoritar(@PathVariable Long id,
                                             @PathVariable Long idFavorita) {
        usuarioBO.buscarPorId(id);
        operadoraBO.desfavoritar(idFavorita);
        return ResponseEntity.noContent().build();
    }
}
