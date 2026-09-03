# Sajitar Backend

## Comandos Relevantes

Na raiz do repositório. Variáveis de ambiente vêm do `local.env` — arquivo **versionado**, com credenciais apenas de desenvolvimento local (sem segredos de produção). O sufixo `local` deixa explícito que não é um `.env` privado/gitignored.

### Docker Compose

| Objetivo | Comando |
| --- | --- |
| Subir Postgres, pgAdmin e o container da aplicação (JDK montado em `/app`) | `env -i docker compose --env-file local.env up -d` |
| Recriar imagens/containers após mudanças no `docker-compose.yml` | `env -i docker compose --env-file local.env up -d --build` |
| Parar e remover containers da stack (mesmo padrão “ambiente limpo” do `up`) | `env -i docker compose --env-file local.env down` |
| Ver logs em tempo real (todos os serviços) | `docker compose --env-file local.env logs -f` |
| Logs só do Postgres ou do container Java | `docker compose --env-file local.env logs -f postgres` ou `docker compose --env-file local.env logs -f springboot` |
| Listar containers da stack | `docker compose --env-file local.env ps` |

#### Shell no container da aplicação (Temurin 26, código em `/app`)

```bash
docker exec -it sajitar-springboot bash
```

Dentro do container, o Compose já injeta `SPRING_DATASOURCE_*` apontando para o Postgres da rede interna; a partir de `/app` você pode usar Maven, por exemplo `./mvnw spring-boot:run`.

#### Cliente `psql` no Postgres

Com variáveis do `local.env` carregadas no shell atual:

```bash
set -a && source local.env && set +a
docker exec -it sajitar-postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"
```

#### pgAdmin

Interface em [http://localhost:15432](http://localhost:15432) (credenciais conforme `PGADMIN_*` no `local.env`).

### Maven

Use `./mvnw` na raiz para reproduzir o mesmo comando do CI; `mvn` também funciona se o Maven estiver instalado no host.

| Objetivo | Comando |
| --- | --- |
| Compilar sem rodar testes | `./mvnw -q -DskipTests compile` |
| Rodar testes | `./mvnw test` |
| Testes + relatório JaCoCo + verificação de cobertura (`verify`) | `./mvnw verify` |
| Limpar artefatos e compilar de novo | `./mvnw clean compile` |
| Um teste por classe ou método | `./mvnw -Dtest=NomeDaClasseTest test` ou `./mvnw -Dtest=NomeDaClasseTest#nomeDoMetodo test` |

Relatório HTML do JaCoCo (após `./mvnw verify`): `target/site/jacoco/index.html`.

#### Testes e cobertura (host)

Os testes com `@SpringBootTest` exigem **PostgreSQL** acessível e as variáveis que `src/main/resources/application.yml` resolve em tempo de execução (`SPRING_DATASOURCE_*`, `SPRING_JPA_*`, `SPRING_SQL_*`, `SAJITAR_DOMAIN_VALIDATION_*`). A configuração complementar de teste fica em `src/test/resources/application.yml` (sem perfil Spring `test` separado).

**Opção A — alinhar ao CI** (Postgres em `localhost:5432`, base/usuário/senha `sajitar_ci`):

```bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://127.0.0.1:5432/sajitar_ci"
export SPRING_DATASOURCE_USERNAME="sajitar_ci"
export SPRING_DATASOURCE_PASSWORD="sajitar_ci"
export SPRING_JPA_HIBERNATE_DDL_AUTO="create-drop"
export SPRING_JPA_SHOW_SQL="false"
export SPRING_SQL_INIT_MODE="always"
export SPRING_SQL_BEFORE_FRAMEWORK="classpath:util/functions.sql"
export SPRING_SQL_AFTER_FRAMEWORK="util/columns.sql, util/uniques.sql, util/indexes.sql, settlement/profile.sql, settlement/checker.sql"
export SAJITAR_DOMAIN_VALIDATION_PROFILE_BIRTHDAY_MIN_AGE_YEARS="18"
export SAJITAR_DOMAIN_VALIDATION_LIMIT_MAX="100"
./mvnw verify
```

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
export SPRING_SQL_AFTER_FRAMEWORK="${SPRING_SQL_AFTER_FRAMEWORK:-util/columns.sql, util/uniques.sql, util/indexes.sql, settlement/profile.sql, settlement/checker.sql}"
export SAJITAR_DOMAIN_VALIDATION_PROFILE_BIRTHDAY_MIN_AGE_YEARS="${SAJITAR_DOMAIN_VALIDATION_PROFILE_BIRTHDAY_MIN_AGE_YEARS:-18}"
export SAJITAR_DOMAIN_VALIDATION_LIMIT_MAX="${SAJITAR_DOMAIN_VALIDATION_LIMIT_MAX:-100}"
./mvnw verify
```

#### Rodar a API na máquina host

Exige Postgres acessível (por exemplo `localhost:5432` com o compose no ar) e as mesmas variáveis que o Spring lê em `application.yml` (`SPRING_DATASOURCE_*`, `SPRING_JPA_*`, `SPRING_SQL_*`, etc.), tipicamente exportadas a partir do `local.env`:

```bash
set -a && source local.env && set +a
# No host, use o Postgres exposto em localhost:5432 e alinhe nomes ao application.yml:
export SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-$POSTGRES_USER}"
export SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-$POSTGRES_PASSWORD}"
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/${POSTGRES_DB}"
./mvnw spring-boot:run
```

### URLs úteis (app na porta 8080)

| Recurso | Endereço |
| --- | --- |
| OpenAPI (JSON) | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) |
| Swagger UI | [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) |
| Collection Postman | [docs/sajitar.postman_collection.json](docs/sajitar.postman_collection.json) (`/profiles` e `/checkers`; Import no Postman) |
| Actuator | [http://localhost:8080/actuator](http://localhost:8080/actuator) (endpoints expostos dependem da configuração) |

### API `/profiles`

Query opcional **`lang`**: `en` (padrão), `pt` ou `es`. Omitida, vazia ou não suportada → inglês. Sem sessão e sem `Accept-Language`.

| Método | Caminho | Sucesso |
| --- | --- | --- |
| POST | `/profiles` | 200 + resumo (id, name, description; sem senha) |
| GET | `/profiles/{id}` | 200 + resumo |
| GET | `/profiles/{id}/details` | 200 + detalhes (sem senha) |
| PUT | `/profiles/{id}` | 200 + resumo; id só na URL; senha omitida mantém o hash |
| PATCH | `/profiles/{id}` | 200 + resumo; campos omitidos permanecem; `"description": null` limpa a descrição |
| DELETE | `/profiles/{id}` | 204; 404 se ausente (não é 204 idempotente) |
| GET | `/profiles` | 200 + página por cursor (`name`, `lastSeenName`, `lastSeenId`, `limit`, `reverse`; `precedingElements` / `followingElements`) |

Erros: **400** mapa campo→mensagens; **409** e-mail já registrado; **404** sem corpo. Detalhes no OpenAPI e na collection Postman.

A listagem **`GET /profiles`** pagina por cursor sobre nome e id. Exemplos de navegação também em `ProfileControllerIntegrationTest`.

### API `/checkers`

Query opcional **`lang`**: mesma regra de `/profiles`. Tipos públicos no JSON: `CHANGE_EMAIL` (0), `VERIFY_EMAIL` (1, restrito), `CHANGE_PASSWORD` (4). O campo `type` aceita o nome do enum ou o número; valores como `CHANGE_PHONE` / `VERIFY_PHONE` → **400**. `code` e `payload` **nunca** saem na resposta.

| Método | Caminho | Sucesso |
| --- | --- | --- |
| POST | `/checkers?profileId=` | 200 + visão pública (`id`, `profileId`, `type`, `replaces`, `attempts`, `updatedAt`, `requiredPayload`) |
| GET | `/checkers/{id}` | 200 + visão pública |
| GET | `/checkers?profileId=&type=` | 200 + um registro do par (perfil, tipo) |
| GET | `/checkers?profileId=&lastSeenType=&limit=` | 200 + página `{content, precedingElements, followingElements, reverse}` (cursor por tipo na query) |
| PUT | `/checkers/{id}` | 200; id só na URL; campos omitidos ou nulos voltam aos defaults (código gerado, payload nulo, attempts 10, replaces 3) |
| PATCH | `/checkers/{id}` | 200; só campos não nulos; omitido ou `null` mantém o valor |
| DELETE | `/checkers/{id}` | 204; 404 se ausente (não é 204 idempotente) |

Erros: **400** mapa campo→mensagens (validação ou tipo desconhecido); **403** criar ou excluir `VERIFY_EMAIL`; **409** já existe o tipo para o perfil; **404** checker ausente sem corpo; **404** perfil inexistente no POST **com** corpo `{profileId:[…]}`; lista vazia → **404**. Detalhes no OpenAPI e na collection Postman.

A listagem **`GET /checkers`** (sem `type`) pagina por cursor sobre o tipo. Exemplos também em `CheckerControllerIntegrationTest`.

### Schema SQL (após o DDL do Hibernate)

Cadeia em `SPRING_SQL_AFTER_FRAMEWORK`: `util/columns.sql` (colunas geradas, CHECKs, FKs) → `util/uniques.sql` (e-mail do perfil; par `profile_id`+`type` do checker) → `util/indexes.sql` → `settlement/profile.sql` e `settlement/checker.sql`. Funções em `util/functions.sql` rodam **antes**, via `SPRING_SQL_BEFORE_FRAMEWORK`. As unicidades não ficam em anotações JPA.

## Git Flow

| Branch | Função |
| --- | --- |
| `main` ou `master` | Código em produção (ou refletindo o que foi liberado). |
| `develop` ou `development` | Integração contínua do time; destino padrão do dia a dia. |
| `feat/*`, `fix/*`, … | Branches de trabalho partindo tipicamente de `develop`. |
| `release/*` | Preparação de versão (congelamento, ajustes finais) antes de ir a produção. |
| `hotfix/*` | Correção urgente em produção, normalmente ramificada a partir de `main`. |

> Para mais detalhes leia a [política de branches](docs/POLITICA-DE-BRANCHES.md) e a [política de testes](docs/POLITICA-DE-TESTES.md) (referência alinhada à ISO/IEC 29119 no âmbito deste repositório).

## 🛠 Tecnologias

### Plataforma

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-26-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)

### Dados e persistência

![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Hibernate](https://img.shields.io/badge/Hibernate-59666C?style=for-the-badge&logo=hibernate&logoColor=white)

### API, contratos e validação

![Spring Web](https://img.shields.io/badge/Spring%20Web-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![OpenAPI](https://img.shields.io/badge/OpenAPI%20%2F%20Swagger-6BA43A?style=for-the-badge&logo=openapiinitiative&logoColor=white)
![Jakarta Validation](https://img.shields.io/badge/Jakarta%20Validation-748289?style=for-the-badge)

### Observabilidade e utilitários

![Spring Actuator](https://img.shields.io/badge/Spring%20Actuator-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![UUID Generator](https://img.shields.io/badge/java--uuid--generator-007396?style=for-the-badge)
![Lombok](https://img.shields.io/badge/Lombok-dc382d?style=for-the-badge)

### Qualidade e testes

![JUnit 5](https://img.shields.io/badge/JUnit%205-25A162?style=for-the-badge&logo=junit5&logoColor=white)
![JaCoCo](https://img.shields.io/badge/JaCoCo-cobertura-007396?style=for-the-badge)
![Surefire](https://img.shields.io/badge/Maven%20Surefire-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)

### CI e infraestrutura local

![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

### Ambiente de desenvolvimento (Docker Compose)

![Eclipse Temurin](https://img.shields.io/badge/Eclipse%20Temurin-26-FF6C00?style=for-the-badge&logo=eclipseadoptium&logoColor=white)
![pgAdmin](https://img.shields.io/badge/pgAdmin-326690?style=for-the-badge&logo=postgresql&logoColor=white)
![Spring DevTools](https://img.shields.io/badge/Spring%20DevTools-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
