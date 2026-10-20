package br.com.fiap.mobilidadeverde.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Configuracao de CORS da API.
 *
 * Sem este filtro o navegador bloqueia as requisicoes vindas do
 * front-end: a aplicacao React roda em um endereco (Vercel, ou
 * localhost:5173 em desenvolvimento) e a API roda em outro, e por
 * padrao o navegador recusa chamadas entre origens diferentes.
 *
 * A liberacao e ampla porque o front e publico e a API nao expoe
 * dados sensiveis nem exige autenticacao nesta etapa do projeto.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration configuracao = new CorsConfiguration();

        //Libera qualquer origem, necessario porque a URL da Vercel
        //muda a cada novo deploy de preview.
        configuracao.addAllowedOriginPattern("*");

        //Verbos HTTP usados pela API.
        configuracao.addAllowedMethod("*");

        //Cabecalhos livres, incluindo Content-Type das requisicoes JSON.
        configuracao.addAllowedHeader("*");

        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", configuracao);

        return new CorsFilter(fonte);
    }
}
