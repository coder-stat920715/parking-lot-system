package com.parking.spot.lock;

import com.parking.spot.exception.LockAcquisitionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Distributed lock (SET key token NX PX lease) shared by ALL instances of this service.
 * Release uses a Lua compare-and-delete so a slow holder can never delete a lock now owned by someone else.
 *
 * If Redis is unreachable we degrade to the JVM-local lock and log a warning. Correctness is still guaranteed by
 * the @Version optimistic lock on ParkingSpot - the lock is a contention reducer, not the only safety net.
 */
@Slf4j
@Primary
@Component
@ConditionalOnProperty(prefix = "parking.lock", name = "type", havingValue = "redis", matchIfMissing = true)
public class RedisSpotLockService implements SpotLockService {

    private static final RedisScript<Long> UNLOCK_SCRIPT = RedisScript.of(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);
    private static final long RETRY_INTERVAL_MS = 20;

    private final StringRedisTemplate redis;
    private final LocalSpotLockService fallback;

    public RedisSpotLockService(StringRedisTemplate redis, LocalSpotLockService fallback) {
        this.redis = redis;
        this.fallback = fallback;
    }

    @Override
    public <T> T executeWithLock(String lockKey, Duration waitTimeout, Duration leaseTime, Supplier<T> action) {
        String redisKey = "parking:lock:" + lockKey;
        String token = UUID.randomUUID().toString();

        boolean acquired;
        try {
            acquired = tryAcquire(redisKey, token, waitTimeout, leaseTime);
        } catch (DataAccessException e) {
            log.warn("Redis unavailable ({}); falling back to JVM-local lock for {}", e.getMessage(), lockKey);
            return fallback.executeWithLock(lockKey, waitTimeout, leaseTime, action);
        }
        if (!acquired) {
            throw new LockAcquisitionException("Timed out waiting for distributed lock " + lockKey);
        }
        try {
            return action.get();
        } finally {
            release(redisKey, token);
        }
    }

    private boolean tryAcquire(String key, String token, Duration wait, Duration lease) {
        long deadline = System.nanoTime() + wait.toNanos();
        do {
            if (Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, token, lease))) {
                return true;
            }
            try {
                Thread.sleep(RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LockAcquisitionException("Interrupted while waiting for lock " + key, e);
            }
        } while (System.nanoTime() < deadline);
        return false;
    }

    private void release(String key, String token) {
        try {
            redis.execute(UNLOCK_SCRIPT, List.of(key), token);
        } catch (DataAccessException e) {
            log.warn("Failed to release lock {} (it will expire by lease): {}", key, e.getMessage());
        }
    }
}
