# API `/profiles`

A API cria, altera, consulta e exclui perfis.
O resumo traz `id`, `type`, `name` e `description`.
Os detalhes acrescentam `birthday`, `email` e `twoFactor`.
Nenhuma resposta traz a senha.

## Idioma

A query `lang` segue a regra de [`/notes`](notes.md).

## Autenticação

O access nasce em [`/tokens`](tokens.md).
O header é `Authorization: Bearer`.
O refresh não vale nesse header.

Estas rotas são públicas.

- `POST /profiles`
- `POST /profiles/password/recovery`
- `POST /profiles/password/confirm`

As outras rotas exigem access vigente no Redis.
Um Bearer ausente ou inválido responde 401 `{token}`.

`POST /profiles` trata Bearer inválido como chamada anônima.
Esse Bearer inválido não responde 401.
Bearer presente e Redis fora do ar respondem 503 sem corpo.
Só um access de caller `MASTER` grava o `type` enviado na criação.

## Tipo

O perfil tem um tipo.
`MASTER` inclui `WRITER` e `READER`.
`WRITER` inclui `READER`.
O nível não é persistido.
A omissão de `type` na criação grava `WRITER`.
Só um caller `MASTER` grava outro `type`.

## Visibilidade

`GET /profiles/{id}` esconde dois casos de quem não é `MASTER`.
O perfil com checker `VERIFY_EMAIL` responde 404 sem corpo.
O `READER` de outro perfil responde 404 sem corpo.
O próprio `READER` vê o próprio resumo.
O 404 não distingue perfil ausente de perfil oculto.

`GET /profiles/{id}/details` mostra o perfil ao dono.
Um caller `MASTER` também vê os detalhes.
Outro caller responde 403 `{id}`.
O detalhe inclui `termId` quando o perfil aceitou um termo.
`termId` nulo fica de fora do JSON.
O resumo e a lista não incluem `termId`.
`PUT` e `PATCH` não alteram `termId`.

A lista omite o perfil da sessão em `content` e nas contagens.
Quem não é `MASTER` não vê perfil com `VERIFY_EMAIL`.
Quem não é `MASTER` não vê `READER` alheio.
PUT e PATCH carregam o perfil pelo `id`.
PUT e PATCH não aplicam essas omissões.

## Operações

| Método | Caminho | Sucesso |
| --- | --- | --- |
| POST | `/profiles` | 200 e o resumo |
| POST | `/profiles/password` | 204 sem corpo |
| POST | `/profiles/password/recovery` | 204 sem corpo |
| POST | `/profiles/password/confirm` | 204 sem corpo |
| POST | `/profiles/email/recovery` | 204 sem corpo |
| POST | `/profiles/email/confirm` | 204 sem corpo |
| POST | `/profiles/email/change` | 204 sem corpo |
| POST | `/profiles/deletion` | 204 sem corpo |
| GET | `/profiles/{id}` | 200 e o resumo |
| GET | `/profiles/{id}/details` | 200 e os detalhes |
| PUT | `/profiles/{id}` | 200 e o resumo |
| PATCH | `/profiles/{id}` | 200 e o resumo |
| DELETE | `/profiles/{id}` | 204 sem corpo |
| GET | `/profiles` | 200 e a página |

### POST `/profiles`

O servidor gera o `id`.
O corpo não envia o `id`.
`type` omitido ou nulo grava `WRITER`.
Caller anônimo, Bearer inválido ou caller que não é `MASTER` ignora o `type` do corpo.
Caller `MASTER` grava o `type` enviado.
A API ignora `twoFactor` extra.
`WRITER` e `READER` nascem com `twoFactor` falso e sem `SIGN_IN`.
`MASTER` nasce com `twoFactor` verdadeiro.
A API cria o checker `VERIFY_EMAIL`.
A API envia o código de seis dígitos ao e-mail.
Falha de correio responde 503 sem corpo.
E-mail já registrado responde 409 `{email}`.

Enquanto `VERIFY_EMAIL` existir, o `signin` e o `refresh` seguem o contrato de [`/tokens`](tokens.md).
O reenvio do código é `POST /tokens/verification`.

### POST `/profiles/password`

A rota troca a senha do perfil do Bearer.
O corpo traz `currentPassword` e `newPassword`.
`signoutAllSessions` é opcional.
`true` encerra todas as sessões antes da escrita.
Omissão ou `false` mantém as sessões.
Senhas iguais respondem 400 `{newPassword}`.
Senha atual errada responde 401 `{credentials}`.
O limite responde 429 `{credentials}` com `Retry-After`.
PUT e PATCH não trocam a senha.

### POST `/profiles/password/recovery`

A rota é pública.
O corpo traz `email`.
A API cria ou gira `CHANGE_PASSWORD`.
O giro mantém o id UUIDv7.
O prazo não reabre.
A API envia o código de seis dígitos.
O código fica só no corpo do e-mail.
A rota responde 204 também sem e-mail nestes casos.

- O e-mail não existe.
- O perfil tem `VERIFY_EMAIL`.
- O checker passou de `sajitar.profile.change-password-max-age-minutes`.

O limite responde 429 `{credentials}` com `Retry-After`.
Falha de correio responde 503 sem corpo.
A rota trata `Authorization` como ausente.

### POST `/profiles/password/confirm`

A rota é pública.
O corpo traz `email`, `code` e `newPassword`.
Código conferido encerra as sessões e grava a senha.
O `wipe` ocorre antes da escrita.
Redis fora do ar responde 503 e o perfil permanece intacto.

| `code` | Resposta |
| --- | --- |
| Ausente ou mal formado | 400 `{code}` |
| Divergente, ausente, vencido, ou e-mail inexistente | 401 `{code}` |

O 401 mantém o código vigente.
O limite responde 429 `{credentials}` com `Retry-After`.

O job interno corre a cada 5 minutos.
O job apaga `CHANGE_PASSWORD` cujo UUIDv7 passou do prazo.
O job não apaga o perfil.
O job não encerra as sessões.

### POST `/profiles/email/recovery`

A rota exige Bearer.
O corpo é vazio.
A API cria ou gira `CHANGE_EMAIL` do perfil da sessão.
O giro zera o payload.
O giro mantém o id UUIDv7.
A API envia o código ao e-mail vigente.
Perfil com `VERIFY_EMAIL` responde 403 `{email}`.
Checker vencido responde 401 `{code}` e não gira.
O prazo é `sajitar.profile.change-email-max-age-minutes`.
O limite responde 429 `{credentials}` com `Retry-After`.
Falha de correio responde 503 sem corpo.

### POST `/profiles/email/confirm`

A rota exige Bearer.
O corpo traz `code` e `newEmail`.
A etapa exige payload nulo e código igual ao vigente.
A API grava `newEmail` no payload.
A API gira o código e envia o novo só para esse endereço.
O e-mail do perfil ainda não muda.

| Caso | Resposta |
| --- | --- |
| `code` mal formado | 400 `{code}` |
| `newEmail` mal formado ou igual ao vigente | 400 `{newEmail}` |
| Código divergente, ausente, vencido ou payload já preenchido | 401 `{code}` |
| `newEmail` de outro perfil | 409 `{email}` |

O 401 não gira o código.
O 409 não consome o código.
O limite responde 429 `{credentials}` com `Retry-After`.
Falha de correio responde 503 sem corpo.

### POST `/profiles/email/change`

A rota exige Bearer.
O corpo traz `code`.
Só o código enviado ao e-mail novo conclui a troca.
Payload presente e código conferido trocam o e-mail.
A API encerra as sessões antes da escrita.
A API apaga o checker depois da gravação.
Payload nulo ou código divergente responde 401 `{code}`.
E-mail de outro perfil responde 409 `{email}` e preserva o checker.
O limite responde 429 `{credentials}` com `Retry-After`.
Redis fora do ar responde 503 e o e-mail permanece o vigente.

O mesmo job de 5 minutos apaga `CHANGE_EMAIL` vencido.
O job não apaga o perfil.
O job não encerra as sessões.

### POST `/profiles/deletion`

A rota exige Bearer.
O corpo é vazio.
A API cria ou gira `DELETE_PROFILE`.
O giro mantém o id UUIDv7 e não reabre o prazo.
A API envia o código ao e-mail vigente.
Perfil com `VERIFY_EMAIL` responde 403 `{email}`.
Checker vencido responde 401 `{code}` e não gira.
O prazo é `sajitar.profile.delete-profile-max-age-minutes`.
O limite responde 429 `{credentials}` com `Retry-After`.
Falha de correio responde 503 sem corpo.

### DELETE `/profiles/{id}`

A rota exige Bearer e o corpo `{code}`.
O `id` da URL tem de ser o `profileId` da sessão.
A regra vale também para caller `MASTER`.
Outro `id` responde 403 `{id}` sem consultar o repositório.
O próprio `id` ausente responde 404 sem corpo.
A exclusão não é idempotente.

| `code` | Resposta |
| --- | --- |
| Ausente ou mal formado | 400 `{code}` |
| Divergente, ausente ou vencido | 401 `{code}` |
| Perfil ainda com `VERIFY_EMAIL` | 403 `{email}` |
| Conferido | 204, depois do `wipe` e do DELETE |

Os checkers desse `profile_id` saem no `ON DELETE CASCADE`.
O limite responde 429 `{credentials}` com `Retry-After`.

O mesmo job de 5 minutos apaga `DELETE_PROFILE` vencido.
O job não apaga o perfil.
O job não encerra as sessões.

### PUT `/profiles/{id}`

O `id` fica só na URL.
`type` e `twoFactor` são obrigatórios.
`email` extra é ignorado.
O e-mail muda só por `POST /profiles/email/recovery`.

`name`, `description` e `birthday` só o próprio perfil grava.
Caller alheio, inclusive `MASTER`, responde 403 `{id}`.

`twoFactor` o próprio perfil grava.
Caller `MASTER` grava `twoFactor` de alvo `WRITER` ou `READER`.
`twoFactor` de outro `MASTER` responde 403 `{id}`.
Estado `MASTER` com `twoFactor` falso responde 400 `{twoFactor}`.
Gravar `twoFactor` falso apaga o `SIGN_IN`, se existir.

Só caller `MASTER` troca o `type` para outro valor.
Outro caller responde 403 `{type}`.
O mesmo `type` responde 200.

Cruzar `MASTER` exige `password` do caller.
Cruzar `MASTER` é promover alguém a `MASTER` ou rebaixar um `MASTER`.
Senha ausente ou mal formada responde 400 `{password}`.
Senha errada responde 401 `{credentials}`.
O limite responde 429 `{credentials}` com `Retry-After`.
Fora dessa fronteira, a senha extra é ignorada.
`WRITER` para `READER` e o inverso ficam fora da fronteira.

### PATCH `/profiles/{id}`

Campo omitido permanece.
`type` omitido ou nulo mantém o vigente.
`twoFactor` omitido mantém o valor.
`twoFactor` nulo responde 400 `{twoFactor}`.
`"description": null` limpa a descrição.
`name` ou `birthday` nulos respondem 400.
As regras de dono, `twoFactor`, `type` e senha de `MASTER` são as do PUT.
Estado resultante `MASTER` com `twoFactor` falso responde 400 `{twoFactor}`.
`email` extra é ignorado.

### GET `/profiles`

A lista pagina por cursor de nome e id.
A query aceita `name`, `type`, `verified`, `lastSeenName`, `lastSeenId`, `limit` e `reverse`.
`name` filtra por trecho do nome, sem distinguir maiúsculas.
`limit` vale 100 quando a query o omite.
`limit` vai de 1 até `sajitar.domain.validation.limit.max`.
`reverse` vale `false` quando a query o omite.
A página traz `content`, `precedingElements`, `followingElements` e `reverse`.
Página vazia responde 404 sem corpo.

Caller `MASTER` filtra `type` com `MASTER`, `WRITER` ou `READER`.
`type` omitido lista todos os tipos visíveis.
Quem não é `MASTER` e envia `type` responde 403 `{type}`.
Valor desconhecido responde 400 `{type}`.

Caller `MASTER` filtra `verified`.
`true` lista perfil sem `VERIFY_EMAIL`.
`false` lista só perfil com esse checker.
`verified` omitido lista os dois conjuntos.
Quem não é `MASTER` e envia `verified` responde 403 `{verified}`.
Valor que não é booleano responde 400 `{verified}`.

## Erros

| Status | Quando |
| --- | --- |
| 400 | Validação. O corpo é um mapa de campo para mensagens. |
| 401 `{token}` | Bearer ausente ou inválido nas rotas protegidas |
| 401 `{credentials}` | Senha atual errada, ou senha do caller errada ao cruzar `MASTER` |
| 401 `{code}` | Código divergente, checker ausente ou checker vencido |
| 403 `{email}` | `VERIFY_EMAIL` na troca de e-mail ou na exclusão |
| 403 `{type}` | `type` diferente do vigente por quem não é `MASTER`, ou `type` na lista |
| 403 `{verified}` | `verified` na lista por quem não é `MASTER` |
| 403 `{id}` | Detalhes de outro perfil, atributo de outro perfil, ou DELETE de outro id |
| 404 | Perfil ausente, perfil oculto no GET ou página vazia. Sem corpo. |
| 409 `{email}` | E-mail já registrado na criação ou na troca |
| 429 `{credentials}` | Limite do escopo `CREDENTIALS`, com `Retry-After` |
| 503 | Redis indisponível no `wipe`, ou correio indisponível no envio |

O OpenAPI e a collection Postman repetem estes contratos.
Os exemplos HTTP estão em `ProfileControllerIntegrationTest`.

Ver também [tokens](tokens.md), [notes](notes.md), [terms](terms.md) e [comandos](../development/commands.md).
