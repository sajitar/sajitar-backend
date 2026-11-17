# Sajitar Backend

API Spring Boot do Sajitar.
Cada perfil Spring guarda a configuração num YAML com valores literais.
Os perfis estão em [Comandos](docs/development/commands.md#perfis-spring).
As credenciais valem só para desenvolvimento, teste e demonstração.

## Documentação

A escrita dos Markdown segue [docs/policies/escrita_ste100.md](docs/policies/escrita_ste100.md).

| Documento | Conteúdo |
| --- | --- |
| [Comandos](docs/development/commands.md) | Perfis Spring, Docker Compose, imagem de demonstração, Maven, `verify`, `spring-boot:run` e URLs |
| [API `/tokens`](docs/api/tokens.md) | Sessão, emissão e rotação de JWT |
| [API `/profiles`](docs/api/profiles.md) | Contrato HTTP de perfil |
| [API `/notes`](docs/api/notes.md) | Contrato HTTP de nota |
| [Schema SQL](docs/development/schema_sql.md) | Cadeia `util/*` e `settlement/*` em volta do DDL |
| [Política de branches](docs/policies/branch_policy.md) | `develop`, prefixos e CI |
| [Política de testes](docs/policies/test_policy.md) | Níveis de teste, cobertura e ISO/IEC 29119 |
| [Escrita técnica](docs/policies/escrita_ste100.md) | Regras ASD-STE100 adaptadas ao português |
| [Collection Postman](docs/api/sajitar.postman_collection.json) | Import no Postman para `/tokens`, `/profiles` e `/notes` |

## Branches

| O que | Função |
| --- | --- |
| `develop` | Única branch longa, protegida e default. Recebe o merge do dia a dia. |
| `feat/*`, `fix/*` e os outros prefixos | Branch de trabalho criada a partir de `develop`. |
| `hotfix/*` | Correção de uma tag já publicada que não é o HEAD de `develop`. |
| GitHub Release `vX.Y.Z` | Snapshot imutável de um SHA de `develop`. |

O detalhe está na [política de branches](docs/policies/branch_policy.md) e na [política de testes](docs/policies/test_policy.md).

## Tecnologias

### Plataforma

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)

### Dados e persistência

![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18.6-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-8.2.9-FF4438?style=for-the-badge&logo=redis&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Hibernate](https://img.shields.io/badge/Hibernate-59666C?style=for-the-badge&logo=hibernate&logoColor=white)

### API, contrato e validação

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

### Ambiente de desenvolvimento

![Eclipse Temurin](https://img.shields.io/badge/Eclipse%20Temurin-25-FF6C00?style=for-the-badge&logo=eclipseadoptium&logoColor=white)
![pgAdmin](https://img.shields.io/badge/pgAdmin-326690?style=for-the-badge&logo=postgresql&logoColor=white)
![RedisInsight](https://img.shields.io/badge/RedisInsight-FF4438?style=for-the-badge&logo=redis&logoColor=white)
![Mailpit](https://img.shields.io/badge/Mailpit-v1.31.0-0055FF?style=for-the-badge)
![Spring DevTools](https://img.shields.io/badge/Spring%20DevTools-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
