package com.reward.core.reward.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.reward.core.reward.domain.Reward;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static com.reward.core.reward.domain.QReward.reward;

@RequiredArgsConstructor
public class RewardRepositoryImpl implements RewardRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Reward> findByIdWithLock(Long id) {
        return Optional.ofNullable(queryFactory
                .selectFrom(reward)
                .where(reward.id.eq(id))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setHint("jakarta.persistence.lock.timeout", 3000)
                .fetchOne());
    }
}
