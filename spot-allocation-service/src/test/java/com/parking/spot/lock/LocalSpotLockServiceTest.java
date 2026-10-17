package com.parking.spot.lock;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class LocalSpotLockServiceTest {

    @Test
    void providesMutualExclusionUnderContention() throws Exception {
        LocalSpotLockService lockService = new LocalSpotLockService();
        int[] counter = {0};                      // deliberately NOT thread-safe
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < 8; t++) {
            futures.add(pool.submit(() -> {
                for (int i = 0; i < 1000; i++) {
                    lockService.executeWithLock("k", Duration.ofSeconds(10), Duration.ofSeconds(10), () -> {
                        counter[0]++;
                        return null;
                    });
                }
            }));
        }
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(counter[0]).isEqualTo(8000);
    }
}
