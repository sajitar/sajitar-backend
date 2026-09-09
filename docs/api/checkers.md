# API `/checkers`

Query opcional **`lang`**: mesma regra de [`/profiles`](profiles.md). Tipos públicos no JSON: `CHANGE_EMAIL` (0), `VERIFY_EMAIL` (1, criação/troca restrita), `CHANGE_PASSWORD` (2). O campo `type` aceita o nome do enum ou o número; valores como `CHANGE_PHONE` / `VERIFY_PHONE` → **400**. A **escrita** aceita só `type` e `payload` no corpo (`profileId` só na query do POST). A resposta inclui `code` e `payload` (`payload` pode ser `null`). Cada alteração real de `type`/`payload` gera novo `code`, restaura `attempts` a 10 e decrementa `replaces`; sem mudança o servidor não grava.

| Método | Caminho | Sucesso |
| --- | --- | --- |
| POST | `/checkers?profileId=` | 200 + checker (`id`, `profileId`, `type`, `code`, `payload`, `replaces`, `attempts`, `updatedAt`, `requiredPayload`); corpo só `type` e `payload` opcional |
| GET | `/checkers/{id}` | 200 + checker completo |
| GET | `/checkers?profileId=&type=` | 200 + um registro do par (perfil, tipo) |
| GET | `/checkers?profileId=&lastSeenType=&limit=&reverse=` | 200 + página `{content, precedingElements, followingElements, reverse}` (cursor por tipo na query) |
| PUT | `/checkers/{id}` | 200; id só na URL; corpo `type` (obrigatório) e `payload` (omitido/nulo limpa); alteração real consome 1 replace |
| PATCH | `/checkers/{id}` | 200; só `type`/`payload`; omitir `payload` mantém; `"payload": null` limpa; alteração real consome 1 replace |
| DELETE | `/checkers/{id}` | 204; 404 se ausente (não é 204 idempotente) |

Erros: **400** mapa campo→mensagens (validação, tipo desconhecido ou `replaces` esgotado); **403** criar ou mudar o type para `VERIFY_EMAIL`; **409** já existe o tipo para o perfil; **404** checker ausente sem corpo; **404** perfil inexistente no POST **com** corpo `{profileId:[…]}`; lista vazia → **404**. Detalhes no OpenAPI e na collection Postman.

A listagem **`GET /checkers`** (sem `type`) pagina por cursor sobre o tipo (`limit`, `reverse`). Exemplos também em `CheckerControllerIntegrationTest`.

Ver também: [profiles](profiles.md) · [authorities](authorities.md) · [notes](notes.md) · [comandos e URLs](../development/commands.md)
