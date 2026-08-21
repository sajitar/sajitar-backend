# Sajitar Backend

## Comandos Relevantes

Na raiz do repositório. Variáveis de ambiente vêm do `.env` (use o arquivo acordado com o time).

### Docker Compose

| Objetivo | Comando |
| --- | --- |
| Subir Postgres, pgAdmin e o container da aplicação (JDK montado em `/app`) | `env -i docker compose --env-file .env up -d` |
| Recriar imagens/containers após mudanças no `docker-compose.yml` | `env -i docker compose --env-file .env up -d --build` |
| Parar e remover containers da stack (mesmo padrão “ambiente limpo” do `up`) | `env -i docker compose --env-file .env down` |
| Ver logs em tempo real (todos os serviços) | `docker compose --env-file .env logs -f` |
| Logs só do Postgres ou do container Java | `docker compose --env-file .env logs -f postgres` ou `docker compose --env-file .env logs -f springboot` |
| Listar containers da stack | `docker compose --env-file .env ps` |

#### Shell no container da aplicação (Temurin 25, código em `/app`)

```bash
docker exec -it sajitar-springboot bash
```

Dentro do container, o Compose já injeta `SPRING_DATASOURCE_*` apontando para o Postgres da rede interna; a partir de `/app` você pode usar Maven, por exemplo `mvn spring-boot:run`.

#### Cliente `psql` no Postgres

Com variáveis do `.env` carregadas no shell atual:

```bash
set -a && source .env && set +a
docker exec -it sajitar-postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"
```

#### pgAdmin

Interface em [http://localhost:15432](http://localhost:15432) (credenciais conforme `PGADMIN_*` no `.env`).

### Maven

| Objetivo | Comando |
| --- | --- |
| Compilar sem rodar testes | `mvn -q -DskipTests compile` |
| Rodar testes | `mvn test` |
| Testes + relatório JaCoCo + verificação de cobertura (`verify`) | `mvn verify` |
| Limpar artefatos e compilar de novo | `mvn clean compile` |
| Um teste por classe ou método | `mvn -Dtest=NomeDaClasseTest test` ou `mvn -Dtest=NomeDaClasseTest#nomeDoMetodo test` |

Relatório HTML do JaCoCo (após `mvn verify`): `target/site/jacoco/index.html`.

#### Rodar a API na máquina host

Exige Postgres acessível (por exemplo `localhost:5432` com o compose no ar) e as mesmas variáveis que o Spring lê em `application.yml` (`SPRING_DATASOURCE_*`, `SPRING_JPA_*`, `SPRING_SQL_*`, etc.), tipicamente exportadas a partir do `.env`:

```bash
set -a && source .env && set +a
# No host, use o Postgres exposto em localhost:5432 e alinhe nomes ao application.yml:
export SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-$POSTGRES_USER}"
export SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-$POSTGRES_PASSWORD}"
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/${POSTGRES_DB}"
mvn spring-boot:run
```

### URLs úteis (app na porta 8080)

| Recurso | Endereço |
| --- | --- |
| OpenAPI (JSON) | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) |
| Swagger UI | [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) |
| Actuator | [http://localhost:8080/actuator](http://localhost:8080/actuator) (endpoints expostos dependem da configuração) |

### Documentação da API de listagem de perfis

A listagem paginada **`GET /profiles`** usa **cursor** (`lastSeenName`, `lastSeenId`) sobre a ordenação por nome e id, com filtros opcionais e contadores `precedingElements` / `followingElements`. O ficheiro **[docs/paginacao-api-profiles.md](docs/paginacao-api-profiles.md)** explica parâmetros, formato da resposta, como avançar e **a lógica em pilha** para voltar a páginas anteriores sem parâmetro dedicado no servidor, e **por que o cursor escala melhor** do que paginação por offset em grandes volumes de dados.

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

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)

### Dados e persistência

![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Spring Data JDBC](https://img.shields.io/badge/Spring%20Data%20JDBC-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
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

![Eclipse Temurin](https://img.shields.io/badge/Eclipse%20Temurin-25-FF6C00?style=for-the-badge&logo=eclipseadoptium&logoColor=white)
![pgAdmin](https://img.shields.io/badge/pgAdmin-326690?style=for-the-badge&logo=postgresql&logoColor=white)
![Spring DevTools](https://img.shields.io/badge/Spring%20DevTools-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
