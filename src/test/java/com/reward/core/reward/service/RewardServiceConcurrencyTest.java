package com.reward.core.reward.service;

import com.reward.core.AbstractIntegrationTest;
import com.reward.core.reward.domain.Reward;
import com.reward.core.reward.domain.RewardType;
import com.reward.core.reward.repository.RewardHistoryRepository;
import com.reward.core.reward.repository.RewardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "resilience4j.ratelimiter.instances.rewardLimiter.limitForPeriod=1000"
})
class RewardServiceConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    private RewardService rewardService;

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
                .name("한정판 보상")
                .type(RewardType.COUPON)
                .totalQuantity(100L)
                .remainingQuantity(100L)
                .weight(100) // 100% 당첨되도록 설정
                .build();
        
        rewardId = rewardRepository.save(reward).getId();
    }

    @Test
    @DisplayName("100개의 보상에 대해 200명이 동시에 요청하면 정확히 100개만 지급되어야 한다.")
    void participate_WithConcurrency_ShouldLimitRewards() throws InterruptedException {
        // given
        int threadCount = 200;
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        // Java 21 가상 스레드 활용 (try-with-resources로 안전한 리소스 해제)
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < threadCount; i++) {
                long userId = i + 1;
                executorService.submit(() -> {
                    try {
                        rewardService.participate(userId);
                    } catch (Exception e) {
                        // Rate Limit 또는 소진 예외 발생 가능
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
        }

        // then
        Reward reward = rewardRepository.findById(rewardId).orElseThrow();
        System.out.println("Remaining Quantity: " + reward.getRemainingQuantity());
        System.out.println("History Count: " + rewardHistoryRepository.count());
        
        assertThat(reward.getRemainingQuantity()).isEqualTo(0L);
        
        long historyCount = rewardHistoryRepository.count();
        assertThat(historyCount).isEqualTo(100L);
    }
}
