# API `/profiles`

Query opcional **`lang`**: `en` (padrão), `pt` ou `es`. Omitida, vazia ou não suportada → inglês. Sem sessão e sem `Accept-Language`.

Autenticação **JWT HS256** (`Authorization: Bearer` com access token, claim `token_use=access`) emitida em [`/tokens`](tokens.md). Público: `POST /profiles`, `POST /profiles/password/recovery` e `POST /profiles/password/confirm` — um `Authorization` Basic ou Bearer inválido é ignorado. Demais rotas de `/profiles` exigem access Bearer válido, isto é, assinado **e** com registro ativo no Redis. Refresh JWT **não** vale no header. O token autentica o perfil. GET por id, details e lista **não** devolvem perfil que ainda tem checker `VERIFY_EMAIL`, salvo o caller com autoridade `MASTER` (404 sem corpo, igual a ausente; a lista omite no SQL). PUT/PATCH/DELETE não aplicam essa regra.

| Método | Caminho | Sucesso |
| --- | --- | --- |
| POST | `/profiles` | 200 + resumo (id, name, description; sem senha); cria internamente checker `VERIFY_EMAIL` e envia o código ao e-mail |
| POST | `/profiles/password` | 204; corpo `{ currentPassword, newPassword, signoutAllSessions? }`; só o perfil do Bearer; `signoutAllSessions: true` encerra todas as sessões |
| POST | `/profiles/password/recovery` | 204; corpo `{ email }`; cria ou gira `CHANGE_PASSWORD` e envia o código; e-mail desconhecido, `VERIFY_EMAIL` ou checker com mais de 12 horas também 204, sem e-mail |
| POST | `/profiles/password/confirm` | 204; corpo `{ email, code, newPassword }`; troca a senha e encerra todas as sessões daquele perfil |
| GET | `/profiles/{id}` | 200 + resumo |
| GET | `/profiles/{id}/details` | 200 + detalhes (sem senha) |
| PUT | `/profiles/{id}` | 200 + resumo; id só na URL; `password` extra é ignorado |
| PATCH | `/profiles/{id}` | 200 + resumo; campos omitidos permanecem; `"description": null` limpa a descrição; `password` extra é ignorado |
| DELETE | `/profiles/{id}` | 204; 404 se ausente (não é 204 idempotente) |
| GET | `/profiles` | 200 + página por cursor (`name`, `lastSeenName`, `lastSeenId`, `limit`, `reverse`; `precedingElements` / `followingElements`) |

Erros: **400** mapa campo→mensagens; **401** Bearer ausente ou inválido `{token:[…]}`, senha atual errada em `POST /profiles/password` `{credentials:[…]}` e recuperação com código divergente, checker ausente/vencido ou e-mail inexistente em `POST /profiles/password/confirm` `{code:[…]}`; **409** e-mail já registrado; **404** sem corpo; **429** `{credentials:[…]}` + `Retry-After` na troca de senha e na recuperação; **503** store de sessões indisponível ou serviço de correio indisponível no POST criar e no pedido de recuperação. Detalhes no OpenAPI e na collection Postman.

O POST criar gera internamente um checker `VERIFY_EMAIL` e envia o código de verificação ao e-mail do perfil. Enquanto o checker existir, [`/tokens/signin`](tokens.md) responde **403** `{email:[…]}` se o `code` faltar, **400** se estiver mal formado e **401** `{code:[…]}` se divergir (o código vigente não muda); senha + código conferindo consomem o checker e abrem a sessão. [`POST /tokens/verification`](tokens.md) reenvia um código novo (429 só do limiter `CREDENTIALS`). [`/tokens/refresh`](tokens.md) responde **403** `{email:[…]}` se o checker ainda existir.

Trocar a senha (`POST /profiles/password`) com `"signoutAllSessions": true`, confirmar a recuperação (`POST /profiles/password/confirm`) e excluir o perfil encerram **todas** as sessões daquele perfil em [`/tokens`](tokens.md), inclusive a corrente: o access deixa de valer na hora, sem esperar o `exp`. O encerramento precede a escrita, então Redis fora do ar responde **503** com o perfil intacto. Omitir `signoutAllSessions` ou enviar `false` na troca autenticada troca a senha e **mantém** as sessões. PUT e PATCH **não** alteram a senha.

`POST /profiles/password/recovery` (público; corpo `{ email }`) cria o checker `CHANGE_PASSWORD` ou gira o código vigente (o id UUIDv7 permanece, então o prazo de 12 horas não reabre) e envia o HTML (código só no corpo). Sempre **204**: e-mail desconhecido, perfil com `VERIFY_EMAIL` ou checker com mais de 12 horas não enviam correio. `POST /profiles/password/confirm` (público; corpo `{ email, code, newPassword }`) confere o código, encerra as sessões e grava a senha nova. Código ausente ou mal formado → **400** `{code:[…]}`; divergente, checker ausente/vencido ou e-mail inexistente → **401** `{code:[…]}` (o código vigente não muda). O purge interno à meia-noite apaga o `CHANGE_PASSWORD` cujo UUIDv7 passou de `sajitar.profile.change-password-max-age-hours` (12), sem tocar o perfil nem as sessões.

A listagem **`GET /profiles`** pagina por cursor sobre nome e id. Quem não é `MASTER` não vê perfis com `VERIFY_EMAIL` (contagens de cursor no SQL). Exemplos de navegação também em `ProfileControllerIntegrationTest`.

Ver também: [tokens](tokens.md) · [authorities](authorities.md) · [notes](notes.md) · [comandos e URLs](../development/commands.md)
