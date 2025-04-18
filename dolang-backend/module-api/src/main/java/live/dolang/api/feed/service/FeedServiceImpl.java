package live.dolang.api.feed.service;

import live.dolang.api.common.enums.SortType;
import live.dolang.api.common.exception.ForbiddenException;
import live.dolang.api.common.exception.NotFoundException;
import live.dolang.api.common.response.BaseResponseStatus;
import live.dolang.api.common.util.UTCTimeUtil;
import live.dolang.api.feed.dto.TodayFeedParticipantsResponseDto;
import live.dolang.api.feed.dto.TodayFeedResponseDto;
import live.dolang.api.feed.projection.TodayFeedProjection;
import live.dolang.api.feed.repository.FeedRepository;
import live.dolang.api.post.service.*;
import live.dolang.core.domain.date.repository.DateRepository;
import live.dolang.core.domain.language.repository.LanguageRepository;
import live.dolang.core.domain.language_level.repository.LanguageLevelRepository;
import live.dolang.core.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@AllArgsConstructor
public class FeedServiceImpl implements FeedService{

    private final UserService userService;
    private final CustomUserDateSentenceService customUserDateSentenceService;
    private final CustomDateSentenceService customDateSentenceService;
    private final UserBookmarkService userBookmarkService;
    private final PostBookmarkService postBookmarkService;
    private final UserHeartService userHeartService;
    private final PostHeartService postHeartService;

    private final FeedRepository feedRepository;
    private final LanguageRepository languageRepository;
    private final DateRepository dateRepository;
    private final LanguageLevelRepository languageLevelRepository;
    /**
     * 정렬 옵션(LATEST/LIKE)에 따라 서로 다른 방식으로 데이터 조회
     * - LATEST: DB(QueryDSL)에서 createdAt desc 로 조회 (커서 = user_date_sentence.id)
     * - LIKE  : Redis Sorted Set에서 좋아요(또는 점수) 순으로 user_date_sentence.id 목록을 가져온 뒤,
     *           다시 DB에서 해당 ID들의 레코드를 조회
     *
     * @param feedId     : 어떤 feed(date_sentence)인가
     * @param sort       : 정렬 옵션 (예: LATEST, LIKE)
     * @param length     : 가져올 데이터 개수
     * @param nextCursor : 커서 (user_date_sentence_id)
     */
    @Override
    public TodayFeedParticipantsResponseDto getTodayFeedParticipants(Integer userId,
                                                                     Integer feedId,
                                                                     SortType sort,
                                                                     Integer length,
                                                                     String nextCursor) {

        // 존재하지 않는 사용자
        if (userId != null && !userService.isUserExists(userId)) {
            throw new NotFoundException(BaseResponseStatus.NOT_EXIST_USER);
        }

        if (!customDateSentenceService.isDateSentenceExists(feedId)) {
            throw new NotFoundException(BaseResponseStatus.NOT_EXIST_FEED);
        }

        boolean isNativeFeed = userId != null && feedRepository.isNativeFeed(userId, feedId);
        TodayFeedParticipantsResponseDto dto = feedRepository.selectFeedParticipantsByLatest(userId, feedId, length, nextCursor);

        if (isNativeFeed) { // 모국어 피드 - 하트
            for(TodayFeedParticipantsResponseDto.FeedParticipant p : dto.getParticipants()) {
                p.setHeartCount(postHeartService.getPostHeartCount(feedId, p.getPostId()));
            }
        } else { // 외국어 피드 - 북마크
            for(TodayFeedParticipantsResponseDto.FeedParticipant p : dto.getParticipants()) {
                p.setBookmarkCount(postBookmarkService.getPostBookmarkCount(feedId, p.getPostId()));
            }
        }
        // 회원인 경우
        if (userId != null) {
            if (isNativeFeed) { // 모국어 피드 - 하트
                for(TodayFeedParticipantsResponseDto.FeedParticipant p : dto.getParticipants()) {
                    p.setIsUserHearted(userHeartService.isHearted(userId, feedId, p.getPostId()));
                }
            } else { // 외국어 피드 - 북마크
                for(TodayFeedParticipantsResponseDto.FeedParticipant p : dto.getParticipants()) {
                    p.setIsUserBookmarked(userBookmarkService.isBookmarked(userId, feedId, p.getPostId()));
                }
            }
        }
        return dto;
    }

    @Override
    public TodayFeedResponseDto getTodayFeed(Integer userId, String language, String languageLevel) {

        // 존재하지 않는 사용자
        if (userId != null && !userService.isUserExists(userId)) {
            throw new NotFoundException(BaseResponseStatus.NOT_EXIST_USER);
        }

        // 존재하지 않는 언어
        if (!languageRepository.existsById(language)) {
            throw new NotFoundException(BaseResponseStatus.NOT_EXIST_LANGUAGE);
        }

        // 존재하지 않는 날짜
        Instant todayUTCInstant = UTCTimeUtil.getTodayUTCInstant();
        if (!dateRepository.existsById(todayUTCInstant)) {
            throw new NotFoundException(BaseResponseStatus.NOT_EXIST_DATE);
        }

        // 존재하지 않는 레벨
        if (!languageLevelRepository.existsById(languageLevel)) {
            throw new NotFoundException(BaseResponseStatus.NOT_EXIST_LANGUAGE_LEVEL);
        }

        TodayFeedProjection proj = feedRepository.selectTodayFeed(userId, language, languageLevel, todayUTCInstant);
        Boolean isUserBookmarked = null;
        Boolean isNativeFeed = proj.getIsNativeFeed();

        if (userId != null) {
            if (!isNativeFeed) {
                // 사용자가 하나라도 어떤 기록을 남겼는지 본다.
                // 근데 이제 이게 모국어 피드인지는 모르는...
                boolean isRecordExist = customUserDateSentenceService.isUserDateSentenceExistsAt(userId, todayUTCInstant);
                if (!isRecordExist) {
                    throw new ForbiddenException(BaseResponseStatus.NOT_NATIVE_POST_UPLOADED);
                }
            }
            if (proj.getFeedId() != null && proj.getPostId() != null) {
                isUserBookmarked = userBookmarkService.isBookmarked(userId, proj.getFeedId(), proj.getPostId());
            }
        }

        TodayFeedResponseDto.Feed.FeedBuilder feedBuilder = TodayFeedResponseDto.Feed.builder()
                .date(UTCTimeUtil.formatInstant(proj.getDate()))
                .feedId(proj.getFeedId())
                .lang(proj.getLanguage())
                .isNativeFeed(isNativeFeed)
                .sentenceInfo(TodayFeedResponseDto.SentenceInfo.builder()
                        .sentence(proj.getSentence())
                        .level(proj.getLevel())
                        .build());

        // userId가 null이 아닐 때만 userParticipation 세팅
        if (userId != null) {
            feedBuilder.userParticipation(
                    TodayFeedResponseDto.UserParticipation.builder()
                            .postId(proj.getPostId())
                            .voiceUrl(proj.getVoiceUrl())
                            .voiceCreatedAt(UTCTimeUtil.formatInstant(proj.getVoiceCreatedAt()))
                            .build()
            );
        } else {
            feedBuilder.userParticipation(
                    TodayFeedResponseDto.UserParticipation.builder().build()
            ).build();
        }

        // 최종적으로 Feed 객체를 만들고, TodayFeedResponseDto에 세팅
        TodayFeedResponseDto dto = TodayFeedResponseDto.builder()
                .feed(feedBuilder.build())
                .build();

        if (userId == null) {
            return dto;
        }

        if (proj.getPostId() == null) return dto;
        // 모국어 피드에 따라 북마크 & 좋아요 관련 설정
        if (isNativeFeed) {
            dto.getFeed().getUserParticipation().setHeartCount(postHeartService.getPostHeartCount(proj.getFeedId(), proj.getPostId()));
        } else {
            dto.getFeed().getUserParticipation().setBookmarkCount(postBookmarkService.getPostBookmarkCount(proj.getFeedId(), proj.getPostId()));
        }
        dto.getFeed().getUserParticipation().setIsSelfBookmarked(isUserBookmarked);
        return dto;
    }


}
