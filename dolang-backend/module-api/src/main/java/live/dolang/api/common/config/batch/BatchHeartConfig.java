package live.dolang.api.common.config.batch;

import live.dolang.api.post.dto.HeartDataDto;
import live.dolang.core.domain.date_sentence.DateSentence;
import live.dolang.core.domain.date_sentence.repository.DateSentenceRepository;
import live.dolang.core.domain.user.User;
import live.dolang.core.domain.user.repository.UserRepository;
import live.dolang.core.domain.user_date_sentence.UserDateSentence;
import live.dolang.core.domain.user_date_sentence.repository.UserDateSentenceRepository;
import live.dolang.core.domain.user_sentence_like_log.UserSentenceLikeLog;
import live.dolang.core.domain.user_sentence_like_log.repository.UserSentenceLikeLogRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.IteratorItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class BatchHeartConfig {

    @Value("${spring.data.redis.user.prefix}")
    private String userPrefix; // 예: "user:"

    @Value("${spring.data.redis.heart.postfix}")
    private String heartPostfix;    // 예: ":heart"

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;
    private final DateSentenceRepository dateSentenceRepository;
    private final UserDateSentenceRepository userDateSentenceRepository;
    private final UserSentenceLikeLogRepository userSentenceLikeLogRepository;

    /**
     * Redis에 저장된 dirty 플래그가 있는 북마크 데이터를 DB에 반영하는 Job
     */
    @Bean
    public Job redisHeartToDatabaseJob() {
        return new JobBuilder("redisHeartToDatabaseJob", jobRepository)
                .start(redisHeartToDatabaseStep())
                .build();
    }

    /**
     * Step 구성
     * Chunk 단위로 BookmarkLogWrapper를 읽고 처리한 후 DB 저장 및 dirty 플래그 제거
     */
    @Bean
    public Step redisHeartToDatabaseStep() {
        return new StepBuilder("redisHeartToDatabaseStep", jobRepository)
                .<HeartLogWrapper, HeartLogWrapper>chunk(100, transactionManager)
                .reader(redisHeartItemReader())
                .processor(redisHeartItemProcessor())
                .writer(redisHeartItemWriter())
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .skipLimit(10)
                .skip(Exception.class)
                .build();
    }

    // ItemReader: Redis에서 dirty 플래그가 있는 각 데이터 항목(키와 필드)을 읽어 BookmarkLogWrapper 목록을 생성
    @Bean
    @StepScope
    public ItemReader<HeartLogWrapper> redisHeartItemReader() {
        List<HeartLogWrapper> wrapperList = new ArrayList<>();
        // 키 패턴: "user:*:feed:*{heartPostfix}"
        String pattern = userPrefix + "*:feed:*" + heartPostfix;
        Set<String> dataKeys = redisTemplate.keys(pattern);

        if (dataKeys == null) {
            return new IteratorItemReader<>(wrapperList);
        }

        for (String dataKey : dataKeys) {
            String[] parts = dataKey.split(":");
            if (parts.length < 5) {
                continue;
            }
            int userId;
            int feedId;
            try {
                userId = Integer.parseInt(parts[1]);
                feedId = Integer.parseInt(parts[3]);
            } catch (NumberFormatException e) {
                continue;
            }

            // dirty 세트 키: dataKey + ":dirty"
            String dirtySetKey = dataKey + ":dirty";
            Set<Object> dirtyFields = redisTemplate.opsForSet().members(dirtySetKey);
            if (dirtyFields == null || dirtyFields.isEmpty()) {
                continue;
            }

            // dirty 세트에 등록된 각 필드(포스트 ID)에 대해 로그 생성
            for (Object fieldObj : dirtyFields) {
                String field = (String) fieldObj;
                Object value = redisTemplate.opsForHash().get(dataKey, field);
                if (!(value instanceof HeartDataDto heartData)) {
                    if (value != null) {
                        System.err.println("Unexpected data type in Redis: " + value.getClass());
                    }
                    continue;
                }
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    System.err.println("User not found for ID: " + userId);
                    continue;
                }
                int sentenceId;
                try {
                    sentenceId = Integer.parseInt(field);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid sentence ID format: " + field);
                    continue;
                }

                DateSentence dateSentence = dateSentenceRepository.findById(feedId).orElse(null);
                if (dateSentence == null) {
                    System.err.println("DateSentence not found for ID: " + feedId);
                    continue;
                }

                UserDateSentence userDateSentence = userDateSentenceRepository.findById(sentenceId).orElse(null);
                if (userDateSentence == null) {
                    System.err.println("UserDateSentence not found for ID: " + sentenceId);
                    continue;
                }

                LocalDateTime createdAt = LocalDateTime.ofEpochSecond(heartData.getTimestamp(), 0, ZoneOffset.UTC);
                Instant instant = createdAt.atZone(ZoneOffset.UTC).toInstant();
                UserSentenceLikeLog log = UserSentenceLikeLog.builder()
                        .user(user)
                        .dateSentence(dateSentence)
                        .userDateSentence(userDateSentence)
                        .likeYn(heartData.isHearted())
                        .createdAt(instant)
                        .build();
                // 각 dirty 항목과 관련된 dataKey와 field 정보를 함께 전달할 수 있도록 Wrapper에 담음
                HeartLogWrapper wrapper = new HeartLogWrapper(log, dataKey, field);
                wrapperList.add(wrapper);
            }
        }
        return new IteratorItemReader<>(wrapperList);
    }

    @Bean
    public ItemProcessor<HeartLogWrapper, HeartLogWrapper> redisHeartItemProcessor() {
        return wrapper -> wrapper;
    }

    //ItemWriter: 로그를 DB에 저장한 후, 해당 dirty 플래그를 Redis에서 제거
    @Bean
    public ItemWriter<HeartLogWrapper> redisHeartItemWriter() {
        return wrappers -> {
            List<UserSentenceLikeLog> logsToSave = new ArrayList<>();
            for (HeartLogWrapper wrapper : wrappers) {
                logsToSave.add(wrapper.getLog());
            }
            // DB 저장
            userSentenceLikeLogRepository.saveAll(logsToSave);
            // 저장 후, 각 항목의 dirty 플래그 제거
            for (HeartLogWrapper wrapper : wrappers) {
                String dirtySetKey = wrapper.getDataKey() + ":dirty";
                redisTemplate.opsForSet().remove(dirtySetKey, wrapper.getField());
            }
        };
    }

    /**
     * Wrapper 클래스: Redis의 데이터 키, 필드와 DB에 저장할 로그 엔티티를 함께 보관
     */
    @Getter
    @AllArgsConstructor
    public static class HeartLogWrapper {
        private final UserSentenceLikeLog log;
        private final String dataKey;
        private final String field;
    }
}
