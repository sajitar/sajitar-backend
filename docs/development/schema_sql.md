# Schema SQL

`SchemaProperties` lê `sajitar.schema.before`, `sajitar.schema.ddl-auto` e `sajitar.schema.after`.
Cada perfil Spring define esses valores no próprio YAML.

| Perfil Spring | `ddl-auto` | Lista after |
| --- | --- | --- |
| `LOCAL` e `DOCKER` | `create` | `settlement` |
| `DEMO` | `create` | `demo` |
| `LOCAL-TEST`, `CI` e `DOCKER-TEST` | `create-drop` | `settlement` |

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

A lista before é a mesma em todos os perfis Spring.

1. `util/functions.sql` cria as extensões, a função `purify` e a função `term_draft_requires_all_types`.
2. `util/enums.sql` cria `profile_type`, `checker_type`, `note_type` e `term_type` quando o tipo está ausente. O tipo existente permanece.

## Scripts after

| Perfil Spring | Arquivos, nesta ordem |
| --- | --- |
| `LOCAL`, `LOCAL-TEST`, `CI`, `DOCKER` e `DOCKER-TEST` | `util/columns.sql`, `util/uniques.sql`, `util/indexes.sql`, `settlement/profile.sql`, `settlement/checker.sql`, `settlement/note.sql`, `settlement/term.sql` |
| `DEMO` | `util/columns.sql`, `util/uniques.sql`, `util/indexes.sql`, `demo/profile.sql`, `demo/note.sql`, `demo/term.sql` |

`util/columns.sql` cria colunas geradas, CHECKs e FKs.
`util/columns.sql` liga o gatilho `term_draft_requires_all_types` à tabela `term_detail`.
O mesmo arquivo cria a FK `term_detail.term_id` para `term(id)` com `ON DELETE CASCADE`.
O mesmo arquivo cria a FK `profile.term_id` para `term(id)` com `ON DELETE SET NULL`.
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

`settlement/term.sql` grava quatro `id` de termo.
O primeiro `id` fica publicado em EN, PT e ES e é menor que o vigente.
O segundo `id` fica vigente em EN, PT e ES.
O terceiro `id` fica em rascunho nas três línguas.
O quarto `id` fica em rascunho só em EN.
`demo/term.sql` grava um `id` publicado antigo e um `id` vigente em EN, PT e ES.
A publicação insere a linha com `draft` true e depois faz `UPDATE` para `draft` false.
Cada `id` entra em `term` antes das linhas de `term_detail`.

Ver também [comandos e perfis Spring](commands.md).
