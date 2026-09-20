---
name: show-local-vars
description: >-
  Gera as variáveis de ambiente do Sajitar Backend consumidas pelo Spring Boot
  (application.yml), no formato export VAR="valor", agrupadas e sem ruído de
  sistema. Use quando o usuário invocar /show-local-vars, pedir exports locais,
  variáveis para ~/.profile, ou filtrar printenv do container springboot.
disable-model-invocation: true
---

# Show local vars

Emite somente variáveis lidas por `application.yml` e pelo serviço
`springboot` do `docker-compose.yml`. Formato fixo:

```bash
export NOME="valor"
```

Valores sempre entre aspas duplas; linha em branco entre grupos.

## Execução

A partir da raiz do repositório:

```bash
.cursor/skills/show-local-vars/scripts/show-local-vars.sh [host|compose|filter]
```

| Alvo | Quando usar |
| --- | --- |
| `host` (padrão) | `./mvnw spring-boot:run` ou `./mvnw verify` na máquina host com Compose no ar (`localhost`) |
| `compose` | Mesmo conjunto que o container `sajitar-springboot` (IPs `10.0.0.x`) |
| `filter` | Entrada `KEY=VALUE` (ex.: `printenv`); descarta `HOSTNAME`, `PATH`, `JAVA_HOME`, etc. |

**Fonte:** `local.env` na raiz (valores base) + overrides do alvo. Credenciais
de Postgres derivam de `POSTGRES_*` quando `SPRING_DATASOURCE_*` não estiver
definido.

**Filtro a partir de container:**

```bash
docker exec sajitar-springboot printenv | .cursor/skills/show-local-vars/scripts/show-local-vars.sh filter
```

## Grupos e ordem

1. `SPRING_PROFILES_ACTIVE`
2. `SPRING_DATASOURCE_*`
3. `SPRING_JPA_*` e `SPRING_SQL_*`
4. `SPRING_DATA_REDIS_*`
5. `SAJITAR_DOMAIN_VALIDATION_*`
6. `SAJITAR_SECURITY_JWT_*`
7. `SAJITAR_SECURITY_ATTEMPT_*`
8. `SAJITAR_MAIL_*` (perfil `LOCAL`)

## O que não incluir

- Variáveis de shell/sistema (`HOSTNAME`, `PATH`, `PWD`, `TERM`, `JAVA_*`, …)
- Variáveis só do Compose auxiliar (`POSTGRES_*`, `PGADMIN_*`, `MP_*`)

`SAJITAR_SECURITY_JWT_SECRET` entra na saída porque está em `local.env` e no
Compose; hoje o segredo em `application.yml` está fixo no YAML — a env ainda
documenta o valor de dev.

## Resposta ao usuário

1. Rode o script com o alvo pedido (ou `host` se não especificado).
2. Cole a saída **integral** em bloco `bash`, sem comentários extras.
3. Se pediu filtro de `printenv`, use `filter`; se pediu exports para
   `~/.profile` no host, use `host`; se citou container Compose, use `compose`.

Não invente valores: se `local.env` mudou, regenere com o script.
