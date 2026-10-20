package br.com.fiap.mobilidadeverde.bo;

import org.springframework.stereotype.Service;
import br.com.fiap.mobilidadeverde.dao.VoucherDAO;
import br.com.fiap.mobilidadeverde.dto.ResultadoValidacao;
import br.com.fiap.mobilidadeverde.model.StatusVoucher;
import br.com.fiap.mobilidadeverde.model.Usuario;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * ================================================================
 * SERVICE DE PREVENCAO A FRAUDE
 *
 * Concentra o metodo de logica de negocio 3 da entrega.
 *
 * RN07 - Um usuario pode manter no maximo cinco cupons em status
 * GERADO ao mesmo tempo. Enquanto o limite estiver atingido, novas
 * solicitacoes sao recusadas ate que algum cupom seja utilizado,
 * expire ou seja cancelado.
 *
 * MOTIVACAO. Sem o limite, uma conta comprometida poderia drenar
 * todo o saldo numa rajada de requisicoes, convertendo pontos em
 * cupons efetivamente transferiveis antes que qualquer
 * monitoramento reagisse. O teto de cinco cobre com folga o uso
 * legitimo, ja que os cupons expiram em 24 horas.
 *
 * POR QUE A REGRA VIVE AQUI E NAO NO BANCO. Uma CHECK constraint no
 * Oracle avalia apenas a linha sendo inserida; nao faz agregacao
 * nem consulta outras linhas da mesma tabela. Contar cupons por
 * usuario e uma agregacao sobre o conjunto. As alternativas de
 * banco seriam trigger de linha, que incorre em erro de tabela
 * mutante, ou view materializada com constraint, de custo
 * desproporcional para o volume do MVP. O banco contribui com o
 * indice IDX_VOUCHER_USR_STAT, que cobre exatamente o predicado
 * (id_usuario, status) e resolve a contagem por acesso ao indice.
 *
 * MUDANCA DA SPRINT 1+2 PARA A SPRINT 3. A contagem deixou de
 * percorrer uma lista em memoria e passou a ser um COUNT(*) no
 * banco, executado DENTRO da mesma transacao que gera o cupom.
 * Isso e o que impede que duas requisicoes simultaneas do mesmo
 * usuario furem o limite.
 * ================================================================
 */
@Service
public class AntiFraudeBO {

    /** Teto de cupons simultaneos em status GERADO por usuario. */
    private static final int LIMITE_CUPONS_ATIVOS = 5;

    private final VoucherDAO voucherDAO;

    public AntiFraudeBO(VoucherDAO voucherDAO) {
        this.voucherDAO = voucherDAO;
    }

    // ================================================================
    // METODO DE NEGOCIO 3 - VALIDACAO ANTIFRAUDE
    // ================================================================

    /**
     * Versao autonoma, que abre a propria conexao.
     * Usada em consultas avulsas, fora de transacao.
     */
    public ResultadoValidacao validarTransacao(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) {
            return ResultadoValidacao.reprovado("Usuario nao informado");
        }
        int cuponsAtivos = voucherDAO.contarPorUsuarioEStatus(
            usuario.getId(), StatusVoucher.GERADO);
        return avaliar(cuponsAtivos);
    }

    /**
     * Versao transacional, que reaproveita a conexao do resgate.
     * E esta que o ConversorBO chama.
     */
    public ResultadoValidacao validarTransacao(Connection conexao, Usuario usuario)
            throws SQLException {
        if (usuario == null || usuario.getId() == null) {
            return ResultadoValidacao.reprovado("Usuario nao informado");
        }
        int cuponsAtivos = voucherDAO.contarPorUsuarioEStatus(
            conexao, usuario.getId(), StatusVoucher.GERADO);
        return avaliar(cuponsAtivos);
    }

    /** Quantos cupons em aberto o usuario ainda pode gerar. */
    public int vagasRestantes(Usuario usuario) {
        int cuponsAtivos = voucherDAO.contarPorUsuarioEStatus(
            usuario.getId(), StatusVoucher.GERADO);
        return Math.max(0, LIMITE_CUPONS_ATIVOS - cuponsAtivos);
    }

    public int getLimiteCuponsAtivos() {
        return LIMITE_CUPONS_ATIVOS;
    }

    private ResultadoValidacao avaliar(int cuponsAtivos) {
        if (cuponsAtivos >= LIMITE_CUPONS_ATIVOS) {
            return ResultadoValidacao.reprovado(
                "Limite de " + LIMITE_CUPONS_ATIVOS + " cupons ativos atingido. "
                + "Use, cancele ou aguarde a expiracao de algum cupom.");
        }
        return ResultadoValidacao.aprovado();
    }
}
