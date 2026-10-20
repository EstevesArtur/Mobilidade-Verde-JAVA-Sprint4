package br.com.fiap.mobilidadeverde.bo;

import br.com.fiap.mobilidadeverde.exception.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import br.com.fiap.mobilidadeverde.dao.DAOException;
import br.com.fiap.mobilidadeverde.dao.CarteiraDAO;
import br.com.fiap.mobilidadeverde.dao.TransacaoDAO;
import br.com.fiap.mobilidadeverde.dao.VoucherDAO;
import br.com.fiap.mobilidadeverde.conexao.ConexaoOracle;
import br.com.fiap.mobilidadeverde.dao.SequenceHelper;
import br.com.fiap.mobilidadeverde.dto.CupomDTO;
import br.com.fiap.mobilidadeverde.dto.ResultadoValidacao;
import br.com.fiap.mobilidadeverde.model.Carteira;
import br.com.fiap.mobilidadeverde.model.FaixaCupom;
import br.com.fiap.mobilidadeverde.model.Operadora;
import br.com.fiap.mobilidadeverde.model.OrigemTransacao;
import br.com.fiap.mobilidadeverde.model.StatusVoucher;
import br.com.fiap.mobilidadeverde.model.TipoTransacao;
import br.com.fiap.mobilidadeverde.model.TransacaoPontos;
import br.com.fiap.mobilidadeverde.model.Usuario;
import br.com.fiap.mobilidadeverde.model.Voucher;
import br.com.fiap.mobilidadeverde.util.CupomGenerator;
import br.com.fiap.mobilidadeverde.util.HashUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * ================================================================
 * SERVICE DE CONVERSAO DE PONTOS EM CUPOM
 *
 * Concentra os metodos de logica de negocio 1 e 5 da entrega.
 *
 * Diferenca central em relacao a Sprint 1+2: o resgate agora e
 * ATOMICO. Ele altera tres tabelas (T_CARTEIRA, T_VOUCHER e
 * T_TRANSACAO_PONTOS) e as tres alteracoes acontecem dentro de uma
 * unica transacao JDBC. Ou tudo e gravado, ou nada e.
 *
 * Sem esse controle, uma falha depois do debito e antes do insert
 * do voucher deixaria o usuario com pontos a menos e sem cupom
 * nenhum. Em produto que movimenta saldo, isso e inaceitavel.
 * ================================================================
 */
@Service
public class ConversorBO {

    /** Validade do cupom em horas, conforme parametros oficiais do projeto. */
    private static final int HORAS_VALIDADE_CUPOM = 24;

    /**
     * Chave compartilhada SoulUp / operadora usada no HMAC de validacao.
     * MOCK do MVP. Em producao vem de secret manager e e diferente
     * por operadora.
     */
    private static final String CHAVE_HMAC_MOCK = "soulup-mobilidade-verde-2026-mvp-key";

    private final CarteiraDAO carteiraDAO;
    private final VoucherDAO voucherDAO;
    private final TransacaoDAO transacaoDAO;
    private final AntiFraudeBO antiFraudeService;

    public ConversorBO(CarteiraDAO carteiraDAO, VoucherDAO voucherDAO,
                            TransacaoDAO transacaoDAO, AntiFraudeBO antiFraudeService) {
        this.carteiraDAO = carteiraDAO;
        this.voucherDAO = voucherDAO;
        this.transacaoDAO = transacaoDAO;
        this.antiFraudeService = antiFraudeService;
    }

    // ================================================================
    // METODO DE NEGOCIO 1 - RESGATE DE PONTOS EM CUPOM
    // Regras aplicadas: RN02, RN03, RN05, RN06 e RN07
    // ================================================================

    /**
     * Converte Pontos ECOA em um cupom de transporte publico.
     *
     * Sequencia executada:
     *  1. Valida que a operadora esta ativa (RN06).
     *  2. Valida que a faixa pertence ao catalogo e esta ativa (RN03).
     *  3. Abre a transacao e desliga o autocommit.
     *  4. Consulta o antifraude, que conta cupons GERADO no banco (RN07).
     *  5. Carrega a carteira e debita, protegendo o saldo (RN02).
     *  6. Gera codigo no formato da operadora e o HMAC de validacao.
     *  7. Grava o voucher com status GERADO e validade de 24 horas.
     *  8. Grava o movimento no log append-only (RN05).
     *  9. Confirma a transacao.
     *
     * Qualquer falha entre os passos 5 e 8 desfaz tudo.
     *
     * @return dados do cupom prontos para exibicao
     * @throws IllegalArgumentException se operadora ou faixa forem invalidas
     * @throws IllegalStateException    se o antifraude bloquear ou faltar saldo
     */
    public CupomDTO converterPontosEmCupom(Usuario usuario, Operadora operadora,
                                           FaixaCupom faixa) {
        // 1 e 2. Validacoes de catalogo, antes de tocar no banco.
        if (operadora == null || !operadora.isAtivo()) {
            throw new IllegalArgumentException(
                "Operadora indisponivel. Cupom so pode ser gerado para operadora ativa (RN06).");
        }
        if (faixa == null || !faixa.isAtivo()) {
            throw new IllegalArgumentException(
                "Faixa de cupom indisponivel. Nao existe valor livre (RN03).");
        }

        Connection conexao = null;
        try {
            // 3. Abre a transacao.
            conexao = ConexaoOracle.abrir();
            conexao.setAutoCommit(false);

            // 4. Antifraude dentro da mesma transacao que gera o cupom.
            ResultadoValidacao validacao =
                antiFraudeService.validarTransacao(conexao, usuario);
            if (!validacao.isOk()) {
                throw new IllegalStateException("Antifraude bloqueou: " + validacao.getMotivo());
            }

            // 5. Carteira e debito. A propria entidade protege a invariante de saldo.
            Carteira carteira = carteiraDAO.buscarPorUsuario(usuario);
            if (carteira == null) {
                throw new IllegalStateException(
                    "Usuario sem carteira: " + usuario.getEmail() + " (RN01)");
            }
            int pontos = faixa.getPontosNecessarios();
            carteira.debitar(pontos);
            carteiraDAO.atualizarSaldo(conexao, carteira);

            // 6. Codigo e hash. A data e truncada no segundo para que o
            //    HMAC continue valido depois de passar pelo TIMESTAMP do
            //    Oracle, que guarda seis casas decimais.
            String codigo = CupomGenerator.gerar(operadora);
            LocalDateTime geradoEm = SequenceHelper.agora();
            LocalDateTime expiraEm = geradoEm.plusHours(HORAS_VALIDADE_CUPOM);
            String hashValidacao =
                HashUtil.hmacSha256(codigo + "|" + geradoEm, CHAVE_HMAC_MOCK);

            // 7. Voucher.
            Voucher voucher = new Voucher(null, usuario, operadora, faixa,
                codigo, hashValidacao, geradoEm, expiraEm);
            voucherDAO.inserir(conexao, voucher);

            // 8. Auditoria.
            TransacaoPontos transacao = new TransacaoPontos(null, usuario,
                TipoTransacao.DEBITO, pontos, OrigemTransacao.VOUCHER_GERACAO,
                voucher, "Resgate cupom " + codigo, null);
            transacaoDAO.inserir(conexao, transacao);

            // 9. Confirma.
            conexao.commit();

            return new CupomDTO(codigo, operadora.getNome(), faixa.getValorCentavos(),
                pontos, expiraEm);

        } catch (SQLException e) {
            ConexaoOracle.desfazer(conexao);
            throw new DAOException("Falha ao resgatar cupom. Nenhum ponto foi debitado.", e);

        } catch (RuntimeException e) {
            ConexaoOracle.desfazer(conexao);
            throw e;

        } finally {
            ConexaoOracle.fechar(conexao);
        }
    }

    // ================================================================
    // METODO DE NEGOCIO 5 - ESTORNO DE CUPOM NAO UTILIZADO
    // Regras aplicadas: RN02, RN04 e RN05
    // ================================================================

    /**
     * Cancela um cupom ainda nao utilizado e devolve os pontos.
     *
     * Fecha o ciclo de vida do voucher previsto no modelo: o status
     * CANCELADO e a origem de transacao ESTORNO ja existiam nas
     * restricoes CK_VOUCHER_STATUS e CK_TRANSACAO_ORIGEM desde a
     * Sprint 1+2, mas nenhum codigo os produzia.
     *
     * Tambem e o caminho legitimo para o usuario liberar espaco no
     * limite de cupons simultaneos da RN07 sem esperar as 24 horas
     * de expiracao.
     *
     * As tres escritas correm na mesma transacao.
     *
     * @param codigo codigo do cupom a estornar
     * @return quantidade de pontos devolvidos a carteira
     */
    public int estornarCupom(String codigo) {
        Voucher voucher = voucherDAO.buscarPorCodigo(codigo);
        if (voucher == null) {
            throw new RecursoNaoEncontradoException("Cupom nao encontrado: " + codigo);
        }
        if (voucher.getStatus() != StatusVoucher.GERADO) {
            throw new IllegalStateException(
                "So e possivel estornar cupom em status GERADO. Status atual: "
                + voucher.getStatus() + " (RN04).");
        }

        Connection conexao = null;
        try {
            conexao = ConexaoOracle.abrir();
            conexao.setAutoCommit(false);

            // 1. Cupom passa a CANCELADO.
            voucher.setStatus(StatusVoucher.CANCELADO);
            voucherDAO.atualizarStatus(conexao, voucher);

            // 2. Pontos voltam para a carteira.
            Carteira carteira = carteiraDAO.buscarPorUsuario(voucher.getUsuario());
            if (carteira == null) {
                throw new IllegalStateException("Usuario sem carteira (RN01)");
            }
            int pontos = voucher.getFaixa().getPontosNecessarios();
            carteira.creditar(pontos);
            carteiraDAO.atualizarSaldo(conexao, carteira);

            // 3. Movimento auditado como CREDITO / ESTORNO.
            TransacaoPontos transacao = new TransacaoPontos(null, voucher.getUsuario(),
                TipoTransacao.CREDITO, pontos, OrigemTransacao.ESTORNO,
                voucher, "Estorno do cupom " + codigo, null);
            transacaoDAO.inserir(conexao, transacao);

            conexao.commit();
            return pontos;

        } catch (SQLException e) {
            ConexaoOracle.desfazer(conexao);
            throw new DAOException("Falha ao estornar o cupom " + codigo, e);

        } catch (RuntimeException e) {
            ConexaoOracle.desfazer(conexao);
            throw e;

        } finally {
            ConexaoOracle.fechar(conexao);
        }
    }
}
