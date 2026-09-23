# API `/profiles`

Query opcional **`lang`**: `en` (padrão), `pt` ou `es`. Omitida, vazia ou não suportada → inglês. Sem sessão e sem `Accept-Language`.

Autenticação **JWT HS256** (`Authorization: Bearer` com access token, claim `token_use=access`) emitida em [`/tokens`](tokens.md). Público: `POST /profiles` — um `Authorization` Basic ou Bearer inválido é ignorado. Demais rotas de `/profiles` exigem access Bearer válido, isto é, assinado **e** com registro ativo no Redis. Refresh JWT **não** vale no header. O token autentica o perfil. GET por id, details e lista **não** devolvem perfil que ainda tem checker `VERIFY_EMAIL`, salvo o caller com autoridade `MASTER` (404 sem corpo, igual a ausente; a lista omite no SQL). PUT/PATCH/DELETE não aplicam essa regra.

| Método | Caminho | Sucesso |
| --- | --- | --- |
| POST | `/profiles` | 200 + resumo (id, name, description; sem senha); cria internamente checker `VERIFY_EMAIL` e envia o código ao e-mail |
| POST | `/profiles/password` | 204; corpo `{ currentPassword, newPassword, signoutAllSessions? }`; só o perfil do Bearer; `signoutAllSessions: true` encerra todas as sessões |
| GET | `/profiles/{id}` | 200 + resumo |
| GET | `/profiles/{id}/details` | 200 + detalhes (sem senha) |
| PUT | `/profiles/{id}` | 200 + resumo; id só na URL; `password` extra é ignorado |
| PATCH | `/profiles/{id}` | 200 + resumo; campos omitidos permanecem; `"description": null` limpa a descrição; `password` extra é ignorado |
| DELETE | `/profiles/{id}` | 204; 404 se ausente (não é 204 idempotente) |
| GET | `/profiles` | 200 + página por cursor (`name`, `lastSeenName`, `lastSeenId`, `limit`, `reverse`; `precedingElements` / `followingElements`) |

Erros: **400** mapa campo→mensagens; **401** Bearer ausente ou inválido `{token:[…]}` e senha atual errada em `POST /profiles/password` `{credentials:[…]}`; **409** e-mail já registrado; **404** sem corpo; **429** `{credentials:[…]}` + `Retry-After` na troca de senha; **503** store de sessões indisponível ou serviço de correio indisponível no POST criar. Detalhes no OpenAPI e na collection Postman.

O POST criar gera internamente um checker `VERIFY_EMAIL` e envia o código de verificação ao e-mail do perfil. Enquanto o checker existir, [`/tokens/signin`](tokens.md) responde **403** `{email:[…]}` se o `code` faltar, **400** se estiver mal formado e **401** `{code:[…]}` se divergir (o código vigente não muda); senha + código conferindo consomem o checker e abrem a sessão. [`POST /tokens/verification`](tokens.md) reenvia um código novo (429 só do limiter `CREDENTIALS`). [`/tokens/refresh`](tokens.md) responde **403** `{email:[…]}` se o checker ainda existir.

Trocar a senha (`POST /profiles/password`) com `"signoutAllSessions": true` e excluir o perfil encerram **todas** as sessões daquele perfil em [`/tokens`](tokens.md), inclusive a corrente: o access deixa de valer na hora, sem esperar o `exp`. O encerramento precede a escrita, então Redis fora do ar responde **503** com o perfil intacto. Omitir `signoutAllSessions` ou enviar `false` troca a senha e **mantém** as sessões. PUT e PATCH **não** alteram a senha.

A listagem **`GET /profiles`** pagina por cursor sobre nome e id. Quem não é `MASTER` não vê perfis com `VERIFY_EMAIL` (contagens de cursor no SQL). Exemplos de navegação também em `ProfileControllerIntegrationTest`.

Ver também: [tokens](tokens.md) · [authorities](authorities.md) · [notes](notes.md) · [comandos e URLs](../development/commands.md)
