# API `/profiles`

Query opcional **`lang`**: `en` (padrão), `pt` ou `es`. Omitida, vazia ou não suportada → inglês. Sem sessão e sem `Accept-Language`.

Autenticação **JWT HS256** (`Authorization: Bearer` com access token, claim `token_use=access`). Públicos: `POST /profiles`, `POST /profiles/signin` e `POST /profiles/refresh` — um `Authorization` Basic ou Bearer inválido é ignorado. Demais rotas de `/profiles` exigem access Bearer válido. Refresh JWT **não** vale no header. O token autentica o perfil; nesta entrega **não** restringe o dono do recurso. Sem store: o refresh antigo permanece válido até o próprio `exp` (logout/revogação por token exigiria persistência, fora desta entrega).

| Método | Caminho | Sucesso |
| --- | --- | --- |
| POST | `/profiles` | 200 + resumo (id, name, description; sem senha) |
| POST | `/profiles/signin` | 200 + `{ token, type: "Bearer", expiresIn, refreshToken, refreshExpiresIn }` |
| POST | `/profiles/refresh` | 200 + o mesmo par; corpo `{ refreshToken }` (não usar `Authorization`) |
| GET | `/profiles/{id}` | 200 + resumo |
| GET | `/profiles/{id}/details` | 200 + detalhes (sem senha) |
| PUT | `/profiles/{id}` | 200 + resumo; id só na URL; senha omitida mantém o hash |
| PATCH | `/profiles/{id}` | 200 + resumo; campos omitidos permanecem; `"description": null` limpa a descrição |
| DELETE | `/profiles/{id}` | 204; 404 se ausente (não é 204 idempotente) |
| GET | `/profiles` | 200 + página por cursor (`name`, `lastSeenName`, `lastSeenId`, `limit`, `reverse`; `precedingElements` / `followingElements`) |

Erros: **400** mapa campo→mensagens; **401** credenciais inválidas `{credentials:[…]}` no signin; refresh ausente/malformado/expirado/`token_use` ≠ `refresh` / perfil sumiu `{refreshToken:[…]}`; Bearer ausente/inválido `{token:[…]}`; **403** e-mail não verificado `{email:[…]}` no signin e no refresh quando o perfil tem checker `VERIFY_EMAIL`; **409** e-mail já registrado; **404** sem corpo. Detalhes no OpenAPI e na collection Postman.

A listagem **`GET /profiles`** pagina por cursor sobre nome e id. Exemplos de navegação também em `ProfileControllerIntegrationTest`.

Ver também: [checkers](checkers.md) · [authorities](authorities.md) · [notes](notes.md) · [comandos e URLs](../development/commands.md)
