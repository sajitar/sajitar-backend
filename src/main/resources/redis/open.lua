-- Abre uma sessão de /tokens e grava o par emitido.
--
-- Antes de gravar, limpa do índice do perfil as sessões que já morreram por TTL
-- e, se o teto continuar estourado, encerra as mais antigas (menor timestamp no
-- UUIDv7 do sessionId, que é a ordem do zset).
--
-- ARGV: 1 sessionId, 2 profileId, 3 bornAtMs, 4 accessId, 5 accessTtlMs,
--       6 refreshId ou '', 7 refreshTtlMs, 8 sessionTtlMs, 9 maxSessions

local sessionId = ARGV[1]
local profileId = ARGV[2]
local bornAtMs = tonumber(ARGV[3])
local accessId = ARGV[4]
local accessTtl = tonumber(ARGV[5])
local refreshId = ARGV[6]
local refreshTtl = tonumber(ARGV[7])
local sessionTtl = tonumber(ARGV[8])
local maxSessions = tonumber(ARGV[9])

local sessionKey = 'session:' .. sessionId
local tokensKey = sessionKey .. ':tokens'
local indexKey = 'profile:' .. profileId .. ':sessions'

local function wipe(id)
    local keyOfSession = 'session:' .. id
    for _, tokenId in ipairs(redis.call('SMEMBERS', keyOfSession .. ':tokens')) do
        redis.call('DEL', 'token:' .. tokenId)
    end
    for _, tombId in ipairs(redis.call('SMEMBERS', keyOfSession .. ':tombs')) do
        redis.call('DEL', 'tomb:' .. tombId)
    end
    redis.call('DEL', keyOfSession, keyOfSession .. ':tokens', keyOfSession .. ':tombs')
end

for _, member in ipairs(redis.call('ZRANGE', indexKey, 0, -1)) do
    if redis.call('EXISTS', 'session:' .. member) == 0 then
        redis.call('ZREM', indexKey, member)
    end
end

while redis.call('ZCARD', indexKey) >= maxSessions do
    local oldest = redis.call('ZRANGE', indexKey, 0, 0)
    wipe(oldest[1])
    redis.call('ZREM', indexKey, oldest[1])
end

redis.call('HSET', sessionKey, 'profileId', profileId, 'accessId', accessId)
redis.call('HSET', 'token:' .. accessId, 'profileId', profileId, 'use', 'access', 'sessionId', sessionId)
redis.call('PEXPIRE', 'token:' .. accessId, accessTtl)
redis.call('SADD', tokensKey, accessId)

if refreshId ~= '' then
    redis.call('HSET', sessionKey, 'refreshId', refreshId)
    redis.call('HSET', 'token:' .. accessId, 'linkedId', refreshId)
    redis.call('HSET', 'token:' .. refreshId,
        'profileId', profileId, 'use', 'refresh', 'sessionId', sessionId, 'linkedId', accessId)
    redis.call('PEXPIRE', 'token:' .. refreshId, refreshTtl)
    redis.call('SADD', tokensKey, refreshId)
end

redis.call('PEXPIRE', sessionKey, sessionTtl)
redis.call('PEXPIRE', tokensKey, sessionTtl)
redis.call('ZADD', indexKey, bornAtMs, sessionId)

if redis.call('PTTL', indexKey) < 0 then
    redis.call('PEXPIRE', indexKey, sessionTtl)
else
    redis.call('PEXPIRE', indexKey, sessionTtl, 'GT')
end

return 'opened'
