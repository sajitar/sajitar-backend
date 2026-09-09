#!/usr/bin/env bash
# Entrypoint da imagem Docker de demonstração: inicia o PostgreSQL (cluster já
# inicializado no build da imagem via pacote apt), garante a senha/o banco da
# aplicação e sobe a aplicação Spring Boot em primeiro plano.
#
# Container efêmero: sem volume para /var/lib/postgresql, então cada
# `docker run` começa de um cluster limpo (idêntico ao gerado no build).
set -euo pipefail

# Defaults de demonstração em runtime — não vão para o Config.Env da imagem.
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-sajitar_demo}"
SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-${POSTGRES_PASSWORD}}"
export POSTGRES_PASSWORD SPRING_DATASOURCE_PASSWORD

PG_VERSION="$(pg_lsclusters -h | awk 'NR==1 {print $1}')"

echo "[demo] iniciando PostgreSQL ${PG_VERSION} (cluster main)..."
pg_ctlcluster "${PG_VERSION}" main start

echo "[demo] configurando role e banco da aplicação..."
su postgres -c "psql -v ON_ERROR_STOP=1 -c \"ALTER USER postgres PASSWORD '${POSTGRES_PASSWORD}';\"" >/dev/null

if ! su postgres -c "psql -tAc \"SELECT 1 FROM pg_database WHERE datname = '${POSTGRES_DB}'\"" | grep -q 1; then
    su postgres -c "createdb '${POSTGRES_DB}'"
fi

echo "[demo] PostgreSQL pronto; iniciando a aplicação Spring Boot..."
exec java -jar /app/app.jar
