package com.reward.core.reward.repository;

import com.reward.core.reward.domain.RewardHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardHistoryRepository extends JpaRepository<RewardHistory, Long> {
}
