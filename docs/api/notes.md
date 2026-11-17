# API `/notes`

A API grava e consulta notas de um perfil.
Um perfil pode ter várias notas.
Duas notas do mesmo perfil podem ter o mesmo `type`.
A resposta traz `id`, `profileId`, `type` e `content`.
O servidor gera o `id`.
`id` e `profileId` permanecem os da criação.

## Idioma

A query `lang` é opcional.
Os valores aceitos são `en`, `pt` e `es`.
A omissão usa inglês.
Um valor vazio usa inglês.
Um valor fora da lista usa inglês.
A API escolhe o idioma só pela query `lang`.

## Autenticação

Toda rota exige `Authorization: Bearer` com access token.
O access nasce em [`POST /tokens/signin`](tokens.md).
Um Bearer ausente responde 401 `{token}`.
Um Bearer inválido responde 401 `{token}`.

## Tipo e conteúdo

Os valores de `type` são `PUBLIC`, `PROTECTED` e `PRIVATE`.
A API aceita só o nome do enum.
Um valor desconhecido responde 400 `{type}`.
Um número no JSON também responde 400 `{type}`.
O corpo de escrita aceita só `type` e `content`.
Um campo extra no corpo é ignorado.

`content` é obrigatório na criação.
Um `content` em branco responde 400 `{content}`.
`content` tem no máximo 1000 caracteres.
PUT e PATCH mantêm o registro quando o valor novo é igual ao atual.

## Operações

| Método | Caminho | Sucesso |
| --- | --- | --- |
| POST | `/notes?profileId=` | 200 e a nota |
| GET | `/notes/{id}` | 200 e a nota |
| GET | `/notes` | 200 e a página |
| PUT | `/notes/{id}` | 200 e a nota |
| PATCH | `/notes/{id}` | 200 e a nota |
| DELETE | `/notes/{id}` | 204 sem corpo |

### POST `/notes`

O `profileId` vai na query.
O corpo traz `type` e `content`.
A omissão de `profileId` responde 400 `{profileId}`.
Um perfil ausente responde 404 `{profileId}`.

### GET `/notes/{id}`

A rota devolve a nota do `id`.
Uma nota ausente responde 404 sem corpo.
Um `id` que não é UUID responde 400 `{id}`.

### GET `/notes`

A listagem pagina por cursor sobre o `id`.
A query aceita `profileId`, `type`, `lastSeenId`, `limit` e `reverse`.
`profileId` e `type` são filtros opcionais.
Sem `profileId`, a página inclui notas de todos os perfis.
O filtro `type` mantém a resposta como página.
`limit` vale 100 quando a query o omite.
`limit` vai de 1 até `sajitar.domain.validation.limit.max`.
`reverse` vale `false` quando a query o omite.
`reverse` com `true` ordena do maior `id` para o menor.
A página JSON traz só `content`, `precedingElements`, `followingElements` e `reverse`.
Uma página vazia responde 404 sem corpo.

### PUT `/notes/{id}`

O `id` fica só na URL.
O corpo exige `type` e `content`.
Uma nota ausente responde 404 sem corpo.

### PATCH `/notes/{id}`

O `id` fica só na URL.
A omissão de `content` mantém o texto.
A omissão de `type` mantém o tipo.
`type` nulo mantém o tipo.
`"content": null` responde 400 `{content}`.
`content` em branco responde 400 `{content}`.
Uma nota ausente responde 404 sem corpo.

### DELETE `/notes/{id}`

A rota remove a nota do `id`.
Uma nota ausente responde 404 sem corpo.

## Erros

| Status | Quando |
| --- | --- |
| 400 | Validação, tipo desconhecido ou UUID inválido. O corpo é um mapa de campo para mensagens. |
| 401 | Bearer ausente ou inválido. O corpo é `{token}`. |
| 404 | Nota ausente, página vazia, ou perfil ausente no POST. O POST traz `{profileId}`. Os outros 404 não trazem corpo. |

O OpenAPI e a collection Postman repetem estes contratos.
Os exemplos HTTP estão em `NoteControllerIntegrationTest`.

Ver também [profiles](profiles.md), [tokens](tokens.md) e [comandos](../development/commands.md).
