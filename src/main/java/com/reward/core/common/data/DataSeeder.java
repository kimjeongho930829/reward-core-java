package com.reward.core.common.data;

import com.reward.core.reward.domain.Reward;
import com.reward.core.reward.domain.RewardType;
import com.reward.core.reward.repository.RewardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final RewardRepository rewardRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (rewardRepository.count() > 0) {
            return;
        }

        log.info("테스트용 초기 보상 데이터 생성을 시작합니다.");

        Reward p10 = Reward.builder()
                .name("10% 확률 포인트 보상")
                .type(RewardType.POINT)
                .totalQuantity(100L)
                .remainingQuantity(100L)
                .weight(10)
                .build();

        Reward p20 = Reward.builder()
                .name("20% 확률 쿠폰 보상")
                .type(RewardType.COUPON)
                .totalQuantity(200L)
                .remainingQuantity(200L)
                .weight(20)
                .build();

        Reward p70 = Reward.builder()
                .name("70% 확률 꽝(포인트 소량)")
                .type(RewardType.POINT)
                .totalQuantity(700L)
                .remainingQuantity(700L)
                .weight(70)
                .build();

        rewardRepository.saveAll(List.of(p10, p20, p70));

        log.info("초기 데이터 생성 완료");
    }
}
