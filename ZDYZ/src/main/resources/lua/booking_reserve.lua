-- KEYS[1] = booking:stock:{timeMark}
-- ARGV[1] = need
-- ARGV[2] = bookedTtlSeconds
-- ARGV[3..] = booking:booked:{day}:{identityId}
-- 返回: 1 成功; -1 余票不足; -2 一证一约冲突; -3 请求内重复游客

local stockKey = KEYS[1]
local need = tonumber(ARGV[1])
local ttl = tonumber(ARGV[2])

local seen = {}
for i = 3, #ARGV do
    local key = ARGV[i]
    if seen[key] then
        return -3
    end
    seen[key] = true
end

local stock = tonumber(redis.call('GET', stockKey))
if stock == nil or stock < need then
    return -1
end

for i = 3, #ARGV do
    if redis.call('EXISTS', ARGV[i]) == 1 then
        return -2
    end
end

redis.call('DECRBY', stockKey, need)
for i = 3, #ARGV do
    if ttl ~= nil and ttl > 0 then
        redis.call('SET', ARGV[i], '1', 'EX', ttl)
    else
        redis.call('SET', ARGV[i], '1')
    end
end
return 1
