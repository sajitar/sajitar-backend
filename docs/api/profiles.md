# API `/profiles`

Query opcional **`lang`**: `en` (padrão), `pt` ou `es`. Omitida, vazia ou não suportada → inglês. Sem sessão e sem `Accept-Language`.

Autenticação **JWT HS256** (`Authorization: Bearer` com access token, claim `token_use=access`) emitida em [`/tokens`](tokens.md). Público: `POST /profiles` — um `Authorization` Basic ou Bearer inválido é ignorado. Demais rotas de `/profiles` exigem access Bearer válido, isto é, assinado **e** com registro ativo no Redis. Refresh JWT **não** vale no header. O token autentica o perfil; nesta entrega **não** restringe o dono do recurso.

| Método | Caminho | Sucesso |
| --- | --- | --- |
| POST | `/profiles` | 200 + resumo (id, name, description; sem senha) |
| GET | `/profiles/{id}` | 200 + resumo |
| GET | `/profiles/{id}/details` | 200 + detalhes (sem senha) |
| PUT | `/profiles/{id}` | 200 + resumo; id só na URL; senha omitida mantém o hash |
| PATCH | `/profiles/{id}` | 200 + resumo; campos omitidos permanecem; `"description": null` limpa a descrição |
| DELETE | `/profiles/{id}` | 204; 404 se ausente (não é 204 idempotente) |
| GET | `/profiles` | 200 + página por cursor (`name`, `lastSeenName`, `lastSeenId`, `limit`, `reverse`; `precedingElements` / `followingElements`) |

Erros: **400** mapa campo→mensagens; **401** Bearer ausente ou inválido `{token:[…]}`; **409** e-mail já registrado; **404** sem corpo; **503** store de sessões indisponível. Detalhes no OpenAPI e na collection Postman.

Trocar a senha (PUT ou PATCH com senha nova) e excluir o perfil encerram **todas** as sessões daquele perfil em [`/tokens`](tokens.md), inclusive a corrente: o access deixa de valer na hora, sem esperar o `exp`. Como o encerramento precede a escrita, Redis fora do ar responde **503** com o perfil intacto.

A listagem **`GET /profiles`** pagina por cursor sobre nome e id. Exemplos de navegação também em `ProfileControllerIntegrationTest`.

Ver também: [tokens](tokens.md) · [checkers](checkers.md) · [authorities](authorities.md) · [notes](notes.md) · [comandos e URLs](../development/commands.md)
