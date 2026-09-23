# Schema SQL (após o DDL do Hibernate)

Cadeia em `SPRING_SQL_AFTER_FRAMEWORK`: `util/columns.sql` (colunas geradas, CHECKs, FKs) → `util/uniques.sql` (e-mail do perfil; par `profile_id`+`type` do checker e da authority) → `util/indexes.sql` → `settlement/profile.sql`, `settlement/checker.sql`, `settlement/authority.sql` e `settlement/note.sql`. Funções em `util/functions.sql` rodam **antes**, via `SPRING_SQL_BEFORE_FRAMEWORK`. As unicidades não ficam em anotações JPA.

A massa `settlement/profile.sql` usa a mesma senha em texto plano **`senhaSegura1`** em todos os perfis (BCrypt no INSERT). Credencial só de desenvolvimento e IT, não de produção. Alice (`alice@example.com`) ainda tem checker `VERIFY_EMAIL` no seed (`234567` em `settlement/checker.sql`); o signin dela exige `code` até esse checker ser consumido.

Ver também: [comandos e variáveis de ambiente](commands.md).

