package com.mechuragi.mechuragi_server.domain.vote.service;

import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.member.repository.MemberRepository;
import com.mechuragi.mechuragi_server.domain.notification.dto.VoteNotificationMessageDTO;
import com.mechuragi.mechuragi_server.domain.notification.dto.VoteNotificationType;
import com.mechuragi.mechuragi_server.domain.notification.event.VoteCompletedEvent;
import com.mechuragi.mechuragi_server.domain.notification.entity.Notification;
import com.mechuragi.mechuragi_server.domain.notification.service.NotificationService;
import com.mechuragi.mechuragi_server.domain.vote.dto.*;
import com.mechuragi.mechuragi_server.domain.vote.entity.VoteOption;
import com.mechuragi.mechuragi_server.domain.vote.entity.VotePost;
import com.mechuragi.mechuragi_server.domain.vote.entity.VotePost.VoteStatus;
import com.mechuragi.mechuragi_server.domain.vote.repository.VotePostRepository;
import com.mechuragi.mechuragi_server.global.exception.BusinessException;
import com.mechuragi.mechuragi_server.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VotePostService {

    private static final String VOTE_EXPIRE_KEY_PREFIX = "vote:expire:";
    private static final String VOTE_SOON_KEY_PREFIX = "vote:soon:";
    private static final long SOON_NOTIFICATION_MINUTES = 10;

    private final VotePostRepository votePostRepository;
    private final MemberRepository memberRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisTemplate<String, Object> redisPubSubTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;

    /**
     * 투표 생성
     */
    @Transactional
    public VoteResponseDTO createVote(Long authorId, VoteCreateRequestDTO request) {
        Member author = memberRepository.findById(authorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        boolean hasImage = request.getOptions().stream()
                .anyMatch(option -> option.getImageUrl() != null && !option.getImageUrl().trim().isEmpty());

        VotePost votePost = VotePost.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .deadline(request.getDeadline()) // Instant 그대로
                .allowMultipleChoice(request.getAllowMultipleChoice())
                .author(author)
                .voteType(hasImage ? VotePost.VoteType.IMAGE : VotePost.VoteType.TEXT)
                .build();

        List<VoteOption> voteOptions = new ArrayList<>();
        for (int i = 0; i < request.getOptions().size(); i++) {
            var option = request.getOptions().get(i);
            voteOptions.add(VoteOption.builder()
                    .optionText(option.getOptionText())
                    .imageUrl(option.getImageUrl())
                    .displayOrder(i + 1)
                    .votePost(votePost)
                    .build());
        }

        votePost.getVoteOptions().addAll(voteOptions);
        VotePost savedVotePost = votePostRepository.save(votePost);

        // Redis TTL 키 등록 (Keyspace Notifications 기반 알림용)
        registerVoteExpirationKeys(savedVotePost);

        return VoteResponseDTO.from(savedVotePost, redisTemplate);
    }

    public VoteResponseDTO getVote(Long voteId) {
        VotePost votePost = votePostRepository.findById(voteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));

        return VoteResponseDTO.from(votePost, redisTemplate);
    }

    /**
     * 모든 투표 조회 (최신순)
     */
    public Page<VoteResponseDTO> getActiveVotes(Pageable pageable) {
        Page<VotePost> votePosts = votePostRepository.findAllOrderByCreatedAtDesc(pageable);
        log.info("조회된 투표 수: {}", votePosts.getTotalElements());
        return votePosts.map(v -> VoteResponseDTO.from(v, redisTemplate));
    }

    /**
     * 완료된 투표 조회
     */
    public Page<VoteResponseDTO> getCompletedVotes(Pageable pageable) {
        Instant now = Instant.now();
        Page<VotePost> votePosts = votePostRepository.findCompletedVotes(now, pageable);
        return votePosts.map(v -> VoteResponseDTO.from(v, redisTemplate));
    }

    /**
     * 특정 유저 작성 투표 목록
     */
    public Page<VoteResponseDTO> getUserVotes(Long userId, Pageable pageable) {
        Page<VotePost> votePosts = votePostRepository.findByAuthorIdOrderByCreatedAtDesc(userId, pageable);
        return votePosts.map(v -> VoteResponseDTO.from(v, redisTemplate));
    }

    /**
     * 핫한 투표 조회 (Redis 기반, 부족 시 DB 폴백)
     */
    @Cacheable(value = "hotVotes", key = "'list:' + #size")
    public List<VoteResponseDTO> getHotVotes(int size) {
        Instant now = Instant.now();

        // 투표중 여부 판단 함수
        java.util.function.Predicate<VotePost> isActive = v ->
                v.getStatus() == VoteStatus.ACTIVE && v.getDeadline().isAfter(now);

        List<VoteResponseDTO> result = new ArrayList<>();
        Set<Long> includedIds = new HashSet<>();

        // Redis ZSet에서 조회
        Set<ZSetOperations.TypedTuple<String>> topVotesWithScores = redisTemplate.opsForZSet()
                .reverseRangeWithScores("vote:hot", 0, size * 2 - 1);

        if (topVotesWithScores != null && !topVotesWithScores.isEmpty()) {
            // voteId -> score 매핑
            Map<Long, Double> scoreMap = topVotesWithScores.stream()
                    .collect(Collectors.toMap(
                            tuple -> Long.valueOf(tuple.getValue()),
                            tuple -> tuple.getScore() != null ? tuple.getScore() : 0.0
                    ));

            List<Long> voteIds = new ArrayList<>(scoreMap.keySet());
            List<VotePost> hotVotes = votePostRepository.findAllById(voteIds);

            Map<Long, VotePost> voteMap = hotVotes.stream()
                    .collect(Collectors.toMap(VotePost::getId, v -> v));

            // 1순위: 투표중 우선 → 2순위: 점수 높은 순 → 3순위: 최신순
            voteIds.stream()
                    .map(voteMap::get)
                    .filter(Objects::nonNull)
                    .sorted(Comparator
                            .comparing((VotePost v) -> isActive.test(v) ? 0 : 1)
                            .thenComparing((VotePost v) -> scoreMap.getOrDefault(v.getId(), 0.0), Comparator.reverseOrder())
                            .thenComparing(VotePost::getCreatedAt, Comparator.reverseOrder()))
                    .limit(size)
                    .forEach(v -> {
                        result.add(VoteResponseDTO.from(v, redisTemplate));
                        includedIds.add(v.getId());
                    });
        }

        // Redis 결과가 부족한 경우 DB에서 최신순으로 보충
        if (result.size() < size) {
            int remaining = size - result.size();
            Page<VotePost> dbFallback = votePostRepository.findAllOrderByCreatedAtDesc(
                    PageRequest.of(0, size + includedIds.size()));
            dbFallback.getContent().stream()
                    .filter(v -> !includedIds.contains(v.getId()))
                    .limit(remaining)
                    .forEach(v -> result.add(VoteResponseDTO.from(v, redisTemplate)));
        }

        return result;
    }

    @Transactional
    public VoteResponseDTO updateVote(Long voteId, Long authorId, VoteUpdateRequestDTO request) {
        VotePost votePost = votePostRepository.findByIdAndAuthorId(voteId, authorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));

        if (votePost.getStatus() == VoteStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.VOTE_ALREADY_COMPLETED);
        }

        // deadline 변경 여부 확인
        boolean deadlineChanged = request.getDeadline() != null
                && !request.getDeadline().equals(votePost.getDeadline());

        votePost.updateVote(request.getTitle(), request.getDescription(), request.getDeadline());

        // deadline 변경 시 Redis 키 재등록
        if (deadlineChanged) {
            removeVoteExpirationKeys(voteId);
            registerVoteExpirationKeys(votePost);
        }

        return VoteResponseDTO.from(votePost, redisTemplate);
    }

    @Transactional
    public void deleteVote(Long voteId, Long authorId) {
        VotePost votePost = votePostRepository.findByIdAndAuthorId(voteId, authorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));

        // Redis TTL 키 삭제
        removeVoteExpirationKeys(voteId);

        votePostRepository.delete(votePost);
    }

    /**
     * 투표 종료 처리 및 Redis 발행
     */
    @Transactional
    public void completeVoteAndNotify(Long voteId) {
        VotePost votePost = votePostRepository.findById(voteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));

        votePost.complete();
        votePostRepository.save(votePost);

        eventPublisher.publishEvent(new VoteCompletedEvent(
                votePost.getId(),
                votePost.getTitle(),
                votePost.getAuthor().getId()
        ));
    }

    /**
     * 투표 종료 10분 전 알림 발행
     */
    @Transactional
    public void notifyVoteEndingSoon(Long voteId, String title) {
        try {
            VotePost votePost = votePostRepository.findById(voteId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));

            Member author = memberRepository.findById(votePost.getAuthor().getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

            log.info("투표 종료 10분 전 알림 처리 시작: voteId={}, authorId={}, voteNotificationEnabled={}",
                    voteId, author.getId(), author.getVoteNotificationEnabled());

            if (!author.getVoteNotificationEnabled()) {
                log.info("투표 종료 10분 전 알림 건너뜀 (알림 설정 OFF): voteId={}, authorId={}",
                        voteId, author.getId());
                votePost.markNotified10MinBefore();
                return;
            }

            Notification notification = notificationService.createNotification(
                    author.getId(),
                    voteId,
                    title,
                    VoteNotificationType.ENDING_SOON
            );

            if (notification == null) {
                log.info("중복 알림으로 인해 발행 건너뜀: voteId={}", voteId);
                votePost.markNotified10MinBefore();
                votePostRepository.save(votePost);
                return;
            }

            // Pub/Sub timestamp는 LocalDateTime으로 유지 (프론트 표시용)
            VoteNotificationMessageDTO message = VoteNotificationMessageDTO.builder()
                    .voteId(voteId)
                    .title(notification.getTitle())
                    .type(VoteNotificationType.ENDING_SOON)
                    .timestamp(LocalDateTime.now())
                    .memberId(author.getId())
                    .build();

            redisPubSubTemplate.convertAndSend("vote:before10min", message);

            votePost.markNotified10MinBefore();
            votePostRepository.save(votePost);

            log.info("투표 종료 10분 전 알림 발행: voteId={}, authorId={}", voteId, author.getId());
        } catch (Exception e) {
            log.error("투표 종료 10분 전 알림 발행 실패: voteId={}", voteId, e);
        }
    }

    /**
     * 만료된 투표들을 일괄 종료 처리 (스케줄러용)
     */
    @Transactional
    public void completeExpiredVotes() {
        Instant now = Instant.now();
        List<VotePost> expiredVotes = votePostRepository.findExpiredActiveVotes(now);
        expiredVotes.forEach(VotePost::complete);
    }

    /**
     * 투표 만료 Redis TTL 키 등록 (Keyspace Notifications 기반)
     *
     * - vote:expire:{voteId}: 투표 종료 트리거 (TTL = deadline - now)
     * - vote:soon:{voteId}: 10분 전 알림 트리거 (TTL = deadline - 10분 - now, 0 이상일 때만)
     */
    public void registerVoteExpirationKeys(VotePost votePost) {
        try {
            Instant now = Instant.now();
            Instant deadline = votePost.getDeadline();
            Long voteId = votePost.getId();

            // 투표 종료 키 TTL 계산
            long expireTtlSeconds = Duration.between(now, deadline).getSeconds();
            if (expireTtlSeconds > 0) {
                String expireKey = VOTE_EXPIRE_KEY_PREFIX + voteId;
                redisTemplate.opsForValue().set(expireKey, "1", expireTtlSeconds, TimeUnit.SECONDS);
                log.debug("[VotePostService] 투표 종료 키 등록: key={}, ttl={}초", expireKey, expireTtlSeconds);
            } else {
                log.warn("[VotePostService] 투표 종료 키 등록 생략 (이미 만료됨): voteId={}", voteId);
            }

            // 10분 전 알림 키 TTL 계산
            Instant soonNotificationTime = deadline.minus(SOON_NOTIFICATION_MINUTES, ChronoUnit.MINUTES);
            long soonTtlSeconds = Duration.between(now, soonNotificationTime).getSeconds();
            if (soonTtlSeconds > 0) {
                String soonKey = VOTE_SOON_KEY_PREFIX + voteId;
                redisTemplate.opsForValue().set(soonKey, "1", soonTtlSeconds, TimeUnit.SECONDS);
                log.debug("[VotePostService] 10분 전 알림 키 등록: key={}, ttl={}초", soonKey, soonTtlSeconds);
            } else {
                log.debug("[VotePostService] 10분 전 알림 키 등록 생략 (이미 지남): voteId={}", voteId);
            }
        } catch (Exception e) {
            log.error("[VotePostService] Redis TTL 키 등록 실패: voteId={}", votePost.getId(), e);
        }
    }

    /**
     * 가장 많이 득표한 옵션명 조회
     */
    public String getWinningOptionText(Long voteId) {
        VotePost votePost = votePostRepository.findById(voteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));

        return votePost.getVoteOptions().stream()
                .max((opt1, opt2) -> {
                    int count1 = getOptionVoteCount(voteId, opt1.getId());
                    int count2 = getOptionVoteCount(voteId, opt2.getId());
                    return Integer.compare(count1, count2);
                })
                .map(opt -> opt.getOptionText())
                .orElse("없음");
    }

    /**
     * 특정 옵션의 투표 수 조회 (Redis 우선, 없으면 DB)
     */
    private int getOptionVoteCount(Long voteId, Long optionId) {
        String optionKey = "vote:" + voteId + ":option:" + optionId + ":count";
        String countStr = redisTemplate.opsForValue().get(optionKey);
        if (countStr != null) {
            return Integer.parseInt(countStr);
        }
        // Redis에 없으면 DB에서 조회 (VoteOption의 participations)
        return 0;
    }

    /**
     * 투표 만료 Redis TTL 키 삭제
     */
    public void removeVoteExpirationKeys(Long voteId) {
        try {
            String expireKey = VOTE_EXPIRE_KEY_PREFIX + voteId;
            String soonKey = VOTE_SOON_KEY_PREFIX + voteId;

            Boolean expireDeleted = redisTemplate.delete(expireKey);
            Boolean soonDeleted = redisTemplate.delete(soonKey);

            log.debug("[VotePostService] Redis TTL 키 삭제: voteId={}, expireKey삭제={}, soonKey삭제={}",
                    voteId, expireDeleted, soonDeleted);
        } catch (Exception e) {
            log.error("[VotePostService] Redis TTL 키 삭제 실패: voteId={}", voteId, e);
        }
    }
}
