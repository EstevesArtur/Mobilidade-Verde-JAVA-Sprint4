package br.com.fiap.mobilidadeverde.resource;

import br.com.fiap.mobilidadeverde.bo.ConversorBO;
import br.com.fiap.mobilidadeverde.bo.CupomBO;
import br.com.fiap.mobilidadeverde.bo.OperadoraBO;
import br.com.fiap.mobilidadeverde.bo.UsuarioBO;
import br.com.fiap.mobilidadeverde.dao.FaixaCupomDAO;
import br.com.fiap.mobilidadeverde.dao.VoucherDAO;
import br.com.fiap.mobilidadeverde.dto.CupomDTO;
import br.com.fiap.mobilidadeverde.dto.NovoVoucherRequest;
import br.com.fiap.mobilidadeverde.dto.UtilizarCupomRequest;
import br.com.fiap.mobilidadeverde.exception.RecursoNaoEncontradoException;
import br.com.fiap.mobilidadeverde.model.FaixaCupom;
import br.com.fiap.mobilidadeverde.model.Operadora;
import br.com.fiap.mobilidadeverde.model.Usuario;
import br.com.fiap.mobilidadeverde.model.Viagem;
import br.com.fiap.mobilidadeverde.model.Voucher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ================================================================
 * RECURSO: VOUCHERS (CUPONS DE TRANSPORTE)
 *
 * Nucleo do produto. Aqui ficam as operacoes que movimentam saldo,
 * todas protegidas por transacao no banco.
 *
 * POST /api/vouchers                    resgata pontos em cupom
 * GET  /api/vouchers/{codigo}           consulta um cupom
 * POST /api/vouchers/{codigo}/utilizar  valida o cupom no embarque
 * POST /api/vouchers/{codigo}/estornar  cancela e devolve os pontos
 * POST /api/vouchers/expirar-vencidos   rotina de expiracao em lote
 * ================================================================
 */
@RestController
@RequestMapping("/api/vouchers")
public class VoucherResource {

    private final ConversorBO conversorBO;
    private final CupomBO cupomBO;
    private final UsuarioBO usuarioBO;
    private final OperadoraBO operadoraBO;
    private final FaixaCupomDAO faixaDAO;
    private final VoucherDAO voucherDAO;

    public VoucherResource(ConversorBO conversorBO, CupomBO cupomBO,
                           UsuarioBO usuarioBO, OperadoraBO operadoraBO,
                           FaixaCupomDAO faixaDAO, VoucherDAO voucherDAO) {
        this.conversorBO = conversorBO;
        this.cupomBO = cupomBO;
        this.usuarioBO = usuarioBO;
        this.operadoraBO = operadoraBO;
        this.faixaDAO = faixaDAO;
        this.voucherDAO = voucherDAO;
    }

    /**
     * Resgata Pontos ECOA em um cupom de transporte.
     *
     * Aplica, em uma unica transacao: verificacao do limite antifraude
     * (RN07), validacao de saldo (RN02), debito da carteira, geracao do
     * codigo com hash HMAC-SHA256 e registro no log de auditoria (RN05).
     *
     * @return 201 com o cupom gerado
     *         400 se operadora ou faixa forem invalidas
     *         404 se usuario, operadora ou faixa nao existirem
     *         409 se o saldo for insuficiente ou o antifraude bloquear
     */
    @PostMapping
    public ResponseEntity<CupomDTO> resgatar(@RequestBody NovoVoucherRequest requisicao) {
        if (requisicao == null || requisicao.getIdUsuario() == null
                || requisicao.getIdOperadora() == null || requisicao.getIdFaixa() == null) {
            throw new IllegalArgumentException(
                "Informe idUsuario, idOperadora e idFaixa no corpo da requisicao");
        }

        Usuario usuario = usuarioBO.buscarPorId(requisicao.getIdUsuario());
        Operadora operadora = operadoraBO.buscarPorId(requisicao.getIdOperadora());

        FaixaCupom faixa = faixaDAO.buscarPorId(requisicao.getIdFaixa());
        if (faixa == null) {
            throw new RecursoNaoEncontradoException(
                "Faixa de cupom nao encontrada. Id: " + requisicao.getIdFaixa());
        }

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(conversorBO.converterPontosEmCupom(usuario, operadora, faixa));
    }

    /**
     * @return 200 com o cupom e os dados de operadora e faixa,
     *         ou 404 se o codigo nao existir
     */
    @GetMapping("/{codigo}")
    public ResponseEntity<Voucher> buscarPorCodigo(@PathVariable String codigo) {
        Voucher voucher = voucherDAO.buscarPorCodigo(codigo);
        if (voucher == null) {
            throw new RecursoNaoEncontradoException("Cupom nao encontrado: " + codigo);
        }
        return ResponseEntity.ok(voucher);
    }

    /**
     * Valida e consome o cupom no validador da operadora, registrando a
     * viagem e o CO2 evitado.
     *
     * @return 200 com a viagem registrada
     *         404 se o cupom nao existir
     *         409 se o cupom ja foi usado, expirou, ou pertence a outra
     *             operadora
     */
    @PostMapping("/{codigo}/utilizar")
    public ResponseEntity<Viagem> utilizar(@PathVariable String codigo,
                                           @RequestBody UtilizarCupomRequest requisicao) {
        if (requisicao == null || requisicao.getIdOperadora() == null) {
            throw new IllegalArgumentException(
                "Informe idOperadora no corpo da requisicao");
        }
        Operadora operadora = operadoraBO.buscarPorId(requisicao.getIdOperadora());
        return ResponseEntity.ok(cupomBO.utilizarCupom(codigo, operadora));
    }

    /**
     * Cancela um cupom ainda nao utilizado e devolve os pontos a
     * carteira, liberando uma vaga no limite antifraude.
     *
     * @return 200 com a quantidade de pontos devolvidos
     *         404 se o cupom nao existir
     *         409 se o cupom nao estiver em status GERADO
     */
    @PostMapping("/{codigo}/estornar")
    public ResponseEntity<Map<String, Object>> estornar(@PathVariable String codigo) {
        int pontos = conversorBO.estornarCupom(codigo);
        return ResponseEntity.ok(Map.of(
            "codigo", codigo,
            "pontosDevolvidos", pontos,
            "mensagem", "Cupom cancelado e pontos devolvidos a carteira"));
    }

    /**
     * Rotina administrativa que move para EXPIRADO os cupons vencidos.
     * Sem ela, o limite antifraude contaria eternamente cupons que ja
     * passaram da validade de 24 horas.
     *
     * @return 200 com a quantidade de cupons expirados nesta execucao
     */
    @PostMapping("/expirar-vencidos")
    public ResponseEntity<Map<String, Object>> expirarVencidos() {
        int expirados = cupomBO.expirarCuponsVencidos();
        return ResponseEntity.ok(Map.of(
            "cuponsExpirados", expirados,
            "mensagem", "Rotina de expiracao executada"));
    }
}
