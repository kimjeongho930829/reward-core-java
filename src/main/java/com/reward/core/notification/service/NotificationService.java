package com.reward.core.notification.service;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class NotificationService {

    /**
     * 가상 스레드에서 실행되는 비동기 알림 발송 메서드
     * Bulkhead를 통해 FCM API 동시 요청 수를 제어 (Node.js의 p-map 역할)
     */
    @Async
    @Bulkhead(name = "notificationBulkhead")
    public void sendNotification(Long userId, String message) {
        log.info("[VirtualThread: {}] 사용자 {}에게 알림 발송 중: {}", 
                Thread.currentThread().isVirtual(), userId, message);

        try {
            // 외부 API 호출 시뮬레이션
            Thread.sleep(100); 

            // FCM 에러 시뮬레이션 (10% 확률로 실패)
            if (ThreadLocalRandom.current().nextInt(10) == 0) {
                throw new RuntimeException("INVALID_TOKEN");
            }

            log.info("사용자 {} 알림 발송 완료", userId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("사용자 {} 알림 발송 실패: {}", userId, e.getMessage());
            purgeToken(userId);
        }
    }

    private void purgeToken(Long userId) {
        log.warn("사용자 {}의 만료된 토큰을 DB에서 삭제합니다.", userId);
        // 실제 운영 환경에서는 Repository를 호출하여 토큰 삭제 로직 수행
    }
}
