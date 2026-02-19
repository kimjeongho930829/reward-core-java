package com.reward.core.reward.event;

public record RewardIssuedEvent(
    Long userId,
    String rewardName
) {
}
