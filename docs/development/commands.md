# Comandos

Execute os comandos na raiz do repositório.
As variáveis vêm de `local.env`.
O arquivo `local.env` está versionado.
As credenciais desse arquivo valem só para o desenvolvimento local.
O sufixo `local` marca que o arquivo não é um `.env` privado.

## Docker Compose

| Objetivo | Comando |
| --- | --- |
| Subir Postgres, Redis, pgAdmin, RedisInsight, Mailpit e o container JDK | `env -i docker compose --env-file local.env up -d` |
| Recriar imagens e containers depois de mudar `docker-compose.yml` | `env -i docker compose --env-file local.env up -d --build` |
| Parar e remover os containers. O volume `redis-data` permanece | `env -i docker compose --env-file local.env down` |
| Ver os logs de todos os serviços | `docker compose --env-file local.env logs -f` |
| Listar os containers da stack | `docker compose --env-file local.env ps` |

Logs de um serviço:

```bash
docker compose --env-file local.env logs -f postgres
docker compose --env-file local.env logs -f redis
docker compose --env-file local.env logs -f redisinsight
docker compose --env-file local.env logs -f mailpit
docker compose --env-file local.env logs -f springboot
```

Aviso: o comando abaixo apaga o volume `redis-data` e revoga as sessões de `/tokens`.

```bash
env -i docker compose --env-file local.env down -v
```

### Shell no container JDK

O container `sajitar-springboot` usa Temurin 25.
O repositório fica montado em `/app`.
O Compose injeta `SPRING_DATASOURCE_*` para o Postgres da rede interna.

```bash
docker exec -it sajitar-springboot bash
```

Dentro de `/app`, rode por exemplo `./mvnw spring-boot:run`.

### `psql`

Carregue `local.env` no shell atual.
Abra o cliente no container `sajitar-postgres`.

```bash
set -a && source local.env && set +a
docker exec -it sajitar-postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"
```

### `redis-cli`

O Redis é a instância das sessões de `/tokens`.
A ACL está em [docker/redis/users.acl](../../docker/redis/users.acl).
O usuário `sajitar` vê os prefixos `token:`, `tomb:`, `session:`, `profile:` e `attempt:`.
O usuário `default` fica ligado para o Redis reexecutar o AOF.
A aplicação não usa `default`.

```bash
set -a && source local.env && set +a
docker exec -it sajitar-redis redis-cli --user "$SPRING_DATA_REDIS_USERNAME" --pass "$SPRING_DATA_REDIS_PASSWORD" --no-auth-warning
```

No cliente, liste as sessões com `SCAN 0 MATCH session:*`.
A ACL nega `KEYS` e `FLUSHDB`.

O AOF fica em `/data`, no volume `redis-data`.
`docker restart`, reboot e `compose down` sem `-v` preservam as sessões cujo TTL ainda vale.

### pgAdmin

Abra [http://localhost:15432](http://localhost:15432).
Use `PGADMIN_DEFAULT_EMAIL` e `PGADMIN_DEFAULT_PASSWORD` do `local.env`.

### RedisInsight

Abra [http://localhost:16379](http://localhost:16379).
Cadastre o Redis da stack com estes dados.

- Host `10.0.0.25` ou `sajitar-redis`.
- Porta `6379`.
- Usuário e senha de `SPRING_DATA_REDIS_USERNAME` e `SPRING_DATA_REDIS_PASSWORD`.
- Sem TLS.

### Mailpit

Abra a interface em [http://localhost:8025](http://localhost:8025).
O SMTP no host é `localhost:1025`.
Na rede do Compose, o host é `10.0.0.35` e o nome é `sajitar-mailpit`.
A caixa fica em memória.
`compose down` ou recreate esvazia os e-mails.

No perfil Spring `LOCAL`, a aplicação envia pelo `Mailer`.
O adapter é `MailpitMailer`.
O layout é `mail/message.html`.
Estas rotas enviam código por e-mail.

- `POST /profiles`
- `POST /tokens/verification`
- `POST /tokens/authentication`
- `POST /profiles/password/recovery`
- `POST /profiles/email/recovery`
- `POST /profiles/email/confirm`
- `POST /profiles/deletion`

A interface mostra a mensagem depois do pedido.

## Imagem de demonstração

A imagem traz PostgreSQL, Redis e a API no mesmo container.
O uso é o front-end em ambiente não produtivo.
O build pula os testes com `-Dmaven.test.skip=true`.
O build não chega à fase `verify`.
A massa inicial tem 5000 perfis e 7500 notas.
A tabela `checker` fica vazia.
A definição está em [docker/demo/Dockerfile](../../docker/demo/Dockerfile) e [docker/demo/entrypoint.sh](../../docker/demo/entrypoint.sh).
Os seeds estão em [src/main/resources/demo](../../src/main/resources/demo).

| Objetivo | Comando |
| --- | --- |
| Gerar a imagem | `docker build -f docker/demo/Dockerfile -t sajitar-backend:demo .` |
| Rodar a API em `:8080` | `docker run --rm -p 8080:8080 sajitar-backend:demo` |
| Expor também o Postgres interno | `docker run --rm -p 8080:8080 -p 5432:5432 sajitar-backend:demo` |
| Expor também o Redis interno | `docker run --rm -p 8080:8080 -p 6379:6379 sajitar-backend:demo` |

A imagem não tem volume para o Postgres nem para o Redis.
Cada `docker run` novo recria a massa.
O conteúdo se repete.
Os ids mudam.
Para recomeçar, remova o container e crie outro.

As credenciais fixas valem só dentro do container.
O usuário é `postgres`.
A senha é `sajitar_demo`.
O banco é `sajitar-demo-db`.
Esses valores não são segredo de produção.

A API usa as [URLs da porta 8080](#urls-com-a-api-na-porta-8080).
Se o Compose já ocupar a porta `8080`, pare a stack ou publique outra porta.
Exemplo: `-p 18080:8080`.

## Maven

Use `./mvnw` na raiz para repetir o comando do CI.
`mvn` também funciona quando o Maven está instalado no host.

| Objetivo | Comando |
| --- | --- |
| Compilar sem testes | `./mvnw -q -DskipTests compile` |
| Rodar os testes | `./mvnw test` |
| Testes, relatório JaCoCo e verificação de cobertura | `./mvnw verify` |
| Limpar artefatos e compilar de novo | `./mvnw clean compile` |
| Um teste por classe | `./mvnw -Dtest=NomeDaClasseTest test` |
| Um teste por método | `./mvnw -Dtest=NomeDaClasseTest#nomeDoMetodo test` |

O relatório HTML fica em `target/site/jacoco/index.html` depois de `./mvnw verify`.

### Testes no host

Os testes com `@SpringBootTest` exigem PostgreSQL e Redis acessíveis.
`src/main/resources/application.yml` lê as variáveis na subida.
A lista inclui `SPRING_DATASOURCE_*`, `SPRING_JPA_*`, `SPRING_SQL_*` e `SPRING_DATA_REDIS_*`.
A lista também inclui `SAJITAR_DOMAIN_VALIDATION_*`, `SAJITAR_SECURITY_JWT_*` e `SAJITAR_SECURITY_ATTEMPT_*`.
O complemento de teste fica em `src/test/resources/application.yml`.
Não há perfil Spring `test` separado.
Sem Redis, os testes de `/tokens` e das rotas protegidas falham.

#### Opção A, igual ao CI

O Postgres fica em `localhost:5432`.
Base, usuário e senha são `sajitar_ci`.
O Redis fica em `localhost:6379` com a ACL do repositório.
As variáveis estão em [`.github/scripts/ci.env`](../../.github/scripts/ci.env).

```bash
docker run --rm -d --name sajitar-redis-ci -p 6379:6379 \
  -v "$PWD/docker/redis/users.acl:/etc/redis/users.acl:ro" \
  redis:8.2.9-alpine \
  redis-server --aclfile /etc/redis/users.acl --maxmemory-policy noeviction --appendonly yes
set -a && source .github/scripts/ci.env && set +a
./mvnw verify
```

Com o Compose no ar, o serviço `redis` já publica a porta `6379`.
Nesse caso, pule o `docker run`.

#### Opção B, Compose no ar

```bash
set -a && source local.env && set +a
export SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-$POSTGRES_USER}"
export SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-$POSTGRES_PASSWORD}"
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/${POSTGRES_DB}"
export SPRING_JPA_HIBERNATE_DDL_AUTO="${SPRING_JPA_HIBERNATE_DDL_AUTO:-create-drop}"
export SPRING_JPA_SHOW_SQL="${SPRING_JPA_SHOW_SQL:-false}"
export SPRING_SQL_BEFORE_FRAMEWORK="${SPRING_SQL_BEFORE_FRAMEWORK:-classpath:util/functions.sql,classpath:util/enums.sql}"
export SPRING_SQL_AFTER_FRAMEWORK="${SPRING_SQL_AFTER_FRAMEWORK:-util/columns.sql, util/uniques.sql, util/indexes.sql, settlement/profile.sql, settlement/checker.sql, settlement/note.sql}"
export SAJITAR_DOMAIN_VALIDATION_PROFILE_BIRTHDAY_MIN_AGE_YEARS="${SAJITAR_DOMAIN_VALIDATION_PROFILE_BIRTHDAY_MIN_AGE_YEARS:-18}"
export SAJITAR_DOMAIN_VALIDATION_LIMIT_MAX="${SAJITAR_DOMAIN_VALIDATION_LIMIT_MAX:-100}"
export SAJITAR_PROFILE_UNVERIFIED_MAX_AGE_MINUTES="${SAJITAR_PROFILE_UNVERIFIED_MAX_AGE_MINUTES:-30}"
export SAJITAR_PROFILE_CHANGE_PASSWORD_MAX_AGE_MINUTES="${SAJITAR_PROFILE_CHANGE_PASSWORD_MAX_AGE_MINUTES:-30}"
export SAJITAR_PROFILE_CHANGE_EMAIL_MAX_AGE_MINUTES="${SAJITAR_PROFILE_CHANGE_EMAIL_MAX_AGE_MINUTES:-30}"
export SAJITAR_PROFILE_DELETE_PROFILE_MAX_AGE_MINUTES="${SAJITAR_PROFILE_DELETE_PROFILE_MAX_AGE_MINUTES:-30}"
export SAJITAR_PROFILE_SIGN_IN_MAX_AGE_MINUTES="${SAJITAR_PROFILE_SIGN_IN_MAX_AGE_MINUTES:-30}"
export SAJITAR_PROFILE_UNVERIFIED_PURGE_ZONE="${SAJITAR_PROFILE_UNVERIFIED_PURGE_ZONE:-UTC}"
export SAJITAR_SECURITY_JWT_SECRET="${SAJITAR_SECURITY_JWT_SECRET:-01234567890123456789012345678901}"
export SAJITAR_SECURITY_JWT_EXPIRATION_SECONDS="${SAJITAR_SECURITY_JWT_EXPIRATION_SECONDS:-3600}"
export SAJITAR_SECURITY_JWT_REFRESH_EXPIRATION_SECONDS="${SAJITAR_SECURITY_JWT_REFRESH_EXPIRATION_SECONDS:-604800}"
export SPRING_DATA_REDIS_HOST="${SPRING_DATA_REDIS_HOST:-127.0.0.1}"
export SPRING_DATA_REDIS_PORT="${SPRING_DATA_REDIS_PORT:-6379}"
./mvnw verify
```

### API no host

Exija Postgres e Redis acessíveis.
Com o Compose no ar, use `localhost:5432` e `localhost:6379`.
`SPRING_PROFILES_ACTIVE` é obrigatório.
Sem essa variável, a subida falha.
Não há valor padrão.

```bash
set -a && source local.env && set +a
export SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-$POSTGRES_USER}"
export SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-$POSTGRES_PASSWORD}"
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/${POSTGRES_DB}"
export SPRING_DATA_REDIS_HOST="${SPRING_DATA_REDIS_HOST:-127.0.0.1}"
export SPRING_DATA_REDIS_PORT="${SPRING_DATA_REDIS_PORT:-6379}"
./mvnw spring-boot:run
```

## URLs com a API na porta 8080

| Recurso | Endereço |
| --- | --- |
| OpenAPI em JSON | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) |
| Swagger UI | [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) |
| Collection Postman | [sajitar.postman_collection.json](../api/sajitar.postman_collection.json) |
| Actuator | [http://localhost:8080/actuator](http://localhost:8080/actuator) |

A collection cobre `/tokens`, `/profiles` e `/notes`.
Importe o arquivo no Postman.
Os endpoints expostos do Actuator dependem da configuração.
