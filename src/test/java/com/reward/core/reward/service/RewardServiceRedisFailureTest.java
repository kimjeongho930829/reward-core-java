package com.reward.core.reward.service;

import com.reward.core.common.utils.DistributedRateLimiter;
import com.reward.core.reward.domain.Reward;
import com.reward.core.reward.domain.RewardType;
import com.reward.core.reward.repository.RewardHistoryRepository;
import com.reward.core.reward.repository.RewardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@SpringBootTest
class RewardServiceRedisFailureTest {

    @Autowired
    private RewardService rewardService;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RewardRepository rewardRepository;

    @Autowired
    private RewardHistoryRepository rewardHistoryRepository;

    private Long rewardId;

    @BeforeEach
    void setUp() {
        rewardHistoryRepository.deleteAll();
        rewardRepository.deleteAll();

        Reward reward = Reward.builder()
                .name("장애 대응 테스트 보상")
                .type(RewardType.POINT)
                .totalQuantity(10L)
                .remainingQuantity(10L)
                .weight(100)
                .build();
        
        rewardId = rewardRepository.save(reward).getId();
    }

    @Test
    @DisplayName("Redis 장애가 발생해도 로컬 Rate LimiterFallback을 통해 보상이 정상 지급되어야 한다.")
    void participate_WhenRedisFails_ShouldFallbackAndSucceed() {
        // given
        // Redis 호출 시 예외 발생 시뮬레이션 (Object... args 매칭을 위해 any(Object[].class) 사용)
        when(redisTemplate.execute(any(), anyList(), any(Object[].class)))
                .thenThrow(new RedisConnectionFailureException("Redis is down"));

        Long userId = 1L;

        // when
        rewardService.participate(userId);

        // then
        Reward reward = rewardRepository.findById(rewardId).orElseThrow();
        assertThat(reward.getRemainingQuantity()).isEqualTo(9L);
        assertThat(rewardHistoryRepository.count()).isEqualTo(1L);
    }
}
