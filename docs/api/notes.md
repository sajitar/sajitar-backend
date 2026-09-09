# API `/notes`

Query opcional **`lang`**: mesma regra de [`/profiles`](profiles.md). Tipos no JSON: `PUBLIC` (0), `PROTECTED` (1), `PRIVATE` (2). O campo `type` aceita o nome do enum ou o número; valores desconhecidos → **400**. A **escrita** aceita só `type` e `content` no corpo (`profileId` só na query do POST). A resposta inclui `id`, `profileId`, `type` e `content`. Um perfil pode ter várias notas, inclusive do mesmo tipo. `content` é obrigatório (não em branco, no máximo 1000 caracteres). Sem mudança real o servidor não grava.

| Método | Caminho | Sucesso |
| --- | --- | --- |
| POST | `/notes?profileId=` | 200 + note (`id`, `profileId`, `type`, `content`); corpo `type` e `content` |
| GET | `/notes/{id}` | 200 + note |
| GET | `/notes?profileId=&type=&lastSeenId=&limit=&reverse=` | 200 + página `{content, precedingElements, followingElements, reverse}` (`profileId` e `type` são filtros opcionais; sem `profileId` lista notas de todos os perfis; cursor por `id`) |
| PUT | `/notes/{id}` | 200; id só na URL; corpo `type` e `content` obrigatórios |
| PATCH | `/notes/{id}` | 200; só `type`/`content`; omitir `content` mantém; `"content": null` ou vazio → 400 |
| DELETE | `/notes/{id}` | 204; 404 se ausente (não é 204 idempotente) |

Erros: **400** mapa campo→mensagens (validação ou tipo desconhecido); **404** note ausente sem corpo; **404** perfil inexistente no POST **com** corpo `{profileId:[…]}`; lista vazia → **404**. Detalhes no OpenAPI e na collection Postman.

A listagem **`GET /notes`** pagina por cursor sobre o `id` (`limit`, `reverse`). `profileId` e `type` são filtros opcionais: informar `profileId` restringe a página às notas do perfil; omiti-lo lista notas de todos os perfis. Não devolve um único registro. Exemplos também em `NoteControllerIntegrationTest`.

Ver também: [profiles](profiles.md) · [checkers](checkers.md) · [authorities](authorities.md) · [comandos e URLs](../development/commands.md)
