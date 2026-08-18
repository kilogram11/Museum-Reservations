-- KEYS[1] = booking:stock:{timeMark}
-- ARGV[1] = need
-- ARGV[2..] = booking:booked:{day}:{identityId}
-- 返回: 1

local stockKey = KEYS[1]
local need = tonumber(ARGV[1])
redis.call('INCRBY', stockKey, need)
for i = 2, #ARGV do
    redis.call('DEL', ARGV[i])
end
return 1
