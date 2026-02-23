package com.reward.core.reward.service;

import com.reward.core.common.utils.DistributedRateLimiter;
import com.reward.core.common.utils.WeightedRandomPicker;
import com.reward.core.reward.domain.Reward;
import com.reward.core.reward.event.RewardIssuedEvent;
import com.reward.core.reward.repository.RewardRepository;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RewardService {

    private final RewardRepository rewardRepository;
    private final RewardIssueService rewardIssueService;
    private final ApplicationEventPublisher eventPublisher;
    private final DistributedRateLimiter distributedRateLimiter;
    private final UserParticipationService participationService;

    @RateLimiter(name = "rewardLimiter")
    public void participate(Long userId) {
        // 1. Redis 분산 처리율 제한 (1초에 10회 제한 시뮬레이션)
        if (!distributedRateLimiter.isAllowed("user:" + userId, 10, 1)) {
            log.warn("사용자 {} - 분산 처리율 제한에 걸렸습니다.", userId);
            throw new IllegalStateException("너무 빈번한 참여 요청입니다. 잠시 후 다시 시도해주세요.");
        }

        // 2. 일일 참여 횟수 선점 (Redis)
        participationService.checkAndIncrease(userId);

        try {
            // 3. 후보 보상 목록 조회 (캐시 활용)
            List<Reward> rewards = getAllRewards();
            WeightedRandomPicker picker = new WeightedRandomPicker(rewards);

            // 4. 가중치 기반 보상 선택
            Reward selectedReward = picker.pick();
            if (selectedReward == null) {
                log.info("사용자 {} - 당첨된 보상이 없습니다.", userId);
                return;
            }

            // 5. 실제 지급 처리 (비관적 락 적용된 서비스 호출)
            rewardIssueService.issue(userId, selectedReward.getId());

            // 6. 이벤트 발행 (알림 등 후속 처리를 위해)
            eventPublisher.publishEvent(new RewardIssuedEvent(userId, selectedReward.getName()));

        } catch (IllegalStateException e) {
            log.info("사용자 {} - 보상 지급 실패 (비즈니스 로직): {}", userId, e.getMessage());
            // 비즈니스 로직상 실패(수량 소진 등)는 정책에 따라 참여 횟수를 차감한 채로 둠
            throw e;
        } catch (Exception e) {
            log.error("사용자 {} - 시스템 오류 발생으로 보상 트랜잭션 실행: {}", userId, e.getMessage());
            // 시스템 오류(DB 장애 등) 발생 시 참여 횟수를 복구 (Compensating Transaction)
            participationService.compensate(userId);
            throw e;
        }
    }

    /**
     * 보상 목록 캐싱
     */
    @Cacheable(value = "rewards")
    public List<Reward> getAllRewards() {
        log.info("보상 목록을 조회합니다 (캐시 미적용 시 DB 접근)");
        return rewardRepository.findAll();
    }
}
