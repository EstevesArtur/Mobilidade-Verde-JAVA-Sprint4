package br.com.fiap.mobilidadeverde.bo;

import org.springframework.stereotype.Service;
import br.com.fiap.mobilidadeverde.dao.ViagemDAO;
import br.com.fiap.mobilidadeverde.dto.ImpactoDTO;
import br.com.fiap.mobilidadeverde.model.Usuario;

/**
 * ================================================================
 * SERVICE DE IMPACTO AMBIENTAL
 *
 * Concentra o metodo de logica de negocio 4 da entrega.
 *
 * Responde a pergunta do desafio: como mensurar o impacto
 * ambiental de forma auditavel.
 *
 * MUDANCA DA SPRINT 1+2 PARA A SPRINT 3. A soma deixou de percorrer
 * uma lista em memoria e passou a ser feita pelo banco com SUM e
 * COUNT. Com centenas de milhares de usuarios, trazer todas as
 * viagens para a JVM apenas para somar uma coluna seria desperdicio
 * de memoria e de rede.
 * ================================================================
 */
@Service
public class ImpactoBO {

    /** Absorcao media anual de CO2 de uma arvore, em kg (media tropical). */
    private static final double ABSORCAO_ARVORE_KG_ANO = 21.77;

    /** Dias no ano, usado para converter a absorcao anual em diaria. */
    private static final double DIAS_NO_ANO = 365.0;

    /** Emissao media de um carro individual urbano, em kg de CO2 por km (MMA). */
    private static final double FATOR_CO2_CARRO_KG_KM = 0.180;

    private final ViagemDAO viagemDAO;

    public ImpactoBO(ViagemDAO viagemDAO) {
        this.viagemDAO = viagemDAO;
    }

    // ================================================================
    // METODO DE NEGOCIO 4 - IMPACTO AMBIENTAL ACUMULADO
    // ================================================================

    /**
     * Calcula o impacto ambiental acumulado do usuario e o traduz em
     * equivalencias que fazem sentido para uma pessoa comum.
     *
     * A agregacao (quantidade de viagens, soma de km e soma de CO2)
     * vem pronta do banco numa unica consulta. A aplicacao apenas
     * converte para as unidades de apresentacao:
     *
     *  - arvoresAno: quantas arvores levariam um ano para absorver
     *    esse CO2, usando 21,77 kg por arvore ao ano.
     *  - arvoresDia: quantas arvores absorveriam esse CO2 em um
     *    unico dia. E a metrica usada na tela do cupom no front.
     *  - kmCarroEvitado: quantos quilometros de carro individual
     *    produziriam a mesma emissao.
     *
     * @param usuario usuario consultado
     * @return DTO pronto para a tela "Meu impacto"
     */
    public ImpactoDTO calcularImpactoAcumulado(Usuario usuario) {
        double[] totais = viagemDAO.totalizarPorUsuario(usuario.getId());

        int qtdViagens = (int) totais[0];
        double kmEstimado = totais[1];
        double co2EvitadoKg = totais[2] / 1000.0;

        double arvoresAno = co2EvitadoKg / ABSORCAO_ARVORE_KG_ANO;
        double arvoresDia = co2EvitadoKg / (ABSORCAO_ARVORE_KG_ANO / DIAS_NO_ANO);
        double kmCarroEvitado = co2EvitadoKg / FATOR_CO2_CARRO_KG_KM;

        return new ImpactoDTO(qtdViagens, kmEstimado, co2EvitadoKg,
            arvoresAno, arvoresDia, kmCarroEvitado);
    }
}
