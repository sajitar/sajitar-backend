-- Resolve um token ativo que ainda é o vigente da sua sessão.
--
-- Registro ausente, tipo trocado ou jti órfão (substituído por uma rotação)
-- devolvem nil: assinatura válida não basta para autenticar.
--
-- ARGV: 1 tokenId, 2 use ('access' | 'refresh')
-- Retorno: 'profileId|sessionId|accessId|refreshId' ou nil

local tokenId = ARGV[1]
local use = ARGV[2]

local token = redis.call('HMGET', 'token:' .. tokenId, 'profileId', 'use', 'sessionId')
if not token[1] or token[2] ~= use then
    return nil
end

local session = redis.call('HMGET', 'session:' .. token[3], 'profileId', 'accessId', 'refreshId')
if not session[1] then
    return nil
end

local current = session[2]
if use == 'refresh' then
    current = session[3]
end
if current ~= tokenId then
    return nil
end

return session[1] .. '|' .. token[3] .. '|' .. (session[2] or '') .. '|' .. (session[3] or '')
