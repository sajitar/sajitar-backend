# API `/terms`

A API grava e consulta as linhas de termos de uso.
Várias linhas compartilham o mesmo `id`.
As linhas de um `id` diferem no `type`.
A chave é o par `id` e `type`.

O servidor gera o `id`.
`id` e `type` permanecem os da criação.
Cada linha guarda `content` e `draft`.

## Idioma

A query `lang` escolhe a linha.
Os valores aceitos são `en`, `pt` e `es`.
A omissão de `lang` pede todas as linhas do `id`.
Uma `lang` inválida responde 400 `{lang}`.
A query `lang` não cai para inglês na escolha da linha.

## Autenticação

Toda rota, exceto `GET /terms/current`, exige `Authorization: Bearer` com access token.
O access nasce em [`POST /tokens/signin`](tokens.md).
Um Bearer ausente responde 401 `{token}`.
Um Bearer inválido responde 401 `{token}`.
Essas rotas exigem o perfil cujo `type` inclui `MASTER`.
Um perfil sem `MASTER` responde 403 `{id}`.

`GET /terms/current` é pública e dispensa Bearer.
Um Bearer inválido não gera 401 nessa rota.
Um Redis indisponível responde 503 sem corpo nas rotas protegidas.

## Tipo e conteúdo

Os valores de `type` são `EN`, `PT` e `ES`.
A API aceita só o nome do enum.
Um `type` desconhecido responde 400 `{type}`.
`content` é obrigatório.
Um `content` em branco responde 400 `{content}`.
`content` não tem teto de caracteres.

`draft` é um booleano.
Uma linha nasce com `draft` true.
A publicação troca `draft` para false.

## Operações

| Método | Caminho | Sucesso |
| --- | --- | --- |
| POST | `/terms` | 200 e a linha |
| POST | `/terms/{id}` | 200 e a linha |
| PATCH | `/terms/{id}` | 200 e a linha |
| GET | `/terms/{id}` | 200 e o Term |
| GET | `/terms` | 200 e a página |
| GET | `/terms/current` | 200 e o vigente |
| DELETE | `/terms/{id}` | 204 sem corpo |

### POST `/terms`

A rota cria a primeira linha de um `id` novo.
O servidor gera o `id`.
O corpo traz `type` e `content`.
A linha nasce com `draft` true.
A resposta traz `id`, `type`, `content` e `draft`.

### POST `/terms/{id}`

A rota acrescenta outra língua a um `id` existente.
O `id` fica só na URL.
O corpo traz `type` e `content`.
A nova linha nasce com `draft` true.
Um `id` ausente responde 404 sem corpo.
O par `id` e `type` já gravado responde 409 sem corpo.

### PATCH `/terms/{id}`

A rota altera uma linha com `draft` true.
O `id` fica só na URL.
O corpo exige `type` e `content`.
O corpo aceita `draft` opcional.
Um `id` ausente responde 404 sem corpo.
Um `type` que o `id` não tem responde 400 `{type}`.

`draft` ausente ou `true` mantém o rascunho.
`draft` `false` publica a linha.
A publicação exige EN, PT e ES no mesmo `id`.
A publicação incompleta responde 400 `{draft}`.
Uma linha já publicada recusa o patch com 400 `{draft}`.
`draft` nulo responde 400 `{draft}`.

### GET `/terms/{id}`

A rota devolve as linhas de um `id`.
Sem a query `lang`, a resposta traz `id` e `contents`.
Cada item de `contents` traz `type`, `content` e `draft`.
Com a query `lang`, a resposta traz `id`, `type`, `content` e `draft`.

Um `type` pedido que o `id` não tem responde 400 `{type}`.
Um `id` ausente responde 404 sem corpo.
Um `id` que não é UUID responde 400 `{id}`.

### GET `/terms`

A listagem pagina por cursor sobre o `id`.
A query aceita `lang`, `lastSeenId`, `limit` e `reverse`.
Sem `lang`, cada item traz `id` e `contents`.
Com `lang`, cada item traz `id`, `type`, `content` e `draft`.
Com `lang`, a página inclui só os `id` que têm o `type`.
A página conta `id`, não linhas.

`limit` vale 100 quando a query o omite.
`limit` vai de 1 até `sajitar.domain.validation.limit.max`.
`reverse` vale `false` quando a query o omite.
`reverse` com `true` ordena do maior `id` para o menor.
A página JSON traz só `content`, `precedingElements`, `followingElements` e `reverse`.
Uma página vazia responde 404 sem corpo.

### GET `/terms/current`

A rota retorna o maior `id` com EN, PT e ES publicados.
A rota é pública e dispensa Bearer.
O corpo omite `draft`.
Sem a query `lang`, a resposta traz `id` e `contents`.
Com a query `lang`, a resposta traz `id`, `type` e `content`.
Sem vigente, a rota responde 404 sem corpo.

### DELETE `/terms/{id}`

Aviso: a remoção apaga todas as linhas do `id`.
A rota remove todas as linhas do `id`.
Um `id` ausente responde 404 sem corpo.
Um `id` que não é UUID responde 400 `{id}`.

## Erros

| Status | Quando |
| --- | --- |
| 400 | Validação, `type` ou `lang` inválido, ou UUID inválido. O corpo é um mapa de campo para mensagens. |
| 401 | Bearer ausente ou inválido numa rota protegida. O corpo é `{token}`. |
| 403 | Perfil sem `MASTER`. O corpo é `{id}`. |
| 404 | `id` ausente, página vazia, ou sem vigente. Sem corpo. |
| 409 | Par `id` e `type` já gravado no `POST /terms/{id}`. Sem corpo. |
| 503 | Redis indisponível na validação do Bearer. Sem corpo. |

O OpenAPI e a collection Postman repetem estes contratos.
Os exemplos HTTP estão em `TermControllerIntegrationTest`.

Ver também [notes](notes.md), [profiles](profiles.md), [tokens](tokens.md) e [comandos](../development/commands.md).
