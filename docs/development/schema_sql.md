# Schema SQL (antes e depois do DDL do Hibernate)

Todos os perfis usam `SchemaBeforeConfiguration` e `SchemaAfterConfiguration` (não `spring.sql.init` nem `hbm2ddl.import_files`). As listas vêm de `sajitar.schema.before` e `sajitar.schema.after` em cada `application-*.yml`.

`SchemaBeforeConfiguration` (separador `;;`) executa a lista **before** — `util/functions.sql` (extensões e `purify`) → `util/enums.sql` (`profile_type`, `checker_type`, `note_type` — cria se faltar e inclui rótulo com `ADD VALUE IF NOT EXISTS`) — **antes** do Hibernate (`EntityManagerFactory` depende de `schemaBefore`).

Depois do DDL (`create` em LOCAL/DOCKER/DEMO, `create-drop` em LOCAL-TEST/DOCKER-TEST/CI, `update` em PROD — não `validate`/`never`, e PROD nunca `create`/`create-drop`), `SchemaAfterConfiguration` (separador `;`, fail closed) executa a lista **after**. LOCAL/LOCAL-TEST/CI/DOCKER/DOCKER-TEST: `util/columns.sql` → `util/uniques.sql` → `util/indexes.sql` → settlement. DEMO: os três `util/*` e `demo/profile.sql` / `demo/note.sql`. PROD: só os três `util/*`, sem povoamento. As unicidades não ficam em anotações JPA.

A massa `settlement/profile.sql` usa a mesma senha em texto plano **`senhaSegura1`** em todos os perfis (BCrypt no INSERT). Credencial só de desenvolvimento e IT, não de produção. Alice (`alice@example.com`) ainda tem checker `VERIFY_EMAIL` no seed (`234567` em `settlement/checker.sql`); o signin dela exige `code` até esse checker ser consumido.

Ver também: [comandos](commands.md).
