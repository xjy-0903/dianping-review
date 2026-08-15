-- 释放互斥锁(校验令牌,防止误删他人锁)
-- KEYS[1]: 锁key  ARGV[1]: 令牌
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
end
return 0
