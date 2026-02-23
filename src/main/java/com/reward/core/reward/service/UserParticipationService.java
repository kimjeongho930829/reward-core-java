package com.reward.core.reward.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserParticipationService {

    private final StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "user:participation:";
    private static final int MAX_PARTICIPATION_PER_DAY = 3;

    /**
     * 유저의 일일 참여 횟수를 확인하고 증가시킵니다.
     * @param userId 유저 ID
     * @throws IllegalStateException 참여 횟수 초과 시
     */
    public void checkAndIncrease(Long userId) {
        String key = generateKey(userId);
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofDays(1));
        }

        if (count != null && count > MAX_PARTICIPATION_PER_DAY) {
            // 한도 초과 시 다시 감소시키고 예외 발생
            redisTemplate.opsForValue().decrement(key);
            log.warn("사용자 {} - 일일 참여 한도 초과 ({}회)", userId, MAX_PARTICIPATION_PER_DAY);
            throw new IllegalStateException("오늘은 더 이상 참여할 수 없습니다. (일일 최대 3회)");
        }
        
        log.info("사용자 {} - 참여 횟수 증가 (현재: {}회)", userId, count);
    }

    /**
     * 보상 트랜잭션: 실패 시 참여 횟수를 다시 감소시킵니다.
     * @param userId 유저 ID
     */
    public void compensate(Long userId) {
        String key = generateKey(userId);
        redisTemplate.opsForValue().decrement(key);
        log.info("사용자 {} - 보상 트랜잭션 실행: 참여 횟수 롤백", userId);
    }

    private String generateKey(Long userId) {
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return KEY_PREFIX + userId + ":" + today;
    }
}
