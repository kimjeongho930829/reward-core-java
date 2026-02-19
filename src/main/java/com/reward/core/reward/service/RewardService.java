package com.reward.core.reward.service;

import com.reward.core.common.utils.DistributedRateLimiter;
import com.reward.core.common.utils.WeightedRandomPicker;
import com.reward.core.reward.domain.Reward;
import com.reward.core.reward.event.RewardIssuedEvent;
import com.reward.core.reward.repository.RewardRepository;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RewardService {

    private final RewardRepository rewardRepository;
    private final RewardIssueService rewardIssueService;
    private final DistributedRateLimiter distributedRateLimiter;
    private final ApplicationEventPublisher eventPublisher;

    @RateLimiter(name = "rewardLimiter")
    public void participate(Long userId) {
        // 1. Redis 분산 처리율 제한 (1초에 10회 제한 시뮬레이션)
        if (!distributedRateLimiter.isAllowed("user:" + userId, 10, 1)) {
            log.warn("사용자 {} - 분산 처리율 제한에 걸렸습니다.", userId);
            throw new IllegalStateException("너무 빈번한 참여 요청입니다. 잠시 후 다시 시도해주세요.");
        }

        log.info("사용자 {} - 보상 참여 요청", userId);

        // 2. 후보 보상 목록 조회
        List<Reward> rewards = rewardRepository.findAll();
        WeightedRandomPicker picker = new WeightedRandomPicker(rewards);

        // 3. 가중치 기반 보상 선택
        Reward selectedReward = picker.pick();
        if (selectedReward == null) {
            log.info("사용자 {} - 당첨된 보상이 없습니다.", userId);
            return;
        }

        // 4. 실제 지급 처리 (비관적 락 적용된 서비스 호출)
        rewardIssueService.issue(userId, selectedReward.getId());

        // 5. 이벤트 발행 (알림 등 후속 처리를 위해)
        eventPublisher.publishEvent(new RewardIssuedEvent(userId, selectedReward.getName()));
        
        log.info("사용자 {} - 보상 참여 프로세스 완료", userId);
    }
}
