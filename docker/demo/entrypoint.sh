#!/usr/bin/env bash
# Entrypoint da imagem Docker de demonstração: inicia o PostgreSQL (cluster já
# inicializado no build da imagem via pacote apt) e o Redis das sessões de
# `/tokens`, garante a senha/o banco da aplicação e sobe a aplicação Spring Boot
# em primeiro plano.
#
# Container efêmero: sem volume para /var/lib/postgresql nem para /var/lib/redis,
# então cada `docker run` começa limpo (idêntico ao gerado no build).
set -euo pipefail

# Defaults de demonstração em runtime — não vão para o Config.Env da imagem.
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-sajitar_demo}"
SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-${POSTGRES_PASSWORD}}"
# Precisa casar com docker/redis/users.acl, copiado para /etc/redis/users.acl.
SPRING_DATA_REDIS_PASSWORD="${SPRING_DATA_REDIS_PASSWORD:-sajitar_dev}"
export POSTGRES_PASSWORD SPRING_DATASOURCE_PASSWORD SPRING_DATA_REDIS_PASSWORD

PG_VERSION="$(pg_lsclusters -h | awk 'NR==1 {print $1}')"

echo "[demo] iniciando PostgreSQL ${PG_VERSION} (cluster main)..."
pg_ctlcluster "${PG_VERSION}" main start

echo "[demo] configurando role e banco da aplicação..."
su postgres -c "psql -v ON_ERROR_STOP=1 -c \"ALTER USER postgres PASSWORD '${POSTGRES_PASSWORD}';\"" >/dev/null

if ! su postgres -c "psql -tAc \"SELECT 1 FROM pg_database WHERE datname = '${POSTGRES_DB}'\"" | grep -q 1; then
    su postgres -c "createdb '${POSTGRES_DB}'"
fi

echo "[demo] iniciando Redis (sessões de /tokens)..."
redis-server /dev/null \
    --aclfile /etc/redis/users.acl \
    --dir /var/lib/redis \
    --maxmemory-policy noeviction \
    --appendonly yes \
    --daemonize yes

for _ in $(seq 1 30); do
    if redis-cli --user "${SPRING_DATA_REDIS_USERNAME}" --pass "${SPRING_DATA_REDIS_PASSWORD}" \
        --no-auth-warning ping 2>/dev/null | grep -q PONG; then
        break
    fi
    sleep 1
done

echo "[demo] PostgreSQL e Redis prontos; iniciando a aplicação Spring Boot..."
exec java -jar /app/app.jar
