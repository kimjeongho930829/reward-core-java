package com.reward.core.reward.event;

import com.reward.core.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RewardEventListener {

    private final NotificationService notificationService;

    @Async // 가상 스레드에서 비동기 처리
    @EventListener
    public void handleRewardIssued(RewardIssuedEvent event) {
        log.info("보상 지급 이벤트 감지: 사용자 {}", event.userId());
        notificationService.sendNotification(event.userId(), event.rewardName() + " 당첨을 축하드립니다!");
    }
}
