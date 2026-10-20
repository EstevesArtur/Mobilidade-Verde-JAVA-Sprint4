package br.com.fiap.mobilidadeverde;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * ================================================================
 * MOBILIDADE VERDE - API RESTful
 *
 * Challenge SoulUp 2026 - Sprint 4
 * Disciplina: Domain Driven Design using Java
 * Equipe FOG - Turma 1TDSPX-2026 - FIAP
 *
 *   Artur Bianchini Esteves            RM569450  (representante)
 *   Joao Vitor Oliveira Fontenele      RM570783
 *   Diego Barbosa Rodrigues            RM568829
 *   Vinicius Pacheco Ruiz              RM571109
 *   Yan de Almeida Cardoso             RM568814
 * ================================================================
 *
 * Classe de inicializacao da aplicacao Spring Boot.
 *
 * O DataSourceAutoConfiguration e excluido de proposito: a conexao
 * com o Oracle e gerenciada pela propria Connection Factory do
 * projeto (classe ConexaoOracle), conforme o padrao DAO trabalhado
 * em aula. Sem essa exclusao o Spring tentaria criar um DataSource
 * automatico e falharia na inicializacao por nao encontrar as
 * propriedades spring.datasource.
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class MobilidadeVerdeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MobilidadeVerdeApplication.class, args);
    }
}
