# API `/tokens`

A API abre, lista, renova e encerra sessões de login.
O JWT é HS256.
O payload identifica o token.
O payload traz `jti`, `iss`, `aud`, `exp` e `token_use`.
O payload não traz `sub`.
O `profileId` fica só no registro do Redis.

Toda resposta inclui `Cache-Control` com `no-store`.

## Idioma

A query `lang` segue a regra de [`/notes`](notes.md).

## Access válido

O access vale com as duas condições abaixo.

1. Assinatura, `iss`, `aud`, `exp` e `token_use=access` conferem.
2. O `jti` é o access vigente da sessão no Redis.

A rotação invalida o access na hora.
A perda do registro no Redis invalida o access na hora.
Redis fora do ar responde 503 sem corpo.
A API não autentica sem consultar o store.

## Rotas

| Método | Caminho | Auth | Sucesso |
| --- | --- | --- | --- |
| POST | `/tokens/signin` | público | 200 e o access |
| POST | `/tokens/verification` | público | 204 sem corpo |
| POST | `/tokens/authentication` | público | 204 sem corpo |
| POST | `/tokens/refresh` | público | 200 e o par novo |
| GET | `/tokens` | Bearer access | 200 e as sessões |
| POST | `/tokens/signout` | Bearer access | 204 sem corpo |

As quatro rotas POST públicas tratam a chamada como anônima.
Um header `Authorization` inválido mantém o mesmo resultado.
`GET /tokens` e `POST /tokens/signout` exigem access vigente no Redis.

### POST `/tokens/signin`

O corpo traz e-mail e senha.
`"refresh": true` também emite `refreshToken`, `refreshId` e `refreshExpiresIn`.
Sem refresh, o JSON omite esses três campos.
O access devolve `token`, `type`, `expiresIn`, `id` e `sessionId`.
`type` vale `Bearer`.

Cada `signin` cria uma sessão.
O `sessionId` é um UUIDv7.
Os 48 bits de tempo desse id são o instante do login.
O teto `max-sessions-per-profile` encerra a sessão mais antiga.
O `signin` segue e responde 200.

Um perfil com checker `VERIFY_EMAIL` exige `code` no corpo.

| `code` | Resposta |
| --- | --- |
| Ausente | 403 `{email}` |
| Mal formado | 400 `{code}` |
| Divergente | 401 `{code}` |
| Igual ao vigente | 200, e a API apaga o checker |

O código divergente permanece o vigente.

Sem `VERIFY_EMAIL`, o segundo fator exige o código de `SIGN_IN`.

| `code` de `SIGN_IN` | Resposta |
| --- | --- |
| Ausente | 403 `{email}` |
| Mal formado | 400 `{code}` |
| Divergente, ausente ou vencido | 401 `{code}` |
| Igual ao vigente e dentro do prazo | 200, e a API apaga o checker |

O 401 de `SIGN_IN` não gira o código.
O prazo é `sajitar.profile.sign-in-max-age-minutes`.
E-mail desconhecido ou senha errada responde 401 `{credentials}`.

O corpo pode trazer `termId` como texto.
Campo ausente chega nulo.
A regra vale para o perfil que não é `MASTER` quando há termo vigente e o vínculo ainda não é esse `id`.

| `termId` | Resposta |
| --- | --- |
| Nulo ou em branco | 403 `{termId}` |
| Presente e mal formado | 400 `{termId}` |
| UUID diferente do vigente | 401 `{termId}` |
| UUID do vigente | 200, e a API grava esse `id` depois de emitir o token |

O perfil `MASTER` não grava `termId`.
Texto mal formado é ignorado quando o vínculo já é o vigente, quando o perfil é `MASTER` ou quando não há termo vigente.
Sem termo vigente, a emissão grava `termId` nulo para quem não é `MASTER`.
Uma falha de `termId` desfaz o consumo do checker.

### POST `/tokens/verification`

O corpo traz e-mail e senha.
Senha conferida e checker `VERIFY_EMAIL` presente geram um código novo.
O código anterior deixa de valer.
A API envia o HTML ao e-mail.
O código fica só no corpo do e-mail.
Sem o checker, a rota responde 204 e não envia e-mail.
E-mail desconhecido ou senha errada responde 401 `{credentials}`.

### POST `/tokens/authentication`

O corpo traz e-mail e senha.
A rota cria ou gira `SIGN_IN` quando as três condições abaixo são verdadeiras.

1. O perfil exige segundo fator.
2. O perfil não tem `VERIFY_EMAIL`.
3. O checker `SIGN_IN`, se existir, ainda está no prazo.

O giro mantém o id UUIDv7.
O prazo não reabre.
A API envia o HTML ao e-mail.
O código fica só no corpo do e-mail.

A rota responde 204 sem e-mail nestes casos.

- O perfil tem `VERIFY_EMAIL`.
- O segundo fator não é obrigatório.
- O checker `SIGN_IN` passou de `sajitar.profile.sign-in-max-age-minutes`.

E-mail desconhecido ou senha errada responde 401 `{credentials}`.

O job interno corre a cada 5 minutos.
O fuso é `sajitar.profile.unverified-purge-zone`.
O job apaga `SIGN_IN` cujo UUIDv7 passou do prazo.
O job não apaga o perfil.
O job não encerra as sessões.

### POST `/tokens/refresh`

O corpo traz `refreshToken`.
O corpo pode trazer `termId` como texto.
Não envie o refresh em `Authorization`.
A rota troca o refresh vigente por um par novo.
O access ligado ao refresh apresentado deixa de valer.
O `sessionId` permanece.
O refresh grava o `client` do User-Agent dessa requisição.

Um retry do mesmo refresh dentro de `refresh-grace-seconds` devolve o mesmo par sucessor.
Esse retry não lê `termId`.
Fora dessa janela, o reuso apaga a sessão.
O reuso fora da janela responde 401 `{refreshToken}`.
`refresh-grace-seconds` com 0 desliga a janela.

O prazo do refresh é o menor entre a inatividade e o teto da sessão.
A inatividade é `refresh-expiration-seconds`.
O teto é `bornAt` mais `session-max-seconds`.

`VERIFY_EMAIL` no perfil responde 403 `{email}`.
A checagem de `termId` vem depois dessa recusa e antes da rotação.
Ela usa a mesma tabela do `signin` para quem não é `MASTER`.
A gravação de `termId` ocorre depois da rotação bem-sucedida.
`SIGN_IN` no perfil não bloqueia o refresh.
Refresh inválido, órfão, expirado ou de perfil ausente responde 401 `{refreshToken}`.

### GET `/tokens`

A rota lista as sessões ativas do perfil do Bearer.
A ordem vai da mais antiga para a mais recente.
Cada item traz `id` e `current`.
O `id` é o `sessionId`.
O `id` não é o `jti`.
`current` vale `true` só na sessão desse request.
A lista vazia responde 200.
A lista vazia não responde 404.
A resposta não traz `createdAt`.
O instante do login está nos 48 bits de tempo do `id`.
A rotação não muda o `id`.

`client` entra quando o YAUAA reconhece o User-Agent.
`client` traz `name`, `os` e `device`.
`device` vale `desktop`, `mobile`, `tablet` ou `unknown`.
Sem header, com valor vazio, `Unknown` ou `Hacker`, a API omite `client`.

### POST `/tokens/signout`

O corpo traz `ids` e, quando exigida, `password`.
A rota encerra sessões do próprio perfil.
A rota remove o access e o refresh de cada id.
O Bearer basta quando `ids` traz só a sessão corrente.
Repetir a sessão corrente ainda dispensa a senha.
Qualquer outro id exige `password`.
A API confere a senha antes de informar se a sessão existe.
O lote é tudo ou nada.

| Resultado | Quando |
| --- | --- |
| 204 sem corpo | Todos os ids são sessões ativas do perfil |
| 404 sem corpo | Algum id não existe, já encerrou ou é de outro perfil |
| 400 `{ids}` | A lista está ausente ou vazia |
| 400 `{password}` | A senha exigida está ausente ou mal formada |
| 401 `{credentials}` | A senha não confere |
| 401 `{token}` | O Bearer está ausente ou inválido |

O 404 não encerra nenhuma sessão do lote.
O 404 não distingue os três casos.

## Erros comuns

| Status | Quando |
| --- | --- |
| 400 | Corpo mal formado, `refreshToken` em branco ou `code` mal formado |
| 401 `{credentials}` | E-mail ou senha inválidos no `signin`, na verificação e na autenticação |
| 401 `{code}` | Código divergente, ausente ou vencido |
| 401 `{refreshToken}` | Refresh inválido, órfão, expirado ou reutilizado fora da graça |
| 403 `{email}` | `VERIFY_EMAIL` com `code` ausente no `signin`, ou ainda presente no refresh |
| 403 `{email}` | Segundo fator com `code` ausente no `signin` |
| 429 | Limite de tentativas. A resposta traz `Retry-After` |
| 503 | Redis indisponível, ou correio indisponível na verificação e na autenticação |

O 429 do `signin`, da verificação, da autenticação e do `signout` com senha usa `{credentials}`.
O 429 do refresh usa `{refreshToken}`.

## Encerramento vindo de `/profiles`

Estes fluxos chamam `wipe` antes da escrita no perfil.
Redis fora do ar responde 503 e o perfil permanece intacto.

- `POST /profiles/password` com `signoutAllSessions` verdadeiro.
- `POST /profiles/password/confirm`.
- `POST /profiles/email/change`.
- `DELETE /profiles/{id}` depois do código conferido.

`signoutAllSessions` omitido ou `false` mantém as sessões.

## Propriedades `sajitar.security.jwt`

A subida exige `session-max-seconds` maior que `refresh-expiration-seconds`.
`refresh-expiration-seconds` tem de ser maior que `expiration-seconds`.
`expiration-seconds` tem de ser maior que 0.
O segredo tem no mínimo 32 bytes.

Os valores abaixo valem em todos os perfis Spring.

| Propriedade | Papel | Valor |
| --- | --- | --- |
| `expiration-seconds` | Validade do access | 3600 |
| `refresh-expiration-seconds` | Inatividade do refresh | 604800 |
| `session-max-seconds` | Teto absoluto desde o `signin` | 2592000 |
| `max-sessions-per-profile` | Sessões ativas por perfil | 10 |
| `refresh-grace-seconds` | Janela de retry do refresh consumido | 15 |
| `issuer` | Claim `iss` | `sajitar-backend` |
| `audience` | Claim `aud` | `sajitar-app` |

## Propriedades `sajitar.security.attempt`

O limite conta cada requisição da janela.
O `signin` conta por endereço e por e-mail.
O e-mail entra na conta mesmo quando o perfil não existe.
O refresh conta por endereço.
O `signout` com senha conta por endereço.

O escopo `CREDENTIALS` é compartilhado por estas rotas.

- `POST /tokens/signin`
- `POST /tokens/verification`
- `POST /tokens/authentication`
- `POST /tokens/signout` quando a senha é exigida
- `POST /profiles/password`
- `POST /profiles/password/recovery` e `POST /profiles/password/confirm`
- `POST /profiles/email/recovery`, `/confirm` e `/change`
- `POST /profiles/deletion` e `DELETE /profiles/{id}`
- PUT e PATCH de perfil que cruzam `MASTER`

O estouro responde 429 com `Retry-After`.

| Propriedade | Papel | `LOCAL`, `DEMO` e `DOCKER` | `LOCAL-TEST`, `CI` e `DOCKER-TEST` |
| --- | --- | --- | --- |
| `credentials-max` | Teto do escopo `CREDENTIALS` | 10 | 5 |
| `credentials-window-seconds` | Janela desse teto | 300 | 60 |
| `refresh-max` | Teto do escopo `REFRESH` | 30 | 5 |
| `refresh-window-seconds` | Janela desse teto | 60 | 60 |
| `trust-forwarded-for` | Com `true`, o IP é o primeiro de `X-Forwarded-For` | `false` | `false` |

## Redis

O Redis das sessões é uma instância dedicada.
AUTH e ACL são obrigatórios.
O arquivo local é `docker/redis/users.acl`.
Os prefixos são `token:`, `tomb:`, `session:`, `profile:` e `attempt:`.
A política é `maxmemory-policy noeviction`.
O AOF fica ligado com `--appendonly yes`.
O Compose local guarda `/data` no volume `redis-data`.
`spring.data.redis.ssl.enabled` liga o TLS do cliente.

O usuário `default` permanece ligado.
A senha de `default` é distinta da senha da aplicação.
A aplicação usa o usuário `sajitar`.
`default` desligado impede o replay do AOF.
Nesse caso o store sobe vazio depois do restart.

`docker restart` e `compose down` sem `-v` preservam as sessões cujo TTL ainda vale.
`compose down -v` apaga o volume e revoga os tokens.

Aviso: `compose down -v` apaga todas as sessões.

A ACL nega `KEYS` e `FLUSHDB`.
`SCAN 0 MATCH session:*` lista as sessões ativas.

Os exemplos HTTP estão em `TokenControllerIntegrationTest`.
O store está em `RedisSessionStoreTest`.

Ver também [profiles](profiles.md), [notes](notes.md), [terms](terms.md) e [comandos](../development/commands.md).
