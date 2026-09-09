# Sajitar Backend

API Spring Boot do Sajitar. Variáveis locais em `local.env` (versionado; só desenvolvimento).

## Documentação

| Documento | Conteúdo |
| --- | --- |
| [Comandos](docs/development/commands.md) | Docker Compose, imagem demo, Maven, `verify`, `spring-boot:run`, URLs úteis |
| [API `/profiles`](docs/api/profiles.md) | Contratos HTTP de perfil |
| [API `/checkers`](docs/api/checkers.md) | Contratos HTTP de checker |
| [API `/authorities`](docs/api/authorities.md) | Contratos HTTP de authority |
| [API `/notes`](docs/api/notes.md) | Contratos HTTP de note |
| [Schema SQL](docs/development/schema_sql.md) | Cadeia `util/*` e `settlement/*` após o DDL |
| [Política de branches](docs/policies/branch_policy.md) | Git Flow e regras de CI |
| [Política de testes](docs/policies/test_policy.md) | Níveis de teste e cobertura (ISO/IEC 29119) |
| [Collection Postman](docs/api/sajitar.postman_collection.json) | Import no Postman (`/profiles`, `/checkers`, `/authorities`, `/notes`) |

## Git Flow

| Branch | Função |
| --- | --- |
| `main` ou `master` | Código em produção (ou refletindo o que foi liberado). |
| `develop` ou `development` | Integração contínua do time; destino padrão do dia a dia. |
| `feat/*`, `fix/*`, … | Branches de trabalho partindo tipicamente de `develop`. |
| `release/*` | Preparação de versão (congelamento, ajustes finais) antes de ir a produção. |
| `hotfix/*` | Correção urgente em produção, normalmente ramificada a partir de `main`. |

> Detalhes na [política de branches](docs/policies/branch_policy.md) e na [política de testes](docs/policies/test_policy.md).

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
