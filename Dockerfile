# ============================================================
# Mobilidade Verde - API Restful
# Challenge SoulUp 2026 - Sprint 4 - Equipe FOG - 1TDSPX-2026
# ------------------------------------------------------------
# O Render nao oferece runtime Java nativo: a propria documentacao
# da plataforma manda usar Docker para aplicacoes JVM. Este arquivo
# descreve a imagem que o servidor constroi e executa.
#
# Ele fica na RAIZ do repositorio, e nao dentro de
# mobilidade-verde-api/, porque o Render resolve o caminho do
# Dockerfile e o contexto de build a partir da raiz do repositorio.
# Deixando o arquivo aqui, nenhum campo de caminho precisa ser
# preenchido no painel: tudo fica no valor padrao.
#
# Sao dois estagios. O primeiro tem o JDK e o Maven e serve apenas
# para compilar o projeto. O segundo copia somente o .jar pronto
# para uma imagem que tem apenas a JRE: a imagem final fica bem
# menor e nao carrega as ferramentas de build.
#
# Para construir manualmente, a partir desta pasta:
#   docker build -t mobilidade-verde-api .
#   docker run -p 8080:8080 mobilidade-verde-api
# ============================================================

# ------------------------------------------------------------
# Estagio 1: compilacao
# ------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /build

COPY mobilidade-verde-api/pom.xml ./
COPY mobilidade-verde-api/src ./src

RUN mvn -B clean package -DskipTests

# ------------------------------------------------------------
# Estagio 2: execucao
# ------------------------------------------------------------
FROM eclipse-temurin:17-jre-noble

WORKDIR /app

COPY --from=build /build/target/mobilidade-verde-api-1.0.0.jar app.jar

# A aplicacao nao roda como root. Se a API for comprometida, o
# processo nao tem privilegio para alterar o sistema de arquivos
# do container.
USER 1000:1000

# A porta real vem da variavel PORT, definida pelo servidor de
# deploy. O application.properties ja faz server.port=${PORT:8080},
# entao localmente a aplicacao continua subindo na 8080.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
