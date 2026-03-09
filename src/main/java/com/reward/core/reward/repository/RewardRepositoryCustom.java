package com.reward.core.reward.repository;

import com.reward.core.reward.domain.Reward;
import java.util.Optional;

public interface RewardRepositoryCustom {
    Optional<Reward> findByIdWithLock(Long id);
}
