-- Lista as sessões ativas de um perfil, da mais antiga para a mais recente.
--
-- O índice guarda o sessionId com o instante do login como score, mas sessões
-- podem morrer por TTL antes de sair de lá: o que já não existe é removido do
-- índice na passagem, como no open.lua.
--
-- ARGV: 1 profileId
-- Retorno: um registro por linha `sessionId|clientName|clientOs|clientDevice`,
--          registros separados por '\n', ou '' quando não há sessão ativa

local indexKey = 'profile:' .. ARGV[1] .. ':sessions'
local alive = {}

for _, member in ipairs(redis.call('ZRANGE', indexKey, 0, -1)) do
    local sessionKey = 'session:' .. member
    if redis.call('EXISTS', sessionKey) == 0 then
        redis.call('ZREM', indexKey, member)
    else
        local client = redis.call('HMGET', sessionKey, 'clientName', 'clientOs', 'clientDevice')
        alive[#alive + 1] = table.concat({
            member,
            client[1] or '',
            client[2] or '',
            client[3] or ''
        }, '|')
    end
end

return table.concat(alive, '\n')
