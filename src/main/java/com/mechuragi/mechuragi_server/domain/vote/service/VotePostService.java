package com.mechuragi.mechuragi_server.domain.vote.service;

import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.member.repository.MemberRepository;
import com.mechuragi.mechuragi_server.domain.notification.dto.VoteNotificationMessageDTO;
import com.mechuragi.mechuragi_server.domain.notification.dto.VoteNotificationType;
import com.mechuragi.mechuragi_server.domain.notification.event.VoteCompletedEvent;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VotePostService {

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

        return VoteResponseDTO.from(savedVotePost, redisTemplate);
    }

    public VoteResponseDTO getVote(Long voteId) {
        VotePost votePost = votePostRepository.findById(voteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));

        return VoteResponseDTO.from(votePost, redisTemplate);
    }

    /**
     * 진행 중 투표 조회
     */
    public Page<VoteResponseDTO> getActiveVotes(Pageable pageable) {
        Instant now = Instant.now();
        Page<VotePost> votePosts = votePostRepository.findActiveVotes(now, pageable);
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
     * 핫한 투표 조회 (Redis 기반)
     */
    @Cacheable(value = "hotVotes", key = "'list:' + #size")
    public List<VoteResponseDTO> getHotVotes(int size) {
        Set<String> topVoteIds = redisTemplate.opsForZSet()
                .reverseRange("vote:hot", 0, size - 1);

        if (topVoteIds == null || topVoteIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> voteIds = topVoteIds.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());

        List<VotePost> hotVotes = votePostRepository.findAllById(voteIds);

        Map<Long, VotePost> voteMap = hotVotes.stream()
                .collect(Collectors.toMap(VotePost::getId, v -> v));

        return voteIds.stream()
                .map(voteMap::get)
                .filter(Objects::nonNull)
                .map(v -> VoteResponseDTO.from(v, redisTemplate))
                .collect(Collectors.toList());
    }

    @Transactional
    public VoteResponseDTO updateVote(Long voteId, Long authorId, VoteUpdateRequestDTO request) {
        VotePost votePost = votePostRepository.findByIdAndAuthorId(voteId, authorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));

        if (votePost.getStatus() == VoteStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.VOTE_ALREADY_COMPLETED);
        }

        votePost.updateVote(request.getTitle(), request.getDescription(), request.getDeadline());
        return VoteResponseDTO.from(votePost, redisTemplate);
    }

    @Transactional
    public void deleteVote(Long voteId, Long authorId) {
        VotePost votePost = votePostRepository.findByIdAndAuthorId(voteId, authorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));

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

            notificationService.createNotification(
                    author.getId(),
                    voteId,
                    title,
                    VoteNotificationType.ENDING_SOON
            );

            // Pub/Sub timestamp는 LocalDateTime으로 유지 (프론트 표시용)
            VoteNotificationMessageDTO message = VoteNotificationMessageDTO.builder()
                    .voteId(voteId)
                    .title(title)
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
}
