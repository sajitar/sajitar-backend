# API `/tokens`

Query opcional **`lang`**: mesma regra de [`/profiles`](profiles.md). Autenticação **JWT HS256** com sessão no **Redis**: o payload identifica o **token** (`jti` UUIDv7), nunca o perfil — `sub` não é usado e o id do perfil só existe no registro do Redis. Toda resposta sai com `Cache-Control` contendo `no-store`.

Um access vale enquanto (1) assinatura, `iss`, `aud`, `exp` e `token_use=access` conferem **e** (2) o `jti` é o access vigente da sessão no Redis. Rotacionar ou perder o registro invalida o token na hora. Redis fora do ar é **503** (nada é emitido nem autenticado sem consultar o store), não 401.

| Método | Caminho | Auth | Sucesso |
| --- | --- | --- | --- |
| POST | `/tokens/signin` | público | 200 + `{ token, type, expiresIn, id, sessionId }`; com `"refresh": true` no corpo, também `refreshToken`, `refreshId` e `refreshExpiresIn`; com `VERIFY_EMAIL`, o corpo também leva `code`
| POST | `/tokens/verification` | público | 204 sem corpo; gira o código de `VERIFY_EMAIL` (o anterior deixa de valer) e envia o e-mail; já verificado também 204, sem e-mail |
| POST | `/tokens/refresh` | público | 200 + par novo no mesmo `sessionId`; corpo `{ refreshToken }` (não usar `Authorization`) |
| GET | `/tokens` | Bearer access | 200 + `{ content: [{ id, current, client? }] }` com as sessões ativas do perfil |
| POST | `/tokens/signout` | Bearer access (+ senha para outra sessão) | 204 sem corpo; corpo `{ ids, password }` |

As rotas de emissão e o reenvio de código são **públicas**: `Authorization` Basic ou Bearer inválido é ignorado. Os campos de refresh são **omitidos** (não vêm como `null`) quando a sessão tem só access. `GET /tokens` e `POST /tokens/signout` exigem access Bearer válido no Redis.

`POST /tokens/verification` (corpo `{ email, password }`) reenvia o código de `VERIFY_EMAIL`: senha conferindo e checker presente geram código novo (o anterior deixa de valer) e mandam o HTML (código só no corpo). Sem checker → 204 sem e-mail. O palpite errado no signin responde **401** `{code:[…]}` e **não** altera o código vigente. O único **429** desses fluxos é o limiter `CREDENTIALS` (o mesmo do signin/signout), com `Retry-After` da janela.

## Sessão e rotação

Cada signin cria uma sessão (`sessionId` UUIDv7, cujos 48 bits de tempo são o instante do login) e conta no teto `max-sessions-per-profile`; ao estourar, a sessão **mais antiga** é encerrada e o signin segue com 200. O refresh tem janela de inatividade (`refresh-expiration-seconds`) limitada pelo teto absoluto da sessão (`session-max-seconds`).

`POST /tokens/refresh` troca o refresh vigente por um par novo em uma operação atômica: o refresh apresentado e o access ligado a ele deixam de valer, o `sessionId` permanece. Um retry do mesmo refresh dentro de `refresh-grace-seconds` devolve **o mesmo par sucessor**; fora dessa janela o reuso é tratado como furto e **apaga a sessão inteira**, respondendo 401.

Erros: **400** mapa campo→mensagens (credenciais mal formadas, `refreshToken` em branco, `code` de verificação mal formado); **401** credenciais inválidas `{credentials:[…]}` no signin e no reenvio; código de `VERIFY_EMAIL` divergente `{code:[…]}`; refresh inválido, órfão, expirado, já consumido fora da graça ou de perfil inexistente `{refreshToken:[…]}`; **403** e-mail não verificado `{email:[…]}` quando o perfil tem checker `VERIFY_EMAIL` e o `code` falta; **429** `{credentials:[…]}` no signin e no reenvio (limiter `CREDENTIALS`) e `{refreshToken:[…]}` no refresh, com header `Retry-After`; **503** store de sessões indisponível ou serviço de correio indisponível no reenvio. Detalhes no OpenAPI e na collection Postman.

## Listagem e saída

`GET /tokens` lista um item por **sessão de login** do perfil do Bearer, da mais antiga para a mais recente. Cada item traz `id` (o `sessionId`, nunca o `jti`), `current` (verdadeiro apenas na sessão do Bearer daquela requisição) e, quando o User-Agent daquela sessão tem um `AgentName` útil no YAUAA, `client` (`name`, `os`, `device` em `desktop` \| `mobile` \| `tablet` \| `unknown`). Sem header, lixo (`Unknown`/`Hacker`) ou parse vazio, o objeto é omitido. O instante do login sai dos 48 bits de tempo do próprio `id`, então não há `createdAt`. Rotação não muda o `id`; refresh **atualiza** o `client` com o User-Agent daquela requisição. Lista sem item não é 404. O campo `location` da proposta ainda não é gravado (não há provedor de GeoIP) e, por isso, continua omitido.

`POST /tokens/signout` encerra uma ou mais sessões do **próprio** perfil, removendo o access e o refresh de cada uma. O Bearer basta quando `ids` traz só a sessão corrente (inclusive repetida); qualquer id de outra sessão exige `password` no corpo, conferida **antes** de revelar se aquelas sessões existem. O lote é tudo ou nada: **204** sem corpo quando todos os ids são sessões ativas suas, **404** sem corpo (sem encerrar nada) quando algum id é inexistente, já encerrado ou de outro perfil — o mesmo 404 cobre os três casos, sem 403 e sem enumeração.

Erros de `GET /tokens`: **401** `{token:[…]}` sem Bearer válido; **503** store indisponível. Erros de `POST /tokens/signout`: **400** `{ids:[…]}` (lista ausente ou vazia) ou `{password:[…]}` (senha exigida, ausente ou mal formada); **401** `{token:[…]}` sem Bearer válido e `{credentials:[…]}` quando a senha não confere; **404** sem corpo; **429** `{credentials:[…]}` com `Retry-After` quando a senha é exigida e o limite (o mesmo do signin, por endereço) estourou; **503** store indisponível.

Trocar a senha (`POST /profiles/password`) com `"wipe": true` e excluir o perfil encerram **todas** as sessões daquele perfil na hora, inclusive a corrente. Sem `wipe` (ou `false`) a troca de senha **mantém** as sessões.

## Propriedades (`sajitar.security.jwt`)

Invariante: `session-max-seconds` > `refresh-expiration-seconds` > `expiration-seconds` > 0.

| Propriedade | Papel | Padrão |
| --- | --- | --- |
| `expiration-seconds` | validade do access | 3600 |
| `refresh-expiration-seconds` | inatividade do refresh e da sessão | 604800 |
| `session-max-seconds` | teto absoluto contado do signin | 2592000 |
| `max-sessions-per-profile` | sessões ativas por perfil | 10 |
| `refresh-grace-seconds` | janela de retry do refresh consumido (0 desliga) | 15 |
| `issuer` / `audience` | claims `iss` e `aud` exigidas na validação | `sajitar-backend` / `sajitar-app` |

## Propriedades (`sajitar.security.attempt`)

Limite de tentativas em `/tokens`: conta **toda** requisição na janela (protege BCrypt e a verificação de assinatura). Signin conta por endereço **e** por e-mail (mesmo inexistente); reenvio de `VERIFY_EMAIL`, palpite do código no primeiro signin e [`POST /profiles/password`](profiles.md) compartilham esse contador; refresh conta por endereço; signout com senha compartilha o contador `CREDENTIALS` do signin. Estouro → **429** com `Retry-After`.

| Propriedade | Papel | Padrão (local/CI/demo) |
| --- | --- | --- |
| `credentials-max` | teto do escopo `CREDENTIALS` (signin e signout com senha) | 10 |
| `credentials-window-seconds` | janela desse teto | 300 |
| `refresh-max` | teto do escopo `REFRESH` | 30 |
| `refresh-window-seconds` | janela desse teto | 60 |
| `trust-forwarded-for` | se `true`, o limite por IP usa o primeiro endereço de `X-Forwarded-For` | `false` |

O Redis é **instância dedicada** a sessões: AUTH e ACL obrigatórios (`docker/redis/users.acl`, prefixos `token:`, `tomb:`, `session:`, `profile:` e `attempt:`), `maxmemory-policy noeviction`, AOF (`--appendonly yes`) e volume nomeado `redis-data` em `/data` no Compose local, e TLS em produção (`SPRING_DATA_REDIS_SSL_ENABLED`). O user `default` precisa estar ligado (senha distinta da do app) para o replay do AOF: `off` faz o `MULTI`/`EXEC` dos scripts Lua falhar no restart e o store sobe vazio. Sem persistência (nem volume), reiniciar ou recriar o Redis revoga todos os tokens.

Exemplos de uso em `TokenControllerIntegrationTest`; comportamento do store em `RedisSessionStoreTest`.

Ver também: [profiles](profiles.md) · [authorities](authorities.md) · [notes](notes.md) · [comandos e URLs](../development/commands.md)
