#!/usr/bin/env bash
# Entrypoint da imagem Docker de demonstração: sobe o Mailpit (caixa dos e-mails
# simulados), inicia o PostgreSQL (cluster já inicializado no build da imagem via
# pacote apt) e o Redis das sessões de `/tokens`, garante a senha/o banco da
# aplicação e sobe a aplicação Spring Boot (perfil DEMO) em primeiro plano.
#
# Container efêmero: sem volume para /var/lib/postgresql nem para /var/lib/redis,
# então cada `docker run` começa limpo (idêntico ao gerado no build). A caixa do
# Mailpit fica em memória e também começa vazia.
set -euo pipefail

# Precisam casar com src/main/resources/application-DEMO.yml e com
# docker/redis/users.acl, copiado para /etc/redis/users.acl.
readonly POSTGRES_DB="sajitar-demo-db"
readonly POSTGRES_PASSWORD="sajitar_demo"
readonly REDIS_USERNAME="sajitar"
readonly REDIS_PASSWORD="sajitar_dev"

echo "[demo] iniciando Mailpit (SMTP 127.0.0.1:1025, caixa em :8025)..."
mailpit --smtp 127.0.0.1:1025 --listen 0.0.0.0:8025 --max 5000 --quiet &

for _ in $(seq 1 30); do
    if (exec 3<>/dev/tcp/127.0.0.1/1025) 2>/dev/null; then
        break
    fi
    sleep 1
done

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
    if redis-cli --user "${REDIS_USERNAME}" --pass "${REDIS_PASSWORD}" \
        --no-auth-warning ping 2>/dev/null | grep -q PONG; then
        break
    fi
    sleep 1
done

echo "[demo] PostgreSQL, Redis e Mailpit prontos; iniciando a aplicação Spring Boot..."
exec java -jar /app/app.jar
