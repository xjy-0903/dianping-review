-- 秒杀预扣库存脚本(Lua 保证原子性)
-- KEYS[1]: 秒杀库存key        seckill:stock:{voucherId}
-- KEYS[2]: 已购用户集合key     seckill:ordered:{voucherId}
-- KEYS[3]: 秒杀券元信息hash    seckill:voucher:{voucherId} 字段: beginTime/endTime(毫秒时间戳)
-- ARGV[1]: 用户id
-- ARGV[2]: 当前时间戳(ms)
-- 返回值: 0-成功 1-库存不足 2-重复下单 3-不在活动时间范围内
local beginTime = tonumber(redis.call('hget', KEYS[3], 'beginTime'))
local endTime = tonumber(redis.call('hget', KEYS[3], 'endTime'))
local now = tonumber(ARGV[2])
if not beginTime or not endTime or now < beginTime or now > endTime then
    return 3
end
local stock = redis.call('get', KEYS[1])
if not stock or tonumber(stock) <= 0 then
    return 1
end
if redis.call('sismember', KEYS[2], ARGV[1]) == 1 then
    return 2
end
redis.call('decr', KEYS[1])
redis.call('sadd', KEYS[2], ARGV[1])
return 0
