package br.com.fiap.mobilidadeverde.bo;

import br.com.fiap.mobilidadeverde.exception.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import br.com.fiap.mobilidadeverde.dao.FavoritaDAO;
import br.com.fiap.mobilidadeverde.dao.OperadoraDAO;
import br.com.fiap.mobilidadeverde.model.Operadora;
import br.com.fiap.mobilidadeverde.model.Usuario;
import br.com.fiap.mobilidadeverde.model.UsuarioOperadoraFavorita;

import java.util.ArrayList;
import java.util.List;

/**
* BO de catalogo de operadoras parceiras e de preferencias do
 * usuario.
 *
 * Novidade da Sprint 3: passou a orquestrar tambem a associativa
 * N:N. Na Sprint 1+2 a classe UsuarioOperadoraFavorita existia no
 * modelo mas nao tinha repositorio nem uso; agora ela tem DAO
 * proprio e alimenta a ordenacao dos cards de operadora na tela de
 * resgate.
 */
@Service
public class OperadoraBO {

    private final OperadoraDAO operadoraDAO;
    private final FavoritaDAO favoritaDAO;

    public OperadoraBO(OperadoraDAO operadoraDAO, FavoritaDAO favoritaDAO) {
        this.operadoraDAO = operadoraDAO;
        this.favoritaDAO = favoritaDAO;
    }

    /** Catalogo que o usuario enxerga: apenas operadoras ativas (RN06). */
    public List<Operadora> listarOperadoras() {
        return operadoraDAO.listarAtivas();
    }

    /** Visao administrativa: inclui as operadoras desativadas. */
    public List<Operadora> listarTodas() {
        return operadoraDAO.listarTodas();
    }

    /**
     * Busca a operadora e recusa com mensagem clara quando ela nao
     * existe, para que a camada Resource devolva 404.
     */
    public Operadora buscarPorId(Long idOperadora) {
        Operadora operadora = operadoraDAO.buscarPorId(idOperadora);
        if (operadora == null) {
            throw new RecursoNaoEncontradoException(
                "Operadora nao encontrada. Id: " + idOperadora);
        }
        return operadora;
    }

    public Operadora buscarPorNome(String nome) {
        return operadoraDAO.buscarPorNome(nome);
    }

    // ------------------------------------------------------------
    // CRUD administrativo do catalogo
    // ------------------------------------------------------------

    /**
     * Cadastra uma operadora parceira.
     * O nome e unico no banco (UK_OPERADORA_NOME), entao a duplicidade
     * e verificada antes para devolver uma mensagem compreensivel em
     * vez do erro cru do Oracle.
     */
    public Operadora cadastrar(Operadora operadora) {
        validar(operadora);
        if (operadoraDAO.buscarPorNome(operadora.getNome()) != null) {
            throw new IllegalArgumentException(
                "Ja existe operadora com o nome " + operadora.getNome());
        }
        return operadoraDAO.inserir(operadora);
    }

    public Operadora atualizar(Long idOperadora, Operadora dados) {
        Operadora operadora = buscarPorId(idOperadora);
        validar(dados);

        Operadora donoDoNome = operadoraDAO.buscarPorNome(dados.getNome());
        if (donoDoNome != null && !donoDoNome.getId().equals(idOperadora)) {
            throw new IllegalArgumentException(
                "O nome " + dados.getNome() + " ja pertence a outra operadora");
        }

        operadora.setNome(dados.getNome());
        operadora.setCidade(dados.getCidade());
        operadora.setModal(dados.getModal());
        operadora.setValorPassagemCentavos(dados.getValorPassagemCentavos());
        operadora.setFatorCo2KgKm(dados.getFatorCo2KgKm());
        operadora.setFormatoCodigo(dados.getFormatoCodigo());
        operadora.setAtivo(dados.isAtivo());

        operadoraDAO.atualizar(operadora);
        return operadora;
    }

    /**
     * Remove a operadora do catalogo.
     *
     * Operadora que ja emitiu cupom ou foi favoritada nao pode ser
     * apagada: a chave estrangeira do banco impede, e esse e o
     * comportamento correto, porque preserva o historico. Para encerrar
     * uma parceria, o caminho e desativar (ativo = false), o que a
     * RN06 ja contempla.
     */
    public void remover(Long idOperadora) {
        buscarPorId(idOperadora);
        operadoraDAO.deletar(idOperadora);
    }

    private void validar(Operadora operadora) {
        if (operadora == null) {
            throw new IllegalArgumentException("Corpo da requisicao ausente");
        }
        if (operadora.getNome() == null || operadora.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome da operadora e obrigatorio");
        }
        if (operadora.getCidade() == null || operadora.getCidade().isBlank()) {
            throw new IllegalArgumentException("Cidade e obrigatoria");
        }
        if (operadora.getModal() == null) {
            throw new IllegalArgumentException(
                "Modal e obrigatorio. Valores aceitos: ONIBUS, METRO, TREM, MISTO, BRT");
        }
        if (operadora.getValorPassagemCentavos() <= 0) {
            throw new IllegalArgumentException(
                "Valor da passagem deve ser maior que zero (CK_OPERADORA_PASSAG)");
        }
        if (operadora.getFatorCo2KgKm() < 0) {
            throw new IllegalArgumentException(
                "Fator de CO2 nao pode ser negativo (CK_OPERADORA_CO2)");
        }
        if (operadora.getFormatoCodigo() == null || operadora.getFormatoCodigo().isBlank()) {
            throw new IllegalArgumentException(
                "Formato do codigo e obrigatorio. Exemplo: TOP-XXXX-XXXX");
        }
    }

    /**
     * Favorita uma operadora para o usuario, na proxima posicao
     * livre da lista de preferencias.
     */
    public UsuarioOperadoraFavorita favoritar(Usuario usuario, Operadora operadora) {
        int proximaOrdem = favoritaDAO.listarPorUsuario(usuario.getId()).size() + 1;
        return favoritaDAO.inserir(
            new UsuarioOperadoraFavorita(null, usuario, operadora, proximaOrdem));
    }

    /**
     * Operadoras do usuario na ordem de preferencia que ele definiu.
     * E essa ordem que a tela de resgate usa para posicionar os cards.
     */
    public List<Operadora> listarFavoritasNaOrdem(Usuario usuario) {
        List<Operadora> operadoras = new ArrayList<>();
        for (UsuarioOperadoraFavorita favorita
                : favoritaDAO.listarPorUsuario(usuario.getId())) {
            operadoras.add(favorita.getOperadora());
        }
        return operadoras;
    }

    public List<UsuarioOperadoraFavorita> listarFavoritas(Usuario usuario) {
        return favoritaDAO.listarPorUsuario(usuario.getId());
    }

    public boolean reordenarFavorita(Long idFavorita, int novaOrdem) {
        return favoritaDAO.atualizarOrdem(idFavorita, novaOrdem);
    }

    public boolean desfavoritar(Long idFavorita) {
        return favoritaDAO.deletar(idFavorita);
    }
}
