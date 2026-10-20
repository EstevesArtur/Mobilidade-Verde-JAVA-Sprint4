# Mobilidade Verde — API Restful

**Challenge SoulUp 2026 · FIAP**
**Disciplina:** Domain Driven Design using Java — **Sprint 4**
**Equipe:** FOG · **Turma:** 1TDSPX-2026 · 1º ano de Análise e Desenvolvimento de Sistemas

API Restful em Spring Boot que expõe o domínio do **Mobilidade Verde**: a feature da plataforma SoulUp que converte **Pontos ECOA** em cupons de transporte público.

---

## Integrantes

| Nome | RM | Função |
|---|---|---|
| **Artur Bianchini Esteves** | **RM569450** | **Representante** |
| João Vitor Oliveira Fontenele Martins | RM570783 | — |
| Diego Barbosa Rodrigues | RM568829 | — |
| Vinicius Pacheco Ruiz | RM571109 | — |
| Yan de Almeida Cardoso | RM568814 | — |

---

## Links

| Recurso | URL |
|---|---|
| Repositório GitHub | _preencher após criar o repositório_ |
| API publicada (Render) | _preencher após o deploy_ |
| Front-end (Vercel) | _preencher após o deploy do front_ |

Depois do deploy, confira a API em `/api/status`. Esse endpoint diz se a aplicação subiu **e** se o Oracle está alcançável.

> **Primeiro acesso demora.** No plano gratuito do Render o serviço hiberna após 15 minutos sem uso. A primeira requisição depois disso leva cerca de 50 segundos para responder, porque o contêiner precisa subir de novo. Não é erro: basta aguardar.

---

## Tecnologias

| Tecnologia | Versão | Papel |
|---|---|---|
| Java | 17 | Linguagem |
| Spring Boot | 3.3.5 | Framework da API Restful |
| Maven | 3.9 | Build e dependências |
| Oracle JDBC (ojdbc11) | 23.5 | Driver de conexão |
| Oracle Database | 19c | Banco de dados (servidor da FIAP) |

Sem JPA e sem Hibernate: a persistência usa **JDBC com camada DAO e Connection Factory**, conforme os padrões trabalhados em aula.

---

## Estrutura de pastas

```
mobilidade-verde-api/
├── pom.xml                      dependências e build
├── render.yaml                  configuração do deploy
├── system.properties            versão do Java no servidor
├── mvnw / mvnw.cmd              Maven Wrapper
└── src/main/
    ├── resources/
    │   └── application.properties
    └── java/br/com/fiap/mobilidadeverde/
        ├── MobilidadeVerdeApplication.java   classe de inicialização
        ├── config/      CorsConfig                    (1 classe)
        ├── conexao/     ConexaoOracle                 (1 classe)
        ├── model/       8 entidades + 4 enums        (12 classes)
        ├── dao/         8 DAOs + helper + exceção    (10 classes)
        ├── bo/          regras de negócio             (7 classes)
        ├── dto/         objetos de entrada e saída   (12 classes)
        ├── resource/    controllers REST              (5 classes)
        ├── exception/   tratamento de erros           (2 classes)
        └── util/        hash, gerador, validadores    (3 classes)
```

**54 classes em 10 pacotes.**

---

## Tabela de endpoints

Prefixo: `/api`

### Operadoras

| Verbo | URI | Descrição | Status |
|---|---|---|---|
| GET | `/api/operadoras` | Lista as operadoras ativas | 200 |
| GET | `/api/operadoras/todas` | Inclui as desativadas | 200 |
| GET | `/api/operadoras/desempenho` | Relatório de aproveitamento | 200 |
| GET | `/api/operadoras/{id}` | Detalhe de uma operadora | 200, 404 |
| POST | `/api/operadoras` | Cadastra uma parceira | 201, 400 |
| PUT | `/api/operadoras/{id}` | Atualiza os dados | 200, 400, 404 |
| DELETE | `/api/operadoras/{id}` | Remove do catálogo | 204, 404, 500 |

### Faixas de cupom

| Verbo | URI | Descrição | Status |
|---|---|---|---|
| GET | `/api/faixas` | Lista as faixas ativas | 200 |
| GET | `/api/faixas/todas` | Inclui as cotações históricas | 200 |
| GET | `/api/faixas/{id}` | Detalhe de uma faixa | 200, 404 |

### Usuários

| Verbo | URI | Descrição | Status |
|---|---|---|---|
| GET | `/api/usuarios` | Lista os usuários | 200 |
| GET | `/api/usuarios/ranking-impacto` | Ranking de CO₂ evitado | 200 |
| GET | `/api/usuarios/{id}` | Detalhe com saldo de pontos | 200, 404 |
| POST | `/api/usuarios` | Cadastra um usuário | 201, 400 |
| PUT | `/api/usuarios/{id}` | Atualiza os dados | 200, 400, 404 |
| DELETE | `/api/usuarios/{id}` | Remove com as dependências | 204, 404 |
| GET | `/api/usuarios/{id}/vouchers` | Cupons do usuário | 200, 404 |
| GET | `/api/usuarios/{id}/transacoes` | Extrato de pontos | 200, 404 |
| GET | `/api/usuarios/{id}/impacto` | Impacto ambiental acumulado | 200, 404 |
| GET | `/api/usuarios/{id}/favoritas` | Operadoras favoritas (N:N) | 200, 404 |
| POST | `/api/usuarios/{id}/favoritas` | Favorita uma operadora | 201, 404, 500 |
| DELETE | `/api/usuarios/{id}/favoritas/{idFavorita}` | Desfavorita | 204, 404 |

### Vouchers

| Verbo | URI | Descrição | Status |
|---|---|---|---|
| POST | `/api/vouchers` | Resgata pontos em cupom | 201, 400, 404, 409 |
| GET | `/api/vouchers/{codigo}` | Consulta um cupom | 200, 404 |
| POST | `/api/vouchers/{codigo}/utilizar` | Valida o cupom no embarque | 200, 404, 409 |
| POST | `/api/vouchers/{codigo}/estornar` | Cancela e devolve os pontos | 200, 404, 409 |
| POST | `/api/vouchers/expirar-vencidos` | Rotina de expiração em lote | 200 |

### Status

| Verbo | URI | Descrição | Status |
|---|---|---|---|
| GET | `/` | Identificação da API | 200 |
| GET | `/api/status` | Estado da aplicação e do banco | 200, 503 |

### Significado dos códigos

| Código | Quando acontece |
|---|---|
| 200 OK | Consulta ou atualização bem-sucedida |
| 201 Created | Recurso criado |
| 204 No Content | Remoção bem-sucedida |
| 400 Bad Request | Dado inválido enviado pelo cliente |
| 404 Not Found | O recurso pedido não existe |
| 409 Conflict | Regra de negócio impede a operação (saldo insuficiente, limite antifraude, cupom já utilizado ou expirado) |
| 500 Internal Server Error | Falha de banco, incluindo violação de chave estrangeira |
| 503 Service Unavailable | A API está no ar, mas o banco não está alcançável |

---

## Como executar localmente

### Pré-requisitos

- **JDK 17 ou superior**
- As 8 tabelas criadas no schema Oracle, com os dados carregados — scripts `mobilidade_verde_ddl.sql` e `mobilidade_verde_carga.sql`, da disciplina de Building Relational Database
- Acesso de rede ao `oracle.fiap.com.br:1521`

Maven não precisa estar instalado: o projeto traz o Maven Wrapper.

### Passo a passo

```bash
# 1. Clonar o repositório
git clone <url-do-repositorio>
cd mobilidade-verde-api

# 2. Compilar e empacotar
./mvnw clean package -DskipTests

# 3. Executar
java -jar target/mobilidade-verde-api-1.0.0.jar
```

No Windows, use `mvnw.cmd` no lugar de `./mvnw`.

A API sobe em `http://localhost:8080`. Para conferir:

```bash
curl http://localhost:8080/api/status
```

### Importar no IntelliJ IDEA

1. **File > Open** e selecione o arquivo `pom.xml`, escolhendo *Open as Project*.
2. O Maven baixa as dependências automaticamente.
3. Execute a classe `MobilidadeVerdeApplication`.

### Credenciais

Por padrão a aplicação conecta no schema `RM569450`. Para usar outro, defina as variáveis de ambiente antes de executar:

```bash
export ORACLE_URL="jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL"
export ORACLE_USER="RM000000"
export ORACLE_PASSWORD="sua-senha"
```

No Windows, troque `export` por `set`.

---

## Deploy no Render

1. Suba o projeto em um repositório **público** no GitHub.
2. No Render, clique em **New > Web Service** e conecte o repositório.
3. Configure:
   - **Language:** Java
   - **Build Command:** `./mvnw clean package -DskipTests`
   - **Start Command:** `java -jar target/mobilidade-verde-api-1.0.0.jar`
   - **Instance Type:** Free
4. Em **Environment**, cadastre as três variáveis:

| Variável | Valor |
|---|---|
| `ORACLE_URL` | `jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL` |
| `ORACLE_USER` | o RM, em maiúsculo |
| `ORACLE_PASSWORD` | a senha do aluno |

5. Clique em **Create Web Service** e aguarde o build.
6. Abra `https://<sua-url>.onrender.com/api/status` e confirme que `"banco": "conectado"`.

Se vier `"banco": "indisponivel"`, a aplicação subiu mas não alcança o Oracle — verifique as variáveis de ambiente e se o servidor da FIAP aceita conexões externas.

---

## Exemplos de uso

```bash
# Listar operadoras ativas
curl https://<sua-url>.onrender.com/api/operadoras

# Consultar um usuário com o saldo
curl https://<sua-url>.onrender.com/api/usuarios/1

# Resgatar pontos em cupom
curl -X POST https://<sua-url>.onrender.com/api/vouchers \
  -H "Content-Type: application/json" \
  -d '{"idUsuario":1,"idOperadora":1,"idFaixa":2}'

# Usar o cupom no embarque
curl -X POST https://<sua-url>.onrender.com/api/vouchers/TOP-A1B2-C3D4/utilizar \
  -H "Content-Type: application/json" \
  -d '{"idOperadora":1}'
```

---

## Regras de negócio

| RN | Regra | Onde é garantida |
|---|---|---|
| RN01 | Carteira única e obrigatória por usuário | `UK_CARTEIRA_USUARIO` + `UsuarioBO.cadastrar` |
| RN02 | Saldo nunca negativo | `CK_CARTEIRA_SALDO` + `Carteira.debitar()` |
| RN03 | Cupom apenas em faixa fixa e ativa | `FK_VOUCHER_FAIXA` + `ConversorBO` |
| RN04 | Cupom de uso único e com expiração | `CK_VOUCHER_STATUS` + `CupomBO` |
| RN05 | Movimentação auditada e imutável | `TransacaoDAO` sem update nem delete de negócio |
| RN06 | Cupom só para operadora ativa | `CK_OPERADORA_ATIVO` + `ConversorBO` |
| RN07 | Máximo de 5 cupons simultâneos em GERADO | `AntiFraudeBO` + `IDX_VOUCHER_USR_STAT` |

---

## Parâmetros oficiais do projeto

- **Cotação:** 100 Pontos ECOA = R$ 1,00
- **Faixas ativas:** R$ 2 = 200 pts · R$ 5 = 500 pts · R$ 10 = 1.000 pts · R$ 20 = 2.000 pts
- **Validade do cupom:** 24 horas
- **CO₂:** carro 0,180 kg/km (MMA) · ônibus 0,082 kg/km (ANTP) · evitado 0,098 kg/km
- **Km médio por passagem em São Paulo:** 7 km
- **Absorção de uma árvore:** 21,77 kg de CO₂ por ano

---

## Contato

**Equipe FOG** — Turma 1TDSPX-2026 — FIAP
Representante: Artur Bianchini Esteves (RM569450)
