package com.reward.core.campaign.batch;

import com.reward.core.reward.domain.Reward;
import com.reward.core.reward.domain.RewardType;
import com.reward.core.reward.repository.RewardHistoryRepository;
import com.reward.core.AbstractIntegrationTest;
import com.reward.core.reward.repository.RewardRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
class RewardBatchIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private RewardRepository rewardRepository;

    @Autowired
    private RewardHistoryRepository rewardHistoryRepository;

    @Test
    @DisplayName("보상 지급 배치 작업이 성공적으로 완료되어야 한다.")
    void rewardJob_Success() throws Exception {
        // given
        Reward reward = Reward.builder()
                .name("배치 보상")
                .type(RewardType.POINT)
                .totalQuantity(1000L)
                .remainingQuantity(1000L)
                .weight(100)
                .build();
        Long rewardId = rewardRepository.save(reward).getId();

        // when
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("rewardId", rewardId)
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // then
        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
        assertThat(rewardHistoryRepository.count()).isEqualTo(1000L);
    }
}
