package com.reward.core.common.utils;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedRateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final RateLimiterRegistry rateLimiterRegistry;

    /**
     * Redis Sorted Set을 이용한 Sliding Window Rate Limiting
     * Fixed Window보다 시간 경계에서의 트래픽 급증을 더 정교하게 제어합니다.
     * @param key 제한할 키
     * @param limit 허용 횟수
     * @param windowSizeSeconds 윈도우 크기 (초)
     * @return 허용 여부
     */
    private static final String LUA_SCRIPT =
            "redis.call('zremrangebyscore', KEYS[1], 0, ARGV[1]) " +
            "local current_count = redis.call('zcard', KEYS[1]) " +
            "if tonumber(current_count) < tonumber(ARGV[2]) then " +
            "    redis.call('zadd', KEYS[1], ARGV[3], ARGV[3]) " +
            "    redis.call('expire', KEYS[1], ARGV[4]) " +
            "    return 1 " +
            "else " +
            "    return 0 " +
            "end";

    private final DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);

    public boolean isAllowed(String key, int limit, int windowSizeSeconds) {
        try {
            long now = System.currentTimeMillis();
            long windowStart = now - (windowSizeSeconds * 1000L);

            Long result = redisTemplate.execute(redisScript,
                    Collections.singletonList("rate_limit:sliding:" + key),
                    String.valueOf(windowStart),
                    String.valueOf(limit),
                    String.valueOf(now),
                    String.valueOf(windowSizeSeconds));

            return result != null && result == 1L;
        } catch (Exception e) {
            log.error("Redis 장애 발생으로 로컬 Rate Limiter로 Fallback 합니다. (Key: {})", key, e);
            return fallbackToLocalRateLimiter(key);
        }
    }

    /**
     * Redis 장애 시 Resilience4j 로컬 Rate Limiter를 사용
     */
    private boolean fallbackToLocalRateLimiter(String key) {
        RateLimiter localLimiter = rateLimiterRegistry.rateLimiter("localFallbackLimiter");
        return localLimiter.acquirePermission();
    }
}
