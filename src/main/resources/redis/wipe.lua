-- Encerra todas as sessões de um perfil (troca de senha, exclusão da conta).
--
-- O índice é apagado junto: sem sessão viva não há o que indexar, e o access
-- que ainda não expirou deixa de autenticar na hora.
--
-- ARGV: 1 profileId
-- Retorno: 'wiped'

local indexKey = 'profile:' .. ARGV[1] .. ':sessions'

for _, member in ipairs(redis.call('ZRANGE', indexKey, 0, -1)) do
    local keyOfSession = 'session:' .. member
    for _, tokenId in ipairs(redis.call('SMEMBERS', keyOfSession .. ':tokens')) do
        redis.call('DEL', 'token:' .. tokenId)
    end
    for _, tombId in ipairs(redis.call('SMEMBERS', keyOfSession .. ':tombs')) do
        redis.call('DEL', 'tomb:' .. tombId)
    end
    redis.call('DEL', keyOfSession, keyOfSession .. ':tokens', keyOfSession .. ':tombs')
end

redis.call('DEL', indexKey)

return 'wiped'
