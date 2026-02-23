package com.reward.core.reward.service;

import com.reward.core.common.utils.DistributedRateLimiter;
import com.reward.core.reward.domain.Reward;
import com.reward.core.reward.domain.RewardType;
import com.reward.core.reward.repository.RewardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class RewardCompensatingTransactionTest {

    @Autowired
    private RewardService rewardService;

    @MockitoBean
    private RewardIssueService rewardIssueService;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private ValueOperations<String, String> valueOperations;

    @MockitoBean
    private RewardRepository rewardRepository;

    @MockitoBean
    private DistributedRateLimiter distributedRateLimiter;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(distributedRateLimiter.isAllowed(anyString(), anyInt(), anyInt())).thenReturn(true);
    }

    @Test
    @DisplayName("DB 처리 중 예외 발생 시 Redis의 참여 횟수가 보상 트랜잭션에 의해 롤백되어야 한다.")
    void participate_WhenDbFails_ShouldCompensateRedisCount() {
        // given
        Long userId = 1L;
        Long rewardId = 1L;
        
        // 1. 참여 횟수 체크 성공 (현재 1회)
        when(valueOperations.increment(anyString())).thenReturn(1L);
        
        // 2. 보상 목록 조회 성공
        Reward reward = Reward.builder().id(rewardId).name("테스트 보상").weight(100).build();
        when(rewardRepository.findAll()).thenReturn(java.util.List.of(reward));

        // 3. DB 처리 중 시스템 예외 발생 시뮬레이션
        doThrow(new RuntimeException("DB Timeout")).when(rewardIssueService).issue(anyLong(), anyLong());

        // when & then
        assertThatThrownBy(() -> rewardService.participate(userId))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("DB Timeout");

        // then: 보상 트랜잭션(decrement)이 호출되었는지 확인
        verify(valueOperations, times(1)).decrement(anyString());
    }
}
