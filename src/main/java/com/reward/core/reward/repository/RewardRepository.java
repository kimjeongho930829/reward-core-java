package com.reward.core.reward.repository;

import com.reward.core.reward.domain.Reward;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardRepository extends JpaRepository<Reward, Long> {
}
