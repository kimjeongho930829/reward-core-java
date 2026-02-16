package com.reward.core.reward.service;

import com.reward.core.reward.domain.Reward;
import com.reward.core.reward.domain.RewardHistory;
import com.reward.core.reward.repository.RewardHistoryRepository;
import com.reward.core.reward.repository.RewardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RewardIssueService {

    private final RewardRepository rewardRepository;
    private final RewardHistoryRepository rewardHistoryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void issue(Long userId, Long rewardId) {
        // 이 시점에 DB에서 신규 트랜잭션으로 락을 걸고 읽어옴
        Reward rewardWithLock = rewardRepository.findByIdWithLock(rewardId)
                .orElseThrow(() -> new IllegalArgumentException("보상을 찾을 수 없습니다."));

        rewardWithLock.decreaseQuantity();

        RewardHistory history = RewardHistory.builder()
                .userId(userId)
                .reward(rewardWithLock)
                .receivedAt(LocalDateTime.now())
                .build();
        
        rewardHistoryRepository.save(history);
        log.info("사용자 {} - 보상 '{}' 지급 완료 (잔여: {})", 
                userId, rewardWithLock.getName(), rewardWithLock.getRemainingQuantity());
    }
}
