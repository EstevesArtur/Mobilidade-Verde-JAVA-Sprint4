package br.com.fiap.mobilidadeverde.resource;

import br.com.fiap.mobilidadeverde.conexao.ConexaoOracle;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ================================================================
 * RECURSO: STATUS DA API
 *
 * Endpoint de verificacao usado para confirmar, apos o deploy, que a
 * aplicacao subiu e que o banco esta alcancavel a partir do servidor
 * na nuvem. E a primeira coisa a chamar quando algo parece errado.
 *
 * GET /              identificacao da API
 * GET /api/status    estado da aplicacao e da conexao com o banco
 * ================================================================
 */
@RestController
public class StatusResource {

    /**
     * Raiz da aplicacao. Evita que o avaliador receba um 404 ao abrir a
     * URL do deploy sem caminho.
     *
     * @return 200 com a identificacao da API
     */
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> raiz() {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("api", "Mobilidade Verde - API Restful");
        corpo.put("solucao", "Mobilidade Verde");
        corpo.put("equipe", "FOG");
        corpo.put("turma", "1TDSPX-2026");
        corpo.put("disciplina", "Domain Driven Design using Java");
        corpo.put("sprint", 4);
        corpo.put("documentacao", "/api/status para verificar a conexao com o banco");
        return ResponseEntity.ok(corpo);
    }

    /**
     * Verifica a conexao com o Oracle.
     *
     * @return 200 com a versao do banco quando a conexao funciona,
     *         503 quando o banco nao esta alcancavel
     */
    @GetMapping("/api/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("aplicacao", "no ar");
        corpo.put("url", ConexaoOracle.getUrl());
        corpo.put("usuario", ConexaoOracle.getUsuario());

        try {
            corpo.put("banco", "conectado");
            corpo.put("detalhe", ConexaoOracle.descreverAmbiente());
            return ResponseEntity.ok(corpo);

        } catch (RuntimeException e) {
            corpo.put("banco", "indisponivel");
            corpo.put("detalhe", e.getMessage());
            return ResponseEntity.status(503).body(corpo);
        }
    }
}
