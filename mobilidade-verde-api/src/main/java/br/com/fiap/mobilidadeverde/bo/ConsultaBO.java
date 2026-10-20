package br.com.fiap.mobilidadeverde.bo;

import org.springframework.stereotype.Service;
import br.com.fiap.mobilidadeverde.dao.CarteiraDAO;
import br.com.fiap.mobilidadeverde.dto.ResumoUsuarioDTO;
import br.com.fiap.mobilidadeverde.model.Carteira;
import br.com.fiap.mobilidadeverde.model.Usuario;

/**
* BO de consultas sobre o usuario.
 *
 * Orquestra Usuario e Carteira para responder perguntas simples
 * como "qual e o meu saldo de Pontos ECOA". Na Sprint 4 este
 * service alimenta o endpoint GET /usuarios/{id} consumido pela
 * tela de carteira do front.
 */
@Service
public class ConsultaBO {

    private final CarteiraDAO carteiraDAO;

    public ConsultaBO(CarteiraDAO carteiraDAO) {
        this.carteiraDAO = carteiraDAO;
    }

    /**
     * Le o saldo direto do banco, e nao de um objeto em memoria.
     * Isso garante que o valor exibido reflita qualquer alteracao
     * feita por outra operacao.
     */
    public int consultarSaldo(Usuario usuario) {
        Carteira carteira = carteiraDAO.buscarPorUsuario(usuario);
        if (carteira == null) {
            throw new IllegalStateException(
                "Usuario sem carteira: " + usuario.getEmail() + " (RN01)");
        }
        return carteira.getSaldoPontos();
    }

    public ResumoUsuarioDTO resumo(Usuario usuario) {
        return new ResumoUsuarioDTO(usuario.getNome(), consultarSaldo(usuario));
    }
}
