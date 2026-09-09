# API `/authorities`

Query opcional **`lang`**: mesma regra de [`/profiles`](profiles.md). Tipos no JSON: `MASTER` (0), `MEMBER` (1), `READER` (2). O campo `type` aceita o nome do enum ou o número; valores desconhecidos → **400**. A **escrita** aceita só `type` no corpo (`profileId` só na query do POST). A resposta inclui `id`, `profileId` e `type`. Sem mudança real o servidor não grava.

| Método | Caminho | Sucesso |
| --- | --- | --- |
| POST | `/authorities?profileId=` | 200 + authority (`id`, `profileId`, `type`); corpo só `type` |
| GET | `/authorities/{id}` | 200 + authority |
| GET | `/authorities?profileId=&type=` | 200 + um registro do par (perfil, tipo) |
| GET | `/authorities?profileId=&lastSeenType=&limit=&reverse=` | 200 + página `{content, precedingElements, followingElements, reverse}` (cursor por tipo na query) |
| PUT | `/authorities/{id}` | 200; id só na URL; corpo `type` obrigatório |
| PATCH | `/authorities/{id}` | 200; só `type`; omitido ou `null` mantém |
| DELETE | `/authorities/{id}` | 204; 404 se ausente (não é 204 idempotente) |

Erros: **400** mapa campo→mensagens (validação ou tipo desconhecido); **409** já existe o tipo para o perfil; **404** authority ausente sem corpo; **404** perfil inexistente no POST **com** corpo `{profileId:[…]}`; lista vazia → **404**. Detalhes no OpenAPI e na collection Postman.

A listagem **`GET /authorities`** (sem `type`) pagina por cursor sobre o tipo (`limit`, `reverse`). Exemplos também em `AuthorityControllerIntegrationTest`.

Ver também: [profiles](profiles.md) · [checkers](checkers.md) · [notes](notes.md) · [comandos e URLs](../development/commands.md)
