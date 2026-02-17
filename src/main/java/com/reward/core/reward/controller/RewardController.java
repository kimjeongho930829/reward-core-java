package com.reward.core.reward.controller;

import com.reward.core.reward.service.RewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rewards")
@RequiredArgsConstructor
public class RewardController {

    private final RewardService rewardService;

    @PostMapping("/participate")
    public ResponseEntity<String> participate(@RequestParam("userId") Long userId) {
        rewardService.participate(userId);
        return ResponseEntity.ok("참여 완료");
    }
}
