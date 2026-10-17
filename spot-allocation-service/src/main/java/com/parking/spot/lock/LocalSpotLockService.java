package com.parking.spot.lock;

import com.parking.spot.exception.LockAcquisitionException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/** JVM-local fair ReentrantLock per key. Protects threads of ONE instance only. */
@Component
public class LocalSpotLockService implements SpotLockService {

    private final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public <T> T executeWithLock(String lockKey, Duration waitTimeout, Duration leaseTime, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(lockKey, k -> new ReentrantLock(true));
        boolean acquired;
        try {
            acquired = lock.tryLock(waitTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException("Interrupted while waiting for lock " + lockKey, e);
        }
        if (!acquired) {
            throw new LockAcquisitionException("Timed out waiting for lock " + lockKey);
        }
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
