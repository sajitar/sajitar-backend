# Comandos

Execute os comandos na raiz do repositório.
O repositório não tem arquivo de variáveis de ambiente.
Cada perfil Spring guarda a configuração num YAML com valores literais.
O `docker-compose.yml` também usa valores literais.
As credenciais valem só para desenvolvimento, teste e demonstração.

## Perfis Spring

`src/main/resources/application.yml` é o arquivo principal.
O arquivo declara só o nome da aplicação e o perfil Spring ativo.
O perfil Spring ativo vem de `SPRING_PROFILES_ACTIVE`.
Sem essa variável, a subida falha.
Não há perfil Spring padrão.

```yaml
spring:
  application:
    name: backend
  profiles:
    active: ${SPRING_PROFILES_ACTIVE}
```

Cada perfil Spring tem um YAML próprio.
Esse YAML não lê variável de ambiente.
Todas as propriedades ficam com valor literal.

| Perfil Spring | Arquivo | Entra no jar |
| --- | --- | --- |
| `LOCAL` | `src/main/resources/application-LOCAL.yml` | Sim |
| `LOCAL-TEST` | `src/test/resources/application-LOCAL-TEST.yml` | Não |
| `CI` | `src/test/resources/application-CI.yml` | Não |
| `DEMO` | `src/main/resources/application-DEMO.yml` | Sim |
| `DOCKER` | `src/main/resources/application-DOCKER.yml` | Sim |
| `DOCKER-TEST` | `src/test/resources/application-DOCKER-TEST.yml` | Não |

Os perfis Spring `LOCAL` e `LOCAL-TEST` valem só para a aplicação fora dos containers Docker.
Postgres, Redis e Mailpit podem estar no Compose.
A JVM da aplicação, porém, corre no host.
Os perfis Spring `DOCKER` e `DOCKER-TEST` valem só dentro do container `sajitar-springboot`.

| Perfil Spring | Descrição | Comando |
| --- | --- | --- |
| `LOCAL` | API no host, fora dos containers. Valores literais. Postgres, Redis e Mailpit em `localhost`. | `SPRING_PROFILES_ACTIVE="LOCAL" mvn clean compile spring-boot:run` |
| `LOCAL-TEST` | Testes no host, fora dos containers. Valores literais. O arquivo fica em `src/test` e não entra no jar. | `SPRING_PROFILES_ACTIVE="LOCAL-TEST" mvn clean compile test` |
| `CI` | Testes no GitHub Actions. Banco e Redis do job. | `SPRING_PROFILES_ACTIVE="CI" ./mvnw verify` |
| `DEMO` | Imagem de demonstração. Postgres, Redis, Mailpit e API no mesmo container. Massa `demo/*.sql`. | `docker run --rm -p 8080:8080 -p 8025:8025 sajitar-backend:demo` |
| `DOCKER` | API dentro do container `sajitar-springboot` do Compose. Hosts na rede `10.0.0.x`. | `SPRING_PROFILES_ACTIVE="DOCKER" ./mvnw clean compile spring-boot:run` |
| `DOCKER-TEST` | Testes dentro do container JDK do Compose. Hosts na rede `10.0.0.x`. | `SPRING_PROFILES_ACTIVE="DOCKER-TEST" ./mvnw clean compile test` |

Uma variável de ambiente tem prioridade sobre o YAML.
Exemplo: `SPRING_DATASOURCE_URL` sobrepõe `spring.datasource.url`.
Passe só `SPRING_PROFILES_ACTIVE`, na frente do comando.
Não exporte outras variáveis `SPRING_*` ou `SAJITAR_*` no shell nem no `~/.profile`.

### Perfil Spring `LOCAL`

O perfil Spring `LOCAL` sobe a API no host.
A API alcança Postgres, Redis e Mailpit em `localhost`.
O `MailpitMailer` envia os e-mails ao Mailpit do Compose.

Aviso: cada subida do perfil Spring `LOCAL` apaga os dados de `sajitar-db`.
O `ddl-auto` `create` recria o schema e grava a massa `settlement`.

1. Suba Postgres, Redis e Mailpit do Compose.
2. Suba a API no host.

```bash
docker compose up -d postgres redis mailpit
SPRING_PROFILES_ACTIVE="LOCAL" mvn clean compile spring-boot:run
```

O container `sajitar-springboot` publica a porta `8080`.
Com esse container no ar, a API do host não sobe.
Pare o container com `docker compose stop springboot`.

### Perfil Spring `LOCAL-TEST`

O perfil Spring `LOCAL-TEST` roda os testes no host.
Os testes alcançam Postgres e Redis em `localhost`.
O `RecordingMailer` guarda os e-mails em memória.
Os testes não usam o Mailpit.

Aviso: os testes apagam os dados de `sajitar-db` e as chaves de sessão do Redis.
O `ddl-auto` `create-drop` recria o schema na subida e apaga as tabelas no fim.
O perfil Spring `LOCAL` usa o mesmo banco e o mesmo Redis.
Pare a API do host antes dos testes.

1. Suba Postgres e Redis do Compose.
2. Rode os testes no host.

```bash
docker compose up -d postgres redis
SPRING_PROFILES_ACTIVE="LOCAL-TEST" mvn clean compile test
```

Para o relatório JaCoCo e a verificação de cobertura, rode o `verify`.

```bash
SPRING_PROFILES_ACTIVE="LOCAL-TEST" mvn clean verify
```

### Perfis Spring `DOCKER` e `DOCKER-TEST`

Os dois perfis Spring rodam no container `sajitar-springboot`.
O container usa Temurin 25.
O repositório fica montado em `/app`.
O YAML de cada perfil Spring aponta os IPs fixos da rede do Compose.

| Serviço | Host |
| --- | --- |
| Postgres | `10.0.0.10` |
| Redis | `10.0.0.25` |
| Mailpit | `10.0.0.35` |

Aviso: `DOCKER` e `DOCKER-TEST` apagam os dados de `sajitar-db`, como `LOCAL` e `LOCAL-TEST`.
Os quatro perfis Spring usam o mesmo banco e o mesmo Redis.

1. Suba a stack inteira do Compose.
2. Abra um shell no container.
3. Em `/app`, rode o comando do perfil Spring.

```bash
docker compose up -d
docker exec -it sajitar-springboot bash
```

```bash
SPRING_PROFILES_ACTIVE="DOCKER" ./mvnw clean compile spring-boot:run
SPRING_PROFILES_ACTIVE="DOCKER-TEST" ./mvnw clean compile test
```

O perfil Spring `DOCKER` publica a API na porta `8080` do host.
O `MailpitMailer` envia os e-mails ao Mailpit em `10.0.0.35`.

O container roda como root.
O Maven do container grava `target/` com o dono root.
Antes de compilar no host, devolva a posse de `target/` ao seu usuário.

```bash
docker exec -w /app sajitar-springboot chown -R "$(id -u):$(id -g)" target
```

### Perfil Spring `CI`

O workflow [`.github/workflows/verify.yml`](../../.github/workflows/verify.yml) usa o perfil Spring `CI`.
O banco `sajitar_ci` é o service `postgres` do job.
O job sobe o Redis com a ACL do repositório.
Os dois ficam em `localhost`.
O job roda `SPRING_PROFILES_ACTIVE="CI" ./mvnw verify`.

### Perfil Spring `DEMO`

A imagem de demonstração usa o perfil Spring `DEMO`.
O uso está em [Imagem de demonstração](#imagem-de-demonstração).

## Docker Compose

O Compose sobe a infraestrutura de desenvolvimento.
O `docker-compose.yml` não lê variável de ambiente.

| Objetivo | Comando |
| --- | --- |
| Subir Postgres, Redis, pgAdmin, RedisInsight, Mailpit e o container JDK | `docker compose up -d` |
| Subir só Postgres, Redis e Mailpit para os perfis Spring `LOCAL` e `LOCAL-TEST` | `docker compose up -d postgres redis mailpit` |
| Recriar imagens e containers depois de mudar `docker-compose.yml` | `docker compose up -d --build` |
| Parar o container JDK e liberar a porta `8080` | `docker compose stop springboot` |
| Parar e remover os containers. O volume `redis-data` permanece | `docker compose down` |
| Ver os logs de todos os serviços | `docker compose logs -f` |
| Listar os containers da stack | `docker compose ps` |

Logs de um serviço:

```bash
docker compose logs -f postgres
docker compose logs -f redis
docker compose logs -f redisinsight
docker compose logs -f mailpit
docker compose logs -f springboot
```

Aviso: o comando abaixo apaga o volume `redis-data` e revoga as sessões de `/tokens`.

```bash
docker compose down -v
```

### `psql`

Abra o cliente no container `sajitar-postgres`.
O usuário é `postgres`.
O banco é `sajitar-db`.

```bash
docker exec -it sajitar-postgres psql -U postgres -d sajitar-db
```

### `redis-cli`

O Redis é a instância das sessões de `/tokens`.
A ACL está em [docker/redis/users.acl](../../docker/redis/users.acl).
O usuário `sajitar` vê os prefixos `token:`, `tomb:`, `session:`, `profile:` e `attempt:`.
O usuário `default` fica ligado para o Redis reexecutar o AOF.
A aplicação não usa `default`.

```bash
docker exec -it sajitar-redis redis-cli --user sajitar --pass sajitar_dev --no-auth-warning
```

No cliente, liste as sessões com `SCAN 0 MATCH session:*`.
A ACL nega `KEYS` e `FLUSHDB`.

O AOF fica em `/data`, no volume `redis-data`.
`docker restart`, reboot e `compose down` sem `-v` preservam as sessões cujo TTL ainda vale.

### pgAdmin

Abra [http://localhost:15432](http://localhost:15432).
O e-mail é `admin@local.host`.
A senha é `123456`.

### RedisInsight

Abra [http://localhost:16379](http://localhost:16379).
Cadastre o Redis da stack com estes dados.

- Host `10.0.0.25` ou `sajitar-redis`.
- Porta `6379`.
- Usuário `sajitar` e senha `sajitar_dev`.
- Sem TLS.

### Mailpit

Abra a interface em [http://localhost:8025](http://localhost:8025).
O SMTP no host é `localhost:1025`.
Na rede do Compose, o host é `10.0.0.35` e o nome é `sajitar-mailpit`.
A caixa fica em memória.
`compose down` ou recreate esvazia os e-mails.

O `Mailer` muda com o perfil Spring.

| Perfil Spring | Adapter | Destino |
| --- | --- | --- |
| `LOCAL` | `MailpitMailer` | `localhost:1025` |
| `DOCKER` | `MailpitMailer` | `10.0.0.35:1025` |
| `DEMO` | `MailpitMailer` | Mailpit da própria imagem, em `127.0.0.1:1025` |
| `LOCAL-TEST`, `CI` e `DOCKER-TEST` | `RecordingMailer` | Memória da JVM de teste |

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

A imagem traz PostgreSQL, Redis, Mailpit e a API no mesmo container.
A imagem define `SPRING_PROFILES_ACTIVE=DEMO`.
O perfil Spring `DEMO` aponta banco, Redis e SMTP para `127.0.0.1`.
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
| Rodar a API em `:8080` e a caixa do Mailpit em `:8025` | `docker run --rm -p 8080:8080 -p 8025:8025 sajitar-backend:demo` |
| Expor também o Postgres interno | `docker run --rm -p 8080:8080 -p 8025:8025 -p 5432:5432 sajitar-backend:demo` |
| Expor também o Redis interno | `docker run --rm -p 8080:8080 -p 8025:8025 -p 6379:6379 sajitar-backend:demo` |

A imagem não tem volume para o Postgres nem para o Redis.
A caixa do Mailpit fica em memória.
Cada `docker run` novo recria a massa e esvazia a caixa.
O conteúdo se repete.
Os ids mudam.
Para recomeçar, remova o container e crie outro.

As credenciais fixas valem só dentro do container.
O usuário é `postgres`.
A senha é `sajitar_demo`.
O banco é `sajitar-demo-db`.
`application-DEMO.yml` e `entrypoint.sh` repetem esses valores.
Esses valores não são segredo de produção.

A API usa as [URLs da porta 8080](#urls-com-a-api-na-porta-8080).
A caixa do Mailpit fica em [http://localhost:8025](http://localhost:8025).
Se o Compose já ocupar as portas `8080` e `8025`, pare a stack ou publique outras portas.
Exemplo: `-p 18080:8080 -p 18025:8025`.

## Maven

Use `./mvnw` na raiz para repetir o comando do CI.
`mvn` também funciona quando o Maven está instalado no host.
Todo comando com testes exige um perfil Spring de teste.
No host, o perfil Spring é `LOCAL-TEST`.
No container `sajitar-springboot`, troque `LOCAL-TEST` por `DOCKER-TEST`.

| Objetivo | Comando |
| --- | --- |
| Compilar sem testes | `./mvnw -q -DskipTests compile` |
| Rodar os testes | `SPRING_PROFILES_ACTIVE="LOCAL-TEST" ./mvnw test` |
| Testes, relatório JaCoCo e verificação de cobertura | `SPRING_PROFILES_ACTIVE="LOCAL-TEST" ./mvnw verify` |
| Limpar artefatos e compilar de novo | `./mvnw clean compile` |
| Um teste por classe | `SPRING_PROFILES_ACTIVE="LOCAL-TEST" ./mvnw -Dtest=NomeDaClasseTest test` |
| Um teste por método | `SPRING_PROFILES_ACTIVE="LOCAL-TEST" ./mvnw -Dtest=NomeDaClasseTest#nomeDoMetodo test` |

O relatório HTML fica em `target/site/jacoco/index.html` depois do `verify`.

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
