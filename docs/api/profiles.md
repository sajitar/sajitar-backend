# API `/profiles`

Query opcional **`lang`**: `en` (padrão), `pt` ou `es`. Omitida, vazia ou não suportada → inglês. Sem sessão e sem `Accept-Language`.

| Método | Caminho | Sucesso |
| --- | --- | --- |
| POST | `/profiles` | 200 + resumo (id, name, description; sem senha) |
| GET | `/profiles/{id}` | 200 + resumo |
| GET | `/profiles/{id}/details` | 200 + detalhes (sem senha) |
| PUT | `/profiles/{id}` | 200 + resumo; id só na URL; senha omitida mantém o hash |
| PATCH | `/profiles/{id}` | 200 + resumo; campos omitidos permanecem; `"description": null` limpa a descrição |
| DELETE | `/profiles/{id}` | 204; 404 se ausente (não é 204 idempotente) |
| GET | `/profiles` | 200 + página por cursor (`name`, `lastSeenName`, `lastSeenId`, `limit`, `reverse`; `precedingElements` / `followingElements`) |

Erros: **400** mapa campo→mensagens; **409** e-mail já registrado; **404** sem corpo. Detalhes no OpenAPI e na collection Postman.

A listagem **`GET /profiles`** pagina por cursor sobre nome e id. Exemplos de navegação também em `ProfileControllerIntegrationTest`.

Ver também: [checkers](checkers.md) · [authorities](authorities.md) · [notes](notes.md) · [comandos e URLs](../development/commands.md)
