# Comandos relevantes

Na raiz do repositório. Variáveis de ambiente vêm do `local.env` — arquivo **versionado**, com credenciais apenas de desenvolvimento local (sem segredos de produção). O sufixo `local` deixa explícito que não é um `.env` privado/gitignored.

## Docker Compose

| Objetivo | Comando |
| --- | --- |
| Subir Postgres, Redis, pgAdmin, RedisInsight e o container da aplicação (JDK montado em `/app`) | `env -i docker compose --env-file local.env up -d` |
| Recriar imagens/containers após mudanças no `docker-compose.yml` | `env -i docker compose --env-file local.env up -d --build` |
| Parar e remover containers da stack (mesmo padrão “ambiente limpo” do `up`) | `env -i docker compose --env-file local.env down` |
| Ver logs em tempo real (todos os serviços) | `docker compose --env-file local.env logs -f` |
| Logs só do Postgres, do Redis, do RedisInsight ou do container Java | `docker compose --env-file local.env logs -f postgres`, `... logs -f redis`, `... logs -f redisinsight` ou `... logs -f springboot` |
| Listar containers da stack | `docker compose --env-file local.env ps` |

### Shell no container da aplicação (Temurin 26, código em `/app`)

```bash
docker exec -it sajitar-springboot bash
```

Dentro do container, o Compose já injeta `SPRING_DATASOURCE_*` apontando para o Postgres da rede interna; a partir de `/app` você pode usar Maven, por exemplo `./mvnw spring-boot:run`.

### Cliente `psql` no Postgres

Com variáveis do `local.env` carregadas no shell atual:

```bash
set -a && source local.env && set +a
docker exec -it sajitar-postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"
```

### Cliente `redis-cli` nas sessões de `/tokens`

Instância dedicada às sessões (AUTH e ACL de [docker/redis/users.acl](../../docker/redis/users.acl); o usuário `default` fica desligado e o `sajitar` só enxerga os prefixos `token:`, `tomb:`, `session:` e `profile:`):

```bash
set -a && source local.env && set +a
docker exec -it sajitar-redis redis-cli --user "$SPRING_DATA_REDIS_USERNAME" --pass "$SPRING_DATA_REDIS_PASSWORD" --no-auth-warning
```

Dentro do cliente, `SCAN 0 MATCH session:*` lista as sessões ativas (`KEYS` e `FLUSHDB` são negados pela ACL).

### pgAdmin

Interface em [http://localhost:15432](http://localhost:15432) (credenciais conforme `PGADMIN_*` no `local.env`).

### RedisInsight

Interface em [http://localhost:16379](http://localhost:16379). Cadastre o Redis da stack: host `10.0.0.25` (ou `sajitar-redis`), porta `6379`, usuário e senha conforme `SPRING_DATA_REDIS_*` no `local.env`, sem TLS.

## Imagem Docker de demonstração

Imagem única e autocontida (PostgreSQL + aplicação no mesmo container), pensada para o **front-end consumir em ambiente não produtivo**. Build sem rodar testes/validações (`-Dmaven.test.skip=true`, sem chegar à fase `verify`) e sobe já com uma massa de dados **densa** (ordem de milhares) pré-carregada: ~5.000 `profile`, ~7.500 `authority` e ~7.500 `note`. A massa de `checker` **não** é carregada (tabela sempre vazia). Definições em [docker/demo/Dockerfile](../../docker/demo/Dockerfile) e [docker/demo/entrypoint.sh](../../docker/demo/entrypoint.sh); seeds em [src/main/resources/demo](../../src/main/resources/demo).

| Objetivo | Comando |
| --- | --- |
| Buildar a imagem | `docker build -f docker/demo/Dockerfile -t sajitar-backend:demo .` |
| Rodar (API em `:8080`) | `docker run --rm -p 8080:8080 sajitar-backend:demo` |
| Rodar expondo também o Postgres interno (cliente `psql`/pgAdmin externo) | `docker run --rm -p 8080:8080 -p 5432:5432 sajitar-backend:demo` |
| Rodar expondo também o Redis interno (cliente `redis-cli`/RedisInsight externo) | `docker run --rm -p 8080:8080 -p 6379:6379 sajitar-backend:demo` |

Observações:
- **Efêmera**: não há volume para o Postgres; cada `docker run` novo começa com a massa de dados recriada do zero (mesmo conteúdo, IDs diferentes). Para "resetar" os dados, remova o container e crie um novo.
- **Credenciais fixas de demonstração** (não são segredos de produção): usuário `postgres`, senha `sajitar_demo`, banco `sajitar-demo-db` — só valem dentro do próprio container.
- Mesmas URLs úteis da [seção abaixo](#urls-úteis-app-na-porta-8080) (Swagger UI, OpenAPI, Actuator), já que a API sobe na mesma porta `8080`.
- Se a stack de desenvolvimento (`docker-compose.yml`) já estiver usando a porta `8080` do host, pare-a antes ou publique a demo em outra porta (`-p 18080:8080`, por exemplo).

## Maven

Use `./mvnw` na raiz para reproduzir o mesmo comando do CI; `mvn` também funciona se o Maven estiver instalado no host.

| Objetivo | Comando |
| --- | --- |
| Compilar sem rodar testes | `./mvnw -q -DskipTests compile` |
| Rodar testes | `./mvnw test` |
| Testes + relatório JaCoCo + verificação de cobertura (`verify`) | `./mvnw verify` |
| Limpar artefatos e compilar de novo | `./mvnw clean compile` |
| Um teste por classe ou método | `./mvnw -Dtest=NomeDaClasseTest test` ou `./mvnw -Dtest=NomeDaClasseTest#nomeDoMetodo test` |

Relatório HTML do JaCoCo (após `./mvnw verify`): `target/site/jacoco/index.html`.

### Testes e cobertura (host)

Os testes com `@SpringBootTest` exigem **PostgreSQL** e **Redis** acessíveis e as variáveis que `src/main/resources/application.yml` resolve em tempo de execução (`SPRING_DATASOURCE_*`, `SPRING_JPA_*`, `SPRING_SQL_*`, `SPRING_DATA_REDIS_*`, `SAJITAR_DOMAIN_VALIDATION_*`, `SAJITAR_SECURITY_JWT_*`). A configuração complementar de teste fica em `src/test/resources/application.yml` (sem perfil Spring `test` separado). Sem Redis, os testes de `/tokens` e das rotas protegidas falham: a aplicação não emite nem valida token sem o store de sessões.

**Opção A — alinhar ao CI** (Postgres em `localhost:5432`, base/usuário/senha `sajitar_ci`; Redis em `localhost:6379` com a ACL do repositório; mesmas variáveis de [`.github/scripts/ci.env`](../../.github/scripts/ci.env)):

```bash
docker run --rm -d --name sajitar-redis-ci -p 6379:6379 \
  -v "$PWD/docker/redis/users.acl:/etc/redis/users.acl:ro" \
  redis:8.2.9-alpine \
  redis-server --aclfile /etc/redis/users.acl --maxmemory-policy noeviction --appendonly yes
set -a && source .github/scripts/ci.env && set +a
./mvnw verify
```

Com o Compose do projeto no ar, o serviço `redis` já publica a mesma porta — nesse caso pule o `docker run`.

**Opção B — Docker Compose no ar** (variáveis derivadas do `local.env`, como em `spring-boot:run`):

```bash
set -a && source local.env && set +a
export SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-$POSTGRES_USER}"
export SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-$POSTGRES_PASSWORD}"
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/${POSTGRES_DB}"
export SPRING_JPA_HIBERNATE_DDL_AUTO="${SPRING_JPA_HIBERNATE_DDL_AUTO:-create-drop}"
export SPRING_JPA_SHOW_SQL="${SPRING_JPA_SHOW_SQL:-false}"
export SPRING_SQL_INIT_MODE="${SPRING_SQL_INIT_MODE:-always}"
export SPRING_SQL_BEFORE_FRAMEWORK="${SPRING_SQL_BEFORE_FRAMEWORK:-classpath:util/functions.sql}"
export SPRING_SQL_AFTER_FRAMEWORK="${SPRING_SQL_AFTER_FRAMEWORK:-util/columns.sql, util/uniques.sql, util/indexes.sql, settlement/profile.sql, settlement/checker.sql, settlement/authority.sql, settlement/note.sql}"
export SAJITAR_DOMAIN_VALIDATION_PROFILE_BIRTHDAY_MIN_AGE_YEARS="${SAJITAR_DOMAIN_VALIDATION_PROFILE_BIRTHDAY_MIN_AGE_YEARS:-18}"
export SAJITAR_DOMAIN_VALIDATION_LIMIT_MAX="${SAJITAR_DOMAIN_VALIDATION_LIMIT_MAX:-100}"
export SAJITAR_SECURITY_JWT_SECRET="${SAJITAR_SECURITY_JWT_SECRET:-01234567890123456789012345678901}"
export SAJITAR_SECURITY_JWT_EXPIRATION_SECONDS="${SAJITAR_SECURITY_JWT_EXPIRATION_SECONDS:-3600}"
export SAJITAR_SECURITY_JWT_REFRESH_EXPIRATION_SECONDS="${SAJITAR_SECURITY_JWT_REFRESH_EXPIRATION_SECONDS:-604800}"
export SPRING_DATA_REDIS_HOST="${SPRING_DATA_REDIS_HOST:-127.0.0.1}"
export SPRING_DATA_REDIS_PORT="${SPRING_DATA_REDIS_PORT:-6379}"
./mvnw verify
```

### Rodar a API na máquina host

Exige Postgres e Redis acessíveis (por exemplo `localhost:5432` e `localhost:6379` com o compose no ar) e as mesmas variáveis que o Spring lê em `application.yml` (`SPRING_DATASOURCE_*`, `SPRING_JPA_*`, `SPRING_SQL_*`, `SPRING_DATA_REDIS_*`, etc.), tipicamente exportadas a partir do `local.env`:

```bash
set -a && source local.env && set +a
# No host, use o Postgres e o Redis expostos em localhost e alinhe nomes ao application.yml:
export SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-$POSTGRES_USER}"
export SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-$POSTGRES_PASSWORD}"
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/${POSTGRES_DB}"
export SPRING_DATA_REDIS_HOST="${SPRING_DATA_REDIS_HOST:-127.0.0.1}"
export SPRING_DATA_REDIS_PORT="${SPRING_DATA_REDIS_PORT:-6379}"
./mvnw spring-boot:run
```

## URLs úteis (app na porta 8080)

| Recurso | Endereço |
| --- | --- |
| OpenAPI (JSON) | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) |
| Swagger UI | [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) |
| Collection Postman | [sajitar.postman_collection.json](../api/sajitar.postman_collection.json) (`/tokens`, `/profiles`, `/checkers`, `/authorities` e `/notes`; Import no Postman) |
| Actuator | [http://localhost:8080/actuator](http://localhost:8080/actuator) (endpoints expostos dependem da configuração) |
