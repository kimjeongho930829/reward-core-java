package com.reward.core.common.utils;

import com.reward.core.reward.domain.Reward;
import com.reward.core.reward.domain.RewardType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class WeightedRandomPickerTest {

    @Test
    @DisplayName("가중치 설정에 따라 통계적으로 보상이 선택되어야 한다.")
    void pick_WithWeights_ShouldFollowProbability() {
        // given
        Reward r1 = Reward.builder().id(1L).name("10%").weight(10).type(RewardType.POINT).build();
        Reward r2 = Reward.builder().id(2L).name("20%").weight(20).type(RewardType.POINT).build();
        Reward r3 = Reward.builder().id(3L).name("70%").weight(70).type(RewardType.POINT).build();
        
        List<Reward> rewards = List.of(r1, r2, r3);
        WeightedRandomPicker picker = new WeightedRandomPicker(rewards);

        int iterations = 100_000;
        Map<String, Integer> counts = new HashMap<>();

        // when
        for (int i = 0; i < iterations; i++) {
            Reward picked = picker.pick();
            counts.put(picked.getName(), counts.getOrDefault(picked.getName(), 0) + 1);
        }

        // then
        // 10% ± 1% 범위 내에 있는지 확인
        assertThat((double) counts.get("10%") / iterations).isCloseTo(0.10, within(0.01));
        assertThat((double) counts.get("20%") / iterations).isCloseTo(0.20, within(0.01));
        assertThat((double) counts.get("70%") / iterations).isCloseTo(0.70, within(0.01));
    }
}
