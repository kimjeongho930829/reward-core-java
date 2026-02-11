package com.reward.core.common.utils;

import com.reward.core.reward.domain.Reward;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

public class WeightedRandomPicker {

    private final NavigableMap<Integer, Reward> map = new TreeMap<>();
    private int totalWeight = 0;

    public WeightedRandomPicker(List<Reward> rewards) {
        for (Reward reward : rewards) {
            if (reward.getWeight() > 0) {
                totalWeight += reward.getWeight();
                map.put(totalWeight, reward);
            }
        }
    }

    public Reward pick() {
        if (totalWeight <= 0) {
            return null;
        }
        int randomValue = ThreadLocalRandom.current().nextInt(totalWeight) + 1;
        return map.ceilingEntry(randomValue).getValue();
    }
}
