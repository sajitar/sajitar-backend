-- Lista as sessões ativas de um perfil, da mais antiga para a mais recente.
--
-- O índice guarda o sessionId com o instante do login como score, mas sessões
-- podem morrer por TTL antes de sair de lá: o que já não existe é removido do
-- índice na passagem, como no open.lua.
--
-- ARGV: 1 profileId
-- Retorno: sessionIds separados por '|', ou '' quando não há sessão ativa

local indexKey = 'profile:' .. ARGV[1] .. ':sessions'
local alive = {}

for _, member in ipairs(redis.call('ZRANGE', indexKey, 0, -1)) do
    if redis.call('EXISTS', 'session:' .. member) == 0 then
        redis.call('ZREM', indexKey, member)
    else
        alive[#alive + 1] = member
    end
end

return table.concat(alive, '|')
