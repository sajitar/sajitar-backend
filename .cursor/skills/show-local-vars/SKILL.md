---
name: show-local-vars
description: >-
  Gera as variáveis de ambiente do Sajitar Backend que application.yml
  e o serviço springboot leem. A saída é export VAR="valor", em grupos,
  sem variável de sistema. Use quando o usuário invocar /show-local-vars,
  pedir exports locais, variáveis para ~/.profile, ou filtrar printenv
  do container springboot.
disable-model-invocation: true
---

# Show local vars

Emita só as variáveis que `application.yml` e o serviço `springboot` leem.
O formato é fixo.

```bash
export NOME="valor"
```

Coloque cada valor entre aspas duplas.
Deixe uma linha em branco entre grupos.

## Execução

Rode o script a partir da raiz do repositório.

```bash
.cursor/skills/show-local-vars/scripts/show-local-vars.sh [host|compose|filter]
```

| Alvo | Quando usar |
| --- | --- |
| `host` | Padrão. Vale para `./mvnw spring-boot:run` ou `./mvnw verify` no host, com o Compose no ar e o banco em `localhost`. |
| `compose` | O mesmo conjunto do container `sajitar-springboot`. Os hosts são `10.0.0.x`. |
| `filter` | A entrada é `KEY=VALUE`, por exemplo `printenv`. O script descarta `HOSTNAME`, `PATH` e `JAVA_HOME`. |

A fonte é `local.env` na raiz.
O alvo aplica os overrides depois desses valores.
Se `SPRING_DATASOURCE_*` não existir, a credencial do Postgres sai de `POSTGRES_*`.

Para filtrar o ambiente do container:

```bash
docker exec sajitar-springboot printenv | .cursor/skills/show-local-vars/scripts/show-local-vars.sh filter
```

## Grupos

Emita os grupos nesta ordem.

1. `SPRING_PROFILES_ACTIVE`
2. `SPRING_DATASOURCE_*`
3. `SPRING_JPA_*` e `SPRING_SQL_*`
4. `SPRING_DATA_REDIS_*`
5. `SAJITAR_DOMAIN_VALIDATION_*`
6. `SAJITAR_SECURITY_JWT_*`
7. `SAJITAR_SECURITY_ATTEMPT_*`
8. `SAJITAR_MAIL_*`, no perfil `LOCAL`

## Fora da saída

Omita variável de shell, como `HOSTNAME`, `PATH`, `PWD`, `TERM` e `JAVA_*`.
Omita variável só do Compose auxiliar, como `POSTGRES_*`, `PGADMIN_*` e `MP_*`.

Inclua `SAJITAR_SECURITY_JWT_SECRET` na saída.
A variável está em `local.env` e no Compose.
O `application.yml` grava o segredo direto no YAML.
A variável ainda documenta o valor de desenvolvimento.

## Resposta

1. Rode o script com o alvo pedido.
2. Sem alvo, use `host`.
3. Cole a saída inteira num bloco `bash`.
4. Não acrescente comentário nesse bloco.
5. Para filtrar `printenv`, use `filter`.
6. Para exports de `~/.profile` no host, use `host`.
7. Para o container do Compose, use `compose`.

Não invente valor.
Se `local.env` mudou, rode o script de novo.
