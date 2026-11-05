# Comandos relevantes

Na raiz do repositório. Perfis `LOCAL`, `LOCAL-TEST`, `CI`, `DOCKER`, `DOCKER-TEST` e `DEMO` têm credenciais e hosts **literais** nos YAML. Toda chamada que sobe a aplicação informa exatamente um `SPRING_PROFILES_ACTIVE`; ausente, em branco, com mais de um perfil ou fora de `LOCAL`, `LOCAL-TEST`, `CI`, `PROD`, `DOCKER`, `DOCKER-TEST` e `DEMO`, o boot falha (sem default). `PROD` continua só com `${VAR}` (segredos). Teste em ambiente local: `SPRING_PROFILES_ACTIVE="LOCAL-TEST" mvn clean compile test`.

## Docker Compose

| Objetivo | Comando |
| --- | --- |
| Subir Postgres, Redis, pgAdmin, RedisInsight, Mailpit e o container da aplicação (JDK montado em `/app`) | `docker compose up -d` |
| Recriar imagens/containers após mudanças no `docker-compose.yml` | `docker compose up -d --build` |
| Parar e remover containers da stack (o volume `redis-data` permanece) | `docker compose down` |
| Apagar também o volume do Redis (revoga todas as sessões de `/tokens`) | `docker compose down -v` |
| Ver logs em tempo real (todos os serviços) | `docker compose logs -f` |
| Logs só do Postgres, do Redis, do RedisInsight, do Mailpit ou do container Java | `docker compose logs -f postgres`, `... logs -f redis`, `... logs -f redisinsight`, `... logs -f mailpit` ou `... logs -f springboot` |
| Listar containers da stack | `docker compose ps` |

Postgres: usuário `postgres`, senha `123456`, banco `sajitar-db`. Redis: usuário `sajitar`, senha `sajitar_dev`.

### Shell no container da aplicação (Temurin 25, código em `/app`)

```bash
docker exec -it sajitar-springboot bash
```

O Compose define `SPRING_PROFILES_ACTIVE=DOCKER` (`application-DOCKER.yml`: Postgres `10.0.0.10`, Redis `10.0.0.25`, Mailpit `10.0.0.35`). A partir de `/app`: `./mvnw spring-boot:run`.

### Cliente `psql` no Postgres

```bash
docker exec -it sajitar-postgres psql -U postgres -d sajitar-db
```

### Cliente `redis-cli` nas sessões de `/tokens`

Instância dedicada às sessões (AUTH e ACL de [docker/redis/users.acl](../../docker/redis/users.acl); o `sajitar` só enxerga os prefixos `token:`, `tomb:`, `session:`, `profile:` e `attempt:`; o `default` fica ligado com senha própria para o Redis reexecutar o AOF no restart — a aplicação não o usa):

```bash
docker exec -it sajitar-redis redis-cli --user sajitar --pass sajitar_dev --no-auth-warning
```

Dentro do cliente, `SCAN 0 MATCH session:*` lista as sessões ativas (`KEYS` e `FLUSHDB` são negados pela ACL).

O Redis local grava AOF em `/data` no volume nomeado `redis-data`: `docker restart`, reboot e `compose down` (sem `-v`) preservam as sessões cujo TTL ainda vale. `compose down -v` apaga o volume.

### pgAdmin

Interface em [http://localhost:15432](http://localhost:15432) (`admin@local.host` / `123456`).

### RedisInsight

Interface em [http://localhost:16379](http://localhost:16379). Cadastre o Redis da stack: host `10.0.0.25` (ou `sajitar-redis`), porta `6379`, usuário `sajitar`, senha `sajitar_dev`, sem TLS.

### Mailpit

Interface em [http://localhost:8025](http://localhost:8025). SMTP em `localhost:1025` no host, ou `10.0.0.35` (`sajitar-mailpit`) na rede do Compose. Inbox em memória: `compose down` ou recreate esvazia os e-mails. Nos perfis `LOCAL` e `DOCKER` a aplicação envia pela porta de domínio `Mailer` (`MailpitMailer` → SMTP). O `POST /profiles` dispara o envio do código de verificação de e-mail em HTML (layout `mail/message.html`); `POST /profiles/password/recovery` envia o código de redefinição; `POST /profiles/email/recovery` e `POST /profiles/email/confirm` enviam os códigos de troca de e-mail. A UI do Mailpit mostra a mensagem após o pedido.

## Imagem Docker de demonstração

Imagem única e autocontida (PostgreSQL + aplicação no mesmo container), pensada para o **front-end consumir em ambiente não produtivo**. Build sem rodar testes/validações (`-Dmaven.test.skip=true`, sem chegar à fase `verify`) e sobe já com uma massa de dados **densa** (ordem de milhares) pré-carregada: ~5.000 `profile` e ~7.500 `note`. A massa de `checker` **não** é carregada (tabela sempre vazia). Definições em [docker/demo/Dockerfile](../../docker/demo/Dockerfile) e [docker/demo/entrypoint.sh](../../docker/demo/entrypoint.sh); perfil `DEMO` (`application-DEMO.yml`); seeds em [src/main/resources/demo](../../src/main/resources/demo).

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
| Rodar testes | `SPRING_PROFILES_ACTIVE="LOCAL-TEST" ./mvnw test` |
| Testes + relatório JaCoCo + verificação de cobertura (`verify`) | `SPRING_PROFILES_ACTIVE="LOCAL-TEST" ./mvnw verify` |
| Limpar artefatos e compilar de novo | `./mvnw clean compile` |
| Um teste por classe ou método | `SPRING_PROFILES_ACTIVE="LOCAL-TEST" ./mvnw -Dtest=NomeDaClasseTest test` ou `SPRING_PROFILES_ACTIVE="LOCAL-TEST" ./mvnw -Dtest=NomeDaClasseTest#nomeDoMetodo test` |

Relatório HTML do JaCoCo (após `SPRING_PROFILES_ACTIVE="LOCAL-TEST" ./mvnw verify`): `target/site/jacoco/index.html`.

### Testes e cobertura (host)

Os testes com `@SpringBootTest` exigem **PostgreSQL** e **Redis** acessíveis e exatamente um `SPRING_PROFILES_ACTIVE`. O perfil Spring **`LOCAL-TEST`** (`application-LOCAL-TEST.yml`) traz datasource/Redis/JWT/DDL literais (`sajitar-db` em `127.0.0.1`, Redis da ACL local, `create-drop`, settlement, `RecordingMailer`). Sem Redis, os testes de `/tokens` e das rotas protegidas falham: a aplicação não emite nem valida token sem o store de sessões.

Com o Compose no ar (Postgres e Redis nas portas do host):

```bash
SPRING_PROFILES_ACTIVE="LOCAL-TEST" mvn clean compile test
```

Ou, com o wrapper e a verificação de cobertura:

```bash
SPRING_PROFILES_ACTIVE="LOCAL-TEST" ./mvnw verify
```

No container `sajitar-springboot` (Postgres/Redis em `10.0.0.x`), use `DOCKER-TEST`:

```bash
SPRING_PROFILES_ACTIVE="DOCKER-TEST" ./mvnw verify
```

Para subir o jar contra o banco `sajitar_ci` (não os testes), use `SPRING_PROFILES_ACTIVE=CI`.

### Rodar a API na máquina host

Exige Postgres e Redis acessíveis (`localhost:5432` e `localhost:6379` com o Compose no ar). `SPRING_PROFILES_ACTIVE` é obrigatório e só aceita `LOCAL`, `LOCAL-TEST`, `CI`, `PROD`, `DOCKER`, `DOCKER-TEST` ou `DEMO` (sem default). No host use `LOCAL`:

```bash
SPRING_PROFILES_ACTIVE=LOCAL ./mvnw spring-boot:run
```

No container Compose o perfil já é `DOCKER`.

## URLs úteis (app na porta 8080)

| Recurso | Endereço |
| --- | --- |
| OpenAPI (JSON) | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) |
| Swagger UI | [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) |
| Collection Postman | [sajitar.postman_collection.json](../api/sajitar.postman_collection.json) (`/tokens`, `/profiles` e `/notes`; Import no Postman) |
| Actuator | [http://localhost:8080/actuator](http://localhost:8080/actuator) (endpoints expostos dependem da configuração) |
