-- Conta uma tentativa na janela do escopo. Na primeira da janela grava o TTL;
-- quando o contador passa do teto, devolve a espera restante em milissegundos.
--
-- ARGV: 1 key, 2 max, 3 windowMs
-- Retorno: '' quando ainda cabe, senão o PTTL em milissegundos

local key = ARGV[1]
local max = tonumber(ARGV[2])
local windowMs = tonumber(ARGV[3])
local count = redis.call('INCR', key)

if count == 1 then
    redis.call('PEXPIRE', key, windowMs)
end

if count > max then
    return tostring(redis.call('PTTL', key))
end

return ''
