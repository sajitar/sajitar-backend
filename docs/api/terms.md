# API `/terms`

A API grava e consulta as linhas de termos de uso.
Várias linhas compartilham o mesmo `id`.
As linhas de um `id` diferem no `type`.
A chave é o par `id` e `type`.

O servidor gera o `id`.
`id` e `type` permanecem os da criação.
Cada linha guarda `content` e `draft`.

## Idioma

GET `/terms` e GET `/terms/{id}` usam a query `type`.
PATCH `/terms/{id}` também usa a query `type`.
Os valores aceitos são `EN`, `PT` e `ES`.
A omissão de `type` em GET pede todas as linhas.
Um `type` fora desses três responde 400 `{type}`.

GET `/terms/current` usa a query `lang`.
Os valores aceitos são `en`, `pt` e `es`.
A omissão de `lang` pede todas as linhas do vigente.
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
`content` é obrigatório no POST.
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

A rota altera uma linha.
O `id` fica na URL.
A query `type` é obrigatória.
A query ausente ou inválida responde 400 `{type}`.
Um `id` ausente responde 404 sem corpo.
A linha do `type` ausente responde 400 `{type}`.

O corpo traz `content`, `draft` ou os dois.
Um `type` no corpo fica sem efeito.

| Corpo | Efeito |
| --- | --- |
| `content` com texto | A rota grava esse texto |
| `content` ausente | A rota mantém o texto gravado |
| `content` nulo, vazio ou em branco | 400 `{content}` |
| `draft` ausente | A rota mantém o `draft` gravado |
| `draft` true | A rota grava `draft` true |
| `draft` false com `EN`, `PT` e `ES` | A rota grava `draft` false nessa linha |
| `draft` false sem as três línguas | 400 `{draft}` |
| `draft` nulo | 400 `{draft}` |
| `content` e `draft` ausentes | 400 `{content}` e `{draft}` |

`content` inválido usa a chave `validation.not-blank`.
`draft` nulo usa a chave `validation.not-null`.
A publicação incompleta usa a chave `validation.term.draft.incomplete`.

O `id` fica fechado quando `EN`, `PT` e `ES` têm `draft` false.
Esse pedido responde 400 `{draft}`.
A mensagem é `validation.term.draft.closed`.
A rota não grava a linha.
Um corpo inválido conserva essa resposta.

A linha publicada ainda aceita o pedido enquanto outra língua é rascunho.
`draft` ausente mantém `draft` false nessa linha.
`draft` true grava `draft` true nessa linha.
`content` presente troca o texto dessa linha.

### GET `/terms/{id}`

A rota devolve as linhas de um `id`.
Sem a query `type`, a resposta traz `id` e `contents`.
Cada item de `contents` traz `type`, `content` e `draft`.
Com a query `type`, a resposta traz `id`, `type`, `content` e `draft`.

Um `type` pedido que o `id` não tem responde 400 `{type}`.
Um `type` inválido responde 400 `{type}`.
Um `id` ausente responde 404 sem corpo.
Um `id` que não é UUID responde 400 `{id}`.

### GET `/terms`

A listagem pagina por cursor sobre o `id`.
A query aceita `type`, `lastSeenId`, `limit` e `reverse`.
Sem `type`, cada item traz `id` e `contents`.
Com `type`, cada item traz `id`, `type`, `content` e `draft`.
Com `type`, a página inclui só os `id` que têm a linha.
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
