# Schema SQL

`SchemaProperties` lê `sajitar.schema.before`, `sajitar.schema.ddl-auto` e `sajitar.schema.after`.
No `application.yml`, esses valores vêm de três variáveis de ambiente.

| Propriedade | Variável |
| --- | --- |
| `sajitar.schema.before` | `SPRING_SQL_BEFORE_FRAMEWORK` |
| `sajitar.schema.ddl-auto` | `SPRING_JPA_HIBERNATE_DDL_AUTO` |
| `sajitar.schema.after` | `SPRING_SQL_AFTER_FRAMEWORK` |

## Ordem na subida

`SchemaBeforeConfiguration` executa a lista before no `DataSource`.
O bean se chama `schemaBefore`.
O separador do script é `;;`.
O encoding é UTF-8.
O `EntityManagerFactory` depende de `schemaBefore`.

`SchemaDdlConfiguration` grava `hibernate.hbm2ddl.auto` com o valor de `ddl-auto`.
O Hibernate aplica esse valor ao criar o `EntityManagerFactory`.

`SchemaAfterConfiguration` executa a lista after.
O bean se chama `schemaAfter`.
O separador do script é `;`.
O bean `schemaAfter` só corre depois de `entityManagerFactory`.

## Falha na subida

Uma lista before vazia interrompe a subida.
Uma lista after vazia interrompe a subida.
Um `ddl-auto` em branco interrompe a subida.
Um `ddl-auto` fora das ações do Hibernate interrompe a subida.
Uma falha de script também interrompe a subida.
A aplicação não usa `spring.sql.init`.
A aplicação não usa `spring.jpa.hibernate.ddl-auto`.
A aplicação não usa `hbm2ddl.import_files`.

## Scripts before

A lista before é a mesma no `local.env`, no CI e na imagem de demonstração.

1. `util/functions.sql` cria as extensões e a função `purify`.
2. `util/enums.sql` cria `profile_type`, `checker_type` e `note_type`.

## Scripts after

| Ambiente | Arquivos, nesta ordem |
| --- | --- |
| `local.env`, CI e Compose | `util/columns.sql`, `util/uniques.sql`, `util/indexes.sql`, `settlement/profile.sql`, `settlement/checker.sql`, `settlement/note.sql` |
| Imagem de demonstração | `util/columns.sql`, `util/uniques.sql`, `util/indexes.sql`, `demo/profile.sql`, `demo/note.sql` |

`util/columns.sql` cria colunas geradas, CHECKs e FKs.
`util/uniques.sql` cria a unicidade do e-mail do perfil.
O mesmo arquivo cria a unicidade do par `profile_id` e `type` do checker.
O repositório não coloca essas unicidades em anotações JPA.
O repositório não define a lista after de produção.

## Massa `settlement`

`settlement/profile.sql` grava a mesma senha em texto plano em todos os perfis.
A senha é `senhaSegura1`.
O INSERT grava o hash BCrypt.
Essa credencial vale para desenvolvimento e teste.
Alice usa o e-mail `alice@example.com`.
`settlement/checker.sql` grava o checker `VERIFY_EMAIL` de Alice com o código `234567`.
O `signin` de Alice exige `code` enquanto esse checker existir.

Ver também [comandos e variáveis de ambiente](commands.md).
