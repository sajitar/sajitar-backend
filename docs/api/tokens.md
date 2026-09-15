# API `/tokens`

Query opcional **`lang`**: mesma regra de [`/profiles`](profiles.md). Autenticação **JWT HS256** com sessão no **Redis**: o payload identifica o **token** (`jti` UUIDv7), nunca o perfil — `sub` não é usado e o id do perfil só existe no registro do Redis. Toda resposta sai com `Cache-Control` contendo `no-store`.

Um access vale enquanto (1) assinatura, `iss`, `aud`, `exp` e `token_use=access` conferem **e** (2) o `jti` é o access vigente da sessão no Redis. Rotacionar ou perder o registro invalida o token na hora. Redis fora do ar é **503** (nada é emitido nem autenticado sem consultar o store), não 401.

| Método | Caminho | Auth | Sucesso |
| --- | --- | --- | --- |
| POST | `/tokens/signin` | público | 200 + `{ token, type, expiresIn, id, sessionId }`; com `"refresh": true` no corpo, também `refreshToken`, `refreshId` e `refreshExpiresIn` |
| POST | `/tokens/refresh` | público | 200 + par novo no mesmo `sessionId`; corpo `{ refreshToken }` (não usar `Authorization`) |

Ambas são **públicas**: `Authorization` Basic ou Bearer inválido é ignorado. Os campos de refresh são **omitidos** (não vêm como `null`) quando a sessão tem só access.

## Sessão e rotação

Cada signin cria uma sessão (`sessionId` UUIDv7, cujos 48 bits de tempo são o instante do login) e conta no teto `max-sessions-per-profile`; ao estourar, a sessão **mais antiga** é encerrada e o signin segue com 200. O refresh tem janela de inatividade (`refresh-expiration-seconds`) limitada pelo teto absoluto da sessão (`session-max-seconds`).

`POST /tokens/refresh` troca o refresh vigente por um par novo em uma operação atômica: o refresh apresentado e o access ligado a ele deixam de valer, o `sessionId` permanece. Um retry do mesmo refresh dentro de `refresh-grace-seconds` devolve **o mesmo par sucessor**; fora dessa janela o reuso é tratado como furto e **apaga a sessão inteira**, respondendo 401.

Erros: **400** mapa campo→mensagens (credenciais mal formadas, `refreshToken` em branco); **401** credenciais inválidas `{credentials:[…]}` no signin; refresh inválido, órfão, expirado, já consumido fora da graça ou de perfil inexistente `{refreshToken:[…]}`; **403** e-mail não verificado `{email:[…]}` quando o perfil tem checker `VERIFY_EMAIL`; **503** store de sessões indisponível. Detalhes no OpenAPI e na collection Postman.

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

O Redis é **instância dedicada** a sessões: AUTH e ACL obrigatórios (`docker/redis/users.acl`), `maxmemory-policy noeviction`, persistência recomendada e TLS em produção (`SPRING_DATA_REDIS_SSL_ENABLED`). Sem persistência, reiniciar o Redis revoga todos os tokens.

Exemplos de uso em `TokenControllerIntegrationTest`; comportamento do store em `RedisSessionStoreTest`.

Ver também: [profiles](profiles.md) · [checkers](checkers.md) · [authorities](authorities.md) · [notes](notes.md) · [comandos e URLs](../development/commands.md)
