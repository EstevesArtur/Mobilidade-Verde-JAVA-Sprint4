package br.com.fiap.mobilidadeverde.bo;

import br.com.fiap.mobilidadeverde.conexao.ConexaoOracle;
import br.com.fiap.mobilidadeverde.dao.CarteiraDAO;
import br.com.fiap.mobilidadeverde.dao.DAOException;
import br.com.fiap.mobilidadeverde.dao.FavoritaDAO;
import br.com.fiap.mobilidadeverde.dao.TransacaoDAO;
import br.com.fiap.mobilidadeverde.dao.UsuarioDAO;
import br.com.fiap.mobilidadeverde.dao.ViagemDAO;
import br.com.fiap.mobilidadeverde.dao.VoucherDAO;
import br.com.fiap.mobilidadeverde.dto.UsuarioRequest;
import br.com.fiap.mobilidadeverde.model.Carteira;
import br.com.fiap.mobilidadeverde.model.OrigemTransacao;
import br.com.fiap.mobilidadeverde.model.TipoTransacao;
import br.com.fiap.mobilidadeverde.model.TransacaoPontos;
import br.com.fiap.mobilidadeverde.model.Usuario;
import br.com.fiap.mobilidadeverde.util.HashUtil;
import br.com.fiap.mobilidadeverde.util.ValidadorUtil;
import br.com.fiap.mobilidadeverde.exception.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * ================================================================
 * BO DE USUARIO
 *
 * Concentra as regras de cadastro do cliente da plataforma.
 *
 * Duas delas merecem destaque:
 *
 * 1. O CPF e a senha nunca sao gravados em texto. Chegam em claro na
 *    requisicao, sao validados e imediatamente convertidos em hash
 *    SHA-256. O banco guarda apenas o hash, atendendo a LGPD.
 *
 * 2. Criar um usuario nao e um INSERT isolado: todo usuario nasce com
 *    uma carteira (RN01, relacao 1:1 obrigatoria) e, quando recebe
 *    saldo inicial, com o lancamento correspondente no log de
 *    auditoria (RN05). As tres escritas correm na mesma transacao.
 * ================================================================
 */
@Service
public class UsuarioBO {

    private final UsuarioDAO usuarioDAO;
    private final CarteiraDAO carteiraDAO;
    private final FavoritaDAO favoritaDAO;
    private final VoucherDAO voucherDAO;
    private final ViagemDAO viagemDAO;
    private final TransacaoDAO transacaoDAO;

    public UsuarioBO(UsuarioDAO usuarioDAO, CarteiraDAO carteiraDAO,
                     FavoritaDAO favoritaDAO, VoucherDAO voucherDAO,
                     ViagemDAO viagemDAO, TransacaoDAO transacaoDAO) {
        this.usuarioDAO = usuarioDAO;
        this.carteiraDAO = carteiraDAO;
        this.favoritaDAO = favoritaDAO;
        this.voucherDAO = voucherDAO;
        this.viagemDAO = viagemDAO;
        this.transacaoDAO = transacaoDAO;
    }

    // ------------------------------------------------------------
    // READ
    // ------------------------------------------------------------

    public List<Usuario> listar() {
        return usuarioDAO.listarTodos();
    }

    /**
     * Busca o usuario pelo id e recusa com mensagem clara quando ele
     * nao existe, para que a camada Resource devolva 404.
     */
    public Usuario buscarPorId(Long id) {
        Usuario usuario = usuarioDAO.buscarPorId(id);
        if (usuario == null) {
            throw new RecursoNaoEncontradoException("Usuario nao encontrado. Id: " + id);
        }
        return usuario;
    }

    /** Saldo de Pontos ECOA, lido direto da carteira no banco. */
    public int consultarSaldo(Usuario usuario) {
        Carteira carteira = carteiraDAO.buscarPorUsuario(usuario);
        if (carteira == null) {
            throw new IllegalStateException(
                "Usuario sem carteira: " + usuario.getEmail() + " (RN01)");
        }
        return carteira.getSaldoPontos();
    }

    // ------------------------------------------------------------
    // CREATE
    // ------------------------------------------------------------

    /**
     * Cadastra um usuario com carteira e, se houver, saldo inicial.
     *
     * @throws IllegalArgumentException se e-mail ou CPF forem invalidos,
     *                                  ou se o e-mail ja estiver em uso
     */
    public Usuario cadastrar(UsuarioRequest requisicao) {
        validar(requisicao, true);

        if (usuarioDAO.buscarPorEmail(requisicao.getEmail()) != null) {
            throw new IllegalArgumentException(
                "Ja existe usuario cadastrado com o e-mail " + requisicao.getEmail());
        }

        int saldoInicial = requisicao.getSaldoInicial() == null
            ? 0 : requisicao.getSaldoInicial();
        if (saldoInicial < 0) {
            throw new IllegalArgumentException("Saldo inicial nao pode ser negativo (RN02)");
        }

        Connection conexao = null;
        try {
            conexao = ConexaoOracle.abrir();
            conexao.setAutoCommit(false);

            Usuario usuario = new Usuario(null,
                requisicao.getNome(),
                requisicao.getEmail(),
                HashUtil.sha256(requisicao.getSenha()),
                HashUtil.sha256(requisicao.getCpf()),
                requisicao.getCidade());
            usuarioDAO.inserir(conexao, usuario);

            carteiraDAO.inserir(conexao, new Carteira(null, usuario, saldoInicial));

            //A restricao CK_TRANSACAO_PONTOS exige pontos > 0, entao um
            //usuario criado sem saldo nao gera lancamento de credito.
            if (saldoInicial > 0) {
                transacaoDAO.inserir(conexao, new TransacaoPontos(null, usuario,
                    TipoTransacao.CREDITO, saldoInicial, OrigemTransacao.OUTROS, null,
                    "Saldo acumulado na plataforma SoulUp", null));
            }

            conexao.commit();
            return usuario;

        } catch (SQLException e) {
            ConexaoOracle.desfazer(conexao);
            throw new DAOException("Falha ao cadastrar o usuario " + requisicao.getEmail(), e);

        } catch (RuntimeException e) {
            ConexaoOracle.desfazer(conexao);
            throw e;

        } finally {
            ConexaoOracle.fechar(conexao);
        }
    }

    // ------------------------------------------------------------
    // UPDATE
    // ------------------------------------------------------------

    /**
     * Atualiza os dados cadastrais. Senha e CPF nao sao alterados por
     * aqui: credencial e documento tem fluxo proprio de alteracao.
     */
    public Usuario atualizar(Long id, UsuarioRequest requisicao) {
        Usuario usuario = buscarPorId(id);
        validar(requisicao, false);

        Usuario donoDoEmail = usuarioDAO.buscarPorEmail(requisicao.getEmail());
        if (donoDoEmail != null && !donoDoEmail.getId().equals(id)) {
            throw new IllegalArgumentException(
                "O e-mail " + requisicao.getEmail() + " ja pertence a outro usuario");
        }

        usuario.setNome(requisicao.getNome());
        usuario.setEmail(requisicao.getEmail());
        usuario.setCidade(requisicao.getCidade());

        if (!usuarioDAO.atualizar(usuario)) {
            throw new IllegalArgumentException("Usuario nao encontrado para atualizacao. Id: " + id);
        }
        return usuario;
    }

    // ------------------------------------------------------------
    // DELETE
    // ------------------------------------------------------------

    /**
     * Remove o usuario e tudo que depende dele, na ordem inversa das
     * chaves estrangeiras:
     *
     *   T_VIAGEM -> T_TRANSACAO_PONTOS -> T_VOUCHER
     *            -> T_USUARIO_OPERADORA_FAVORITA
     *            -> T_CARTEIRA -> T_USUARIO
     *
     * Essa ordem nao e opcional: tentar apagar o usuario primeiro
     * resulta em ORA-02292, e essa recusa do banco e justamente a
     * protecao do historico financeiro contra exclusao acidental.
     * Tudo corre em uma unica transacao.
     */
    public void remover(Long id) {
        Usuario usuario = buscarPorId(id);

        Connection conexao = null;
        try {
            conexao = ConexaoOracle.abrir();
            conexao.setAutoCommit(false);

            viagemDAO.deletarPorUsuario(conexao, id);
            transacaoDAO.deletarPorUsuario(conexao, id);
            voucherDAO.deletarPorUsuario(conexao, id);
            favoritaDAO.deletarPorUsuario(conexao, id);
            carteiraDAO.deletarPorUsuario(conexao, id);
            usuarioDAO.deletar(conexao, id);

            conexao.commit();

        } catch (SQLException e) {
            ConexaoOracle.desfazer(conexao);
            throw new DAOException("Falha ao remover o usuario " + usuario.getEmail(), e);

        } catch (RuntimeException e) {
            ConexaoOracle.desfazer(conexao);
            throw e;

        } finally {
            ConexaoOracle.fechar(conexao);
        }
    }

    // ------------------------------------------------------------
    // VALIDACAO
    // ------------------------------------------------------------

    private void validar(UsuarioRequest requisicao, boolean exigirCredenciais) {
        if (requisicao == null) {
            throw new IllegalArgumentException("Corpo da requisicao ausente");
        }
        if (vazio(requisicao.getNome())) {
            throw new IllegalArgumentException("Nome e obrigatorio");
        }
        if (!ValidadorUtil.validarEmail(requisicao.getEmail())) {
            throw new IllegalArgumentException("E-mail invalido: " + requisicao.getEmail());
        }
        if (exigirCredenciais) {
            if (vazio(requisicao.getSenha())) {
                throw new IllegalArgumentException("Senha e obrigatoria");
            }
            if (!ValidadorUtil.validarCpf(requisicao.getCpf())) {
                throw new IllegalArgumentException(
                    "CPF invalido: os digitos verificadores nao conferem");
            }
        }
    }

    private boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }
}
