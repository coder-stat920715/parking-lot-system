package com.parking.spot.lock;

import java.time.Duration;
import java.util.function.Supplier;

/** Lock abstraction so the allocation logic does not care whether the lock is JVM-local or distributed. */
public interface SpotLockService {

    <T> T executeWithLock(String lockKey, Duration waitTimeout, Duration leaseTime, Supplier<T> action);
}
