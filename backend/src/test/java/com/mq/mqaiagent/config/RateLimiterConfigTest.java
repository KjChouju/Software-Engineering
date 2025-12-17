package com.mq.mqaiagent.config;

import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterConfigTest {

    @Test
    void testUserRateLimiterManager_getRateLimiter_sameInstancePerUser() {
        RateLimiterConfig.UserRateLimiterManager manager = new RateLimiterConfig.UserRateLimiterManager();
        RateLimiter l1 = manager.getRateLimiter(1L);
        RateLimiter l2 = manager.getRateLimiter(1L);
        RateLimiter l3 = manager.getRateLimiter(2L);

        assertSame(l1, l2);
        assertNotSame(l1, l3);
    }

    @Test
    void testUserRateLimiterManager_tryAcquire_success() {
        RateLimiterConfig.UserRateLimiterManager manager = new RateLimiterConfig.UserRateLimiterManager();
        boolean ok = manager.tryAcquire(100L, 1, TimeUnit.SECONDS);
        assertTrue(ok);
    }

    @Test
    void testUserRateLimiterManager_tryAcquire_eventuallyFalseWithZeroTimeout() {
        RateLimiterConfig.UserRateLimiterManager manager = new RateLimiterConfig.UserRateLimiterManager();
        List<Boolean> results = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            results.add(manager.tryAcquire(200L, 0, TimeUnit.MILLISECONDS));
        }
        assertTrue(results.stream().anyMatch(b -> !b));
    }
}

