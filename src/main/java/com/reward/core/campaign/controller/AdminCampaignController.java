package com.reward.core.campaign.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/campaign")
@RequiredArgsConstructor
public class AdminCampaignController {

    private final JobLauncher jobLauncher;
    private final Job rewardJob;

    @PostMapping("/start")
    public ResponseEntity<String> startBatch(@org.springframework.web.bind.annotation.RequestParam("rewardId") Long rewardId) {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("rewardId", rewardId)
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();
            
            jobLauncher.run(rewardJob, jobParameters);
            
            return ResponseEntity.ok("배치 작업이 성공적으로 시작되었습니다.");
        } catch (Exception e) {
            log.error("배치 작업 실행 중 오류 발생", e);
            return ResponseEntity.internalServerError().body("배치 작업 실행 실패: " + e.getMessage());
        }
    }
}
