package com.reward.core.campaign.batch;

import com.reward.core.reward.domain.Reward;
import com.reward.core.reward.domain.RewardHistory;
import com.reward.core.reward.repository.RewardHistoryRepository;
import com.reward.core.reward.repository.RewardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.LongStream;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RewardBatchConfig {

    private final RewardRepository rewardRepository;

    @Bean
    public Job rewardJob(JobRepository jobRepository, Step rewardStep) {
        return new JobBuilder("rewardJob", jobRepository)
                .start(rewardStep)
                .build();
    }

    @Bean
    public Step rewardStep(JobRepository jobRepository, 
                          PlatformTransactionManager transactionManager,
                          ItemProcessor<Long, RewardHistory> rewardProcessor) {
        return new StepBuilder("rewardStep", jobRepository)
                .<Long, RewardHistory>chunk(100, transactionManager) // Chunk 단위 처리 (메모리 효율)
                .reader(rewardReader())
                .processor(rewardProcessor)
                .writer(rewardWriter())
                .build();
    }

    /**
     * CSV 파일을 읽는 FlatFileItemReader로 쉽게 교체 가능
     */
    @Bean
    @StepScope
    public ListItemReader<Long> rewardReader() {
        // 시뮬레이션: 1000명의 사용자 ID 생성
        List<Long> userIds = LongStream.rangeClosed(1, 1000).boxed().toList();
        return new ListItemReader<>(userIds);
    }

    @Bean
    @StepScope
    public ItemProcessor<Long, RewardHistory> rewardProcessor(
            @org.springframework.beans.factory.annotation.Value("#{jobParameters['rewardId']}") Long rewardId
    ) {
        // JobParameters로 전달받은 보상 ID를 사용하여 실제 보상 정보를 로드
        Reward selectedReward = rewardRepository.findById(rewardId).orElse(null);
        
        return userId -> RewardHistory.builder()
                .userId(userId)
                .reward(selectedReward)
                .receivedAt(LocalDateTime.now())
                .build();
    }

    private final RewardHistoryRepository rewardHistoryRepository;

    @Bean
    public ItemWriter<RewardHistory> rewardWriter() {
        // Chunk 단위로 DB에 일괄 저장 (Bulk Insert 효과)
        return chunk -> {
            log.info("{} 건의 보상 이력을 Bulk Insert 중...", chunk.size());
            rewardHistoryRepository.saveAll(chunk.getItems());
            log.info("Batch Write 완료");
        };
    }
}
