package cn.ccg.redis.service;


import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RedisBaseService {

    protected RedisTemplate<String, Object> redisTemplate;

    public boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public Long incr(final String key,final long delta) {
        return redisTemplate.boundValueOps(key).increment(delta);
    }

    /**
     *
     */
    public boolean lock(String key, long timeout) {
        long min = System.currentTimeMillis();
        final long gap = timeout * 1000L;
        return redisTemplate.execute(new SessionCallback<Boolean>() {
            @SuppressWarnings({"unchecked" })
            @Override
            public Boolean execute(RedisOperations operations) throws DataAccessException {
                try {
                    while ((System.currentTimeMillis() - min) < gap) {
                        operations.watch(key);
                        Object value = operations.opsForValue().get(key);
                        if (value == null) {
                            operations.multi();
                            operations.opsForValue().set(key, Thread.currentThread().getName(), timeout, TimeUnit.SECONDS);
                            operations.exec();
                            try {
                                if (operations.opsForValue().get(key) != null && operations.opsForValue().get(key).equals(Thread.currentThread().getName())) {
                                    return true;
                                } else {
                                    operations.unwatch();
                                    Thread.sleep(10);
                                }
                            } catch (Exception e) {
                                //如果外面执行的时间小于了10ms，则有可能拿不到值，认为没有拿到锁处理
                                operations.unwatch();
                                Thread.sleep(10);
                            }
                        } else {
                            operations.unwatch();
                            Thread.sleep(10);
                        }
                    }
                } catch (Exception e) {
                    log.error("fail to lock", e);
                }
                return false;
            }
        });

    }

    public void unlock(String key) {
        del(key);
    }


    public void del(String key) {
        redisTemplate.delete(key);
    }

    public void expire(String key, Date expireTime) {
        if (expireTime != null) {
            redisTemplate.expireAt(key, expireTime);
        }
    }

    public void expire(String key, long expireSec) {
        if (expireSec > 0) {
            redisTemplate.expire(key, expireSec, TimeUnit.SECONDS);
        }
    }

    public Object getInfo(String part, String key) {
        return redisTemplate.execute(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                return connection.info(part).get(key);
            }
        });
    }

}
