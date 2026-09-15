-- Troca o refresh vigente pelo par novo em uma única passagem atômica.
--
-- Tombstone dentro da graça devolve as claims do par sucessor (retry de cliente
-- não derruba a sessão); fora da graça é reuso: apaga a sessão inteira. Sem
-- tombstone, a troca só ocorre se o jti apresentado ainda for o refresh vigente
-- da sessão, então dois pedidos simultâneos nunca rotacionam o mesmo refresh.
--
-- ARGV: 1 presentedRefreshId, 2 nowMs, 3 graceMs,
--       4 sessionId (ausente quando não há par candidato),
--       5 accessId, 6 accessIat, 7 accessExp, 8 accessTtlMs,
--       9 refreshId, 10 refreshIat, 11 refreshExp, 12 refreshTtlMs,
--       13 sessionTtlMs, 14 tombTtlMs
-- Retorno: 'rotated' | 'replayed|sessionId|accessId|iat|exp|refreshId|iat|exp' | 'invalid'

local presented = ARGV[1]
local now = tonumber(ARGV[2])
local grace = tonumber(ARGV[3])
local tombKey = 'tomb:' .. presented

local function wipe(id)
    local keyOfSession = 'session:' .. id
    for _, tokenId in ipairs(redis.call('SMEMBERS', keyOfSession .. ':tokens')) do
        redis.call('DEL', 'token:' .. tokenId)
    end
    for _, tombId in ipairs(redis.call('SMEMBERS', keyOfSession .. ':tombs')) do
        redis.call('DEL', 'tomb:' .. tombId)
    end
    local owner = redis.call('HGET', keyOfSession, 'profileId')
    if owner then
        redis.call('ZREM', 'profile:' .. owner .. ':sessions', id)
    end
    redis.call('DEL', keyOfSession, keyOfSession .. ':tokens', keyOfSession .. ':tombs')
end

local tomb = redis.call('HMGET', tombKey,
    'sessionId', 'rotatedAt', 'accessId', 'accessIat', 'accessExp', 'refreshId', 'refreshIat', 'refreshExp')
if tomb[1] then
    if now - tonumber(tomb[2]) < grace then
        return table.concat({ 'replayed', tomb[1], tomb[3], tomb[4], tomb[5], tomb[6], tomb[7], tomb[8] }, '|')
    end
    wipe(tomb[1])
    return 'invalid'
end

local sessionId = ARGV[4]
if not sessionId or sessionId == '' then
    return 'invalid'
end

local sessionKey = 'session:' .. sessionId
local session = redis.call('HMGET', sessionKey, 'profileId', 'accessId', 'refreshId')
if not session[1] or session[3] ~= presented then
    return 'invalid'
end

local profileId = session[1]
local previousAccessId = session[2]
local accessId = ARGV[5]
local refreshId = ARGV[9]
local accessTtl = tonumber(ARGV[8])
local refreshTtl = tonumber(ARGV[12])
local sessionTtl = tonumber(ARGV[13])
local tombTtl = tonumber(ARGV[14])
local tokensKey = sessionKey .. ':tokens'
local tombsKey = sessionKey .. ':tombs'

redis.call('DEL', 'token:' .. previousAccessId, 'token:' .. presented)
redis.call('SREM', tokensKey, previousAccessId, presented)

redis.call('HSET', tombKey,
    'sessionId', sessionId, 'rotatedAt', now,
    'accessId', accessId, 'accessIat', ARGV[6], 'accessExp', ARGV[7],
    'refreshId', refreshId, 'refreshIat', ARGV[10], 'refreshExp', ARGV[11])
redis.call('PEXPIRE', tombKey, tombTtl)
redis.call('SADD', tombsKey, presented)
redis.call('PEXPIRE', tombsKey, tombTtl)

redis.call('HSET', 'token:' .. accessId,
    'profileId', profileId, 'use', 'access', 'sessionId', sessionId, 'linkedId', refreshId)
redis.call('PEXPIRE', 'token:' .. accessId, accessTtl)
redis.call('HSET', 'token:' .. refreshId,
    'profileId', profileId, 'use', 'refresh', 'sessionId', sessionId, 'linkedId', accessId)
redis.call('PEXPIRE', 'token:' .. refreshId, refreshTtl)
redis.call('SADD', tokensKey, accessId, refreshId)
redis.call('PEXPIRE', tokensKey, sessionTtl)

redis.call('HSET', sessionKey, 'accessId', accessId, 'refreshId', refreshId)
redis.call('PEXPIRE', sessionKey, sessionTtl)

local indexKey = 'profile:' .. profileId .. ':sessions'
if redis.call('PTTL', indexKey) < 0 then
    redis.call('PEXPIRE', indexKey, sessionTtl)
else
    redis.call('PEXPIRE', indexKey, sessionTtl, 'GT')
end

return 'rotated'
