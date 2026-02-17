package com.reward.core.reward.service;

import com.reward.core.common.utils.WeightedRandomPicker;
import com.reward.core.reward.domain.Reward;
import com.reward.core.reward.repository.RewardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RewardService {

    private final RewardRepository rewardRepository;
    private final RewardIssueService rewardIssueService;

    public void participate(Long userId) {
        log.info("사용자 {} - 보상 참여 요청", userId);

        // 1. 후보 보상 목록 조회
        List<Reward> rewards = rewardRepository.findAll();
        WeightedRandomPicker picker = new WeightedRandomPicker(rewards);

        // 2. 가중치 기반 보상 선택
        Reward selectedReward = picker.pick();
        if (selectedReward == null) {
            log.info("사용자 {} - 당첨된 보상이 없습니다.", userId);
            return;
        }

        // 3. 실제 지급 처리 (비관적 락 적용된 서비스 호출)
        rewardIssueService.issue(userId, selectedReward.getId());
        
        log.info("사용자 {} - 보상 참여 프로세스 완료", userId);
    }
}
