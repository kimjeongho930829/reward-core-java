package com.reward.core.reward.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rewards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Reward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RewardType type;

    @Column(nullable = false)
    private Long totalQuantity;

    @Column(nullable = false)
    private Long remainingQuantity;

    @Column(nullable = false)
    private Integer weight; // 가중치 (예: 10, 20, 70)

    @Version
    private Long version; // 낙관적 락(Optimistic Lock)을 위한 버전 관리

    public void decreaseQuantity() {
        if (this.remainingQuantity <= 0) {
            throw new IllegalStateException("보상 수량이 소진되었습니다.");
        }
        this.remainingQuantity--;
    }
}
