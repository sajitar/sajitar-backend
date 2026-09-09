# Imagem Docker de demonstração do Sajitar Backend.
#
# Responsabilidade: levantar o projeto rapidamente (sem rodar testes/validações
# no build) com uma massa de dados densa (ordem de milhares) pré-carregada no
# banco — exceto `checker`, que fica sempre vazio. PostgreSQL e a aplicação
# rodam juntos neste único container. Uso restrito a ambiente não produtivo
# (demo para o front-end); não usar em produção.

# ---- Estágio 1: build do jar, sem testes/validações (rápido) ----
FROM eclipse-temurin:26-jdk-noble AS build
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn/ .mvn/
RUN chmod +x mvnw && ./mvnw -q -B -Dmaven.test.skip=true dependency:go-offline
COPY src ./src
RUN ./mvnw -q -B -Dmaven.test.skip=true package

# ---- Estágio 2: runtime único (PostgreSQL + aplicação no mesmo container) ----
FROM eclipse-temurin:26-jre-noble AS runtime

# PostgreSQL 18.6 via o repositório oficial da PGDG (o Ubuntu Noble só oferece a
# 16 por padrão). Ajuste esta versão junto com docker-compose.yml e o workflow
# de CI para manter o projeto todo na mesma versão do PostgreSQL; para achar o
# candidato vigente depois que a 18.6 saia do repositório, rode
# `apt-cache madison postgresql-18` após habilitar o repositório PGDG abaixo.
ARG POSTGRESQL_PGDG_VERSION=18.6-1.pgdg24.04+2

RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        ca-certificates postgresql-common \
    && /usr/share/postgresql-common/pgdg/apt.postgresql.org.sh -y </dev/null \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        "postgresql-18=${POSTGRESQL_PGDG_VERSION}" \
        "postgresql-client-18=${POSTGRESQL_PGDG_VERSION}" \
    && rm -rf /var/lib/apt/lists/*

# Por padrão o pacote só ouve em localhost (dentro do container). Habilita ouvir
# em todas as interfaces e autenticar por senha via TCP, para que `-p 5432:5432`
# funcione (uso opcional, para inspecionar o banco com psql/pgAdmin externos).
RUN PG_VERSION=$(ls /etc/postgresql) \
    && pg_conftool "${PG_VERSION}" main set listen_addresses '*' \
    && printf '%s\n' \
        'host all all 0.0.0.0/0 scram-sha-256' \
        'host all all ::0/0 scram-sha-256' \
        >> /etc/postgresql/${PG_VERSION}/main/pg_hba.conf

# Senhas de demo ficam no entrypoint (docker run -e sobrescreve).
ENV POSTGRES_DB=sajitar-demo-db
ENV SPRING_DATASOURCE_URL="jdbc:postgresql://127.0.0.1:5432/sajitar-demo-db"
ENV SPRING_DATASOURCE_USERNAME=postgres
ENV SPRING_JPA_HIBERNATE_DDL_AUTO=create
ENV SPRING_JPA_SHOW_SQL=false
ENV SPRING_SQL_INIT_MODE=always
ENV SPRING_SQL_BEFORE_FRAMEWORK=classpath:util/functions.sql
ENV SPRING_SQL_AFTER_FRAMEWORK="util/columns.sql, util/uniques.sql, util/indexes.sql, demo/profile.sql, demo/authority.sql, demo/note.sql"
ENV SAJITAR_DOMAIN_VALIDATION_PROFILE_BIRTHDAY_MIN_AGE_YEARS=18
ENV SAJITAR_DOMAIN_VALIDATION_LIMIT_MAX=100

COPY --from=build /app/target/*.jar /app/app.jar
COPY docker/demo/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

EXPOSE 8080 5432
HEALTHCHECK --interval=5s --timeout=3s --start-period=45s --retries=12 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080' || exit 1

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
