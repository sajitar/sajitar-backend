-- Encerra em lote as sessões informadas, tudo ou nada.
--
-- A primeira passagem só confere: toda sessão precisa existir e pertencer ao
-- perfil do Bearer, senão nada é encerrado. Ids repetidos são inofensivos nas
-- duas passagens.
--
-- ARGV: 1 profileId, 2..N sessionIds
-- Retorno: 'closed' | 'absent'

local profileId = ARGV[1]
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
    redis.call('ZREM', indexKey, id)
end

for index = 2, #ARGV do
    if redis.call('HGET', 'session:' .. ARGV[index], 'profileId') ~= profileId then
        return 'absent'
    end
end

for index = 2, #ARGV do
    wipe(ARGV[index])
end

return 'closed'
