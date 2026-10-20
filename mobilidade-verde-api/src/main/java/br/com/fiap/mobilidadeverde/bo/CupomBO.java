package br.com.fiap.mobilidadeverde.bo;

import br.com.fiap.mobilidadeverde.exception.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import br.com.fiap.mobilidadeverde.dao.DAOException;
import br.com.fiap.mobilidadeverde.dao.ViagemDAO;
import br.com.fiap.mobilidadeverde.dao.VoucherDAO;
import br.com.fiap.mobilidadeverde.conexao.ConexaoOracle;
import br.com.fiap.mobilidadeverde.dao.SequenceHelper;
import br.com.fiap.mobilidadeverde.model.Operadora;
import br.com.fiap.mobilidadeverde.model.StatusVoucher;
import br.com.fiap.mobilidadeverde.model.Viagem;
import br.com.fiap.mobilidadeverde.model.Voucher;
import br.com.fiap.mobilidadeverde.util.HashUtil;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * ================================================================
 * SERVICE DE USO DO CUPOM
 *
 * Concentra os metodos de logica de negocio 2 e 6 da entrega.
 *
 * Representa o momento em que o cupom e apresentado no validador da
 * operadora. E o ponto de integracao com o mundo externo, e por
 * isso o metodo e defensivo: valida propriedade, integridade,
 * estado e prazo antes de aceitar o embarque.
 * ================================================================
 */
@Service
public class CupomBO {

    /** Emissao media de um carro individual urbano, em kg de CO2 por km (MMA). */
    private static final double FATOR_CO2_CARRO_KG_KM = 0.180;

    /** Distancia media de uma viagem urbana em Sao Paulo, em km (ANTP). */
    private static final double KM_MEDIO_POR_PASSAGEM = 7.0;

    /** Mesma chave compartilhada usada pelo ConversorBO. */
    private static final String CHAVE_HMAC_MOCK = "soulup-mobilidade-verde-2026-mvp-key";

    private final VoucherDAO voucherDAO;
    private final ViagemDAO viagemDAO;

    public CupomBO(VoucherDAO voucherDAO, ViagemDAO viagemDAO) {
        this.voucherDAO = voucherDAO;
        this.viagemDAO = viagemDAO;
    }

    // ================================================================
    // METODO DE NEGOCIO 2 - UTILIZACAO DO CUPOM E REGISTRO DA VIAGEM
    // Regras aplicadas: RN04 e calculo de impacto ambiental
    // ================================================================

    /**
     * Valida e consome um cupom no validador da operadora, criando a
     * viagem correspondente.
     *
     * Barreiras aplicadas, nesta ordem:
     *  1. O cupom existe.
     *  2. O cupom pertence a operadora que esta validando. Cupom da
     *     TOP nao vale na SPTrans.
     *  3. O HMAC recalculado bate com o gravado. Detecta cupom
     *     adulterado ou forjado por terceiro.
     *  4. O cupom ainda nao foi usado (RN04, uso unico).
     *  5. O cupom nao expirou. Se expirou, o status e atualizado
     *     para EXPIRADO antes de recusar.
     *
     * Passadas as barreiras, o cupom vira UTILIZADO e a viagem e
     * gravada. As duas escritas correm na mesma transacao.
     *
     * CALCULO DO CO2 EVITADO. O numero de passagens e derivado do
     * valor do cupom dividido pela tarifa da operadora: um cupom de
     * R$ 10,00 em uma tarifa de R$ 4,40 corresponde a 2 passagens.
     * Cada passagem equivale a 7 km urbanos. O CO2 evitado e a
     * diferenca entre a emissao do carro individual (0,180 kg/km) e
     * a do transporte publico (0,082 kg/km da operadora), ou seja
     * 0,098 kg por km rodado.
     *
     * @param codigo    codigo apresentado pelo usuario
     * @param operadora operadora que esta validando o cupom
     * @return a viagem registrada, com o CO2 evitado calculado
     */
    public Viagem utilizarCupom(String codigo, Operadora operadora) {
        // 1. Existencia.
        Voucher voucher = voucherDAO.buscarPorCodigo(codigo);
        if (voucher == null) {
            throw new RecursoNaoEncontradoException("Cupom nao encontrado: " + codigo);
        }

        // 2. Propriedade cruzada entre operadoras.
        if (!voucher.getOperadora().getId().equals(operadora.getId())) {
            throw new IllegalStateException(
                "Cupom nao pertence a operadora " + operadora.getNome()
                + ". Foi emitido para " + voucher.getOperadora().getNome() + ".");
        }

        // 3. Integridade do codigo.
        String hashEsperado = HashUtil.hmacSha256(
            voucher.getCodigo() + "|" + voucher.getGeradoEm(), CHAVE_HMAC_MOCK);
        if (!hashEsperado.equals(voucher.getHashValidacao())) {
            throw new IllegalStateException(
                "Hash de validacao invalido. Cupom possivelmente adulterado.");
        }

        // 4. Uso unico.
        if (voucher.getStatus() == StatusVoucher.UTILIZADO) {
            throw new IllegalStateException(
                "Cupom ja utilizado em " + voucher.getUtilizadoEm() + " (RN04).");
        }
        if (voucher.getStatus() != StatusVoucher.GERADO) {
            throw new IllegalStateException(
                "Cupom em estado invalido para uso: " + voucher.getStatus());
        }

        // 5. Prazo. Cupom vencido e marcado como EXPIRADO e recusado.
        if (voucher.estaExpirado()) {
            voucher.setStatus(StatusVoucher.EXPIRADO);
            voucherDAO.atualizarStatus(voucher);
            throw new IllegalStateException(
                "Cupom expirado em " + voucher.getExpiraEm() + " (RN04).");
        }

        // Calculo do impacto.
        int passagens = calcularPassagens(voucher);
        double kmEstimado = passagens * KM_MEDIO_POR_PASSAGEM;
        double co2EvitadoPorKm = FATOR_CO2_CARRO_KG_KM - voucher.getOperadora().getFatorCo2KgKm();
        if (co2EvitadoPorKm < 0) {
            co2EvitadoPorKm = 0;
        }
        int co2EvitadoG = (int) Math.round(co2EvitadoPorKm * kmEstimado * 1000);

        Connection conexao = null;
        try {
            conexao = ConexaoOracle.abrir();
            conexao.setAutoCommit(false);

            voucher.setStatus(StatusVoucher.UTILIZADO);
            voucher.setUtilizadoEm(SequenceHelper.agora());
            voucherDAO.atualizarStatus(conexao, voucher);

            Viagem viagem = new Viagem(null, voucher, kmEstimado, co2EvitadoG);
            viagemDAO.inserir(conexao, viagem);

            conexao.commit();
            return viagem;

        } catch (SQLException e) {
            ConexaoOracle.desfazer(conexao);
            throw new DAOException("Falha ao registrar o uso do cupom " + codigo, e);

        } catch (RuntimeException e) {
            ConexaoOracle.desfazer(conexao);
            throw e;

        } finally {
            ConexaoOracle.fechar(conexao);
        }
    }

    /**
     * Quantas passagens o valor do cupom cobre na tarifa da operadora.
     *
     * Usa arredondamento, e nao divisao inteira, porque a bilhetagem
     * acumula o troco: um cupom de R$ 20,00 numa tarifa de R$ 4,40 da
     * 4,55 passagens, e na pratica o usuario faz a quinta viagem
     * completando poucos centavos do proprio saldo.
     *
     * Minimo de uma passagem: a menor faixa (R$ 2,00) nao cobre a
     * tarifa cheia, mas ainda financia parte de uma viagem real.
     *
     * Esta e exatamente a mesma formula usada pelo front-end em
     * src/utils/co2.ts, para que a tela e o backend nunca mostrem
     * numeros diferentes para o mesmo cupom.
     */
    private int calcularPassagens(Voucher voucher) {
        int tarifa = voucher.getOperadora().getValorPassagemCentavos();
        if (tarifa <= 0) {
            return 1;
        }
        double passagens = (double) voucher.getFaixa().getValorCentavos() / tarifa;
        return Math.max(1, (int) Math.round(passagens));
    }

    // ================================================================
    // METODO DE NEGOCIO 6 - EXPIRACAO EM LOTE DE CUPONS VENCIDOS
    // Regra aplicada: RN04
    // ================================================================

    /**
     * Rotina administrativa que varre os cupons vencidos e os move
     * para EXPIRADO.
     *
     * E uma regra que so existe com banco de dados: em memoria, a
     * expiracao era calculada na hora da leitura. Persistida, ela
     * precisa de uma rotina que atualize o estado, senao o limite
     * antifraude da RN07 contaria eternamente cupons que ja
     * venceram e o usuario ficaria travado.
     *
     * Resolvida em uma unica instrucao UPDATE no banco, sem trazer
     * linha alguma para a aplicacao.
     *
     * @return quantidade de cupons expirados nesta execucao
     */
    public int expirarCuponsVencidos() {
        return voucherDAO.expirarVencidos();
    }
}
