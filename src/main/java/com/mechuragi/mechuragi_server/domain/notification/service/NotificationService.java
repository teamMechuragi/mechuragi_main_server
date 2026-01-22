package com.mechuragi.mechuragi_server.domain.notification.service;

import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.member.repository.MemberRepository;
import com.mechuragi.mechuragi_server.domain.notification.dto.NotificationResponseDTO;
import com.mechuragi.mechuragi_server.domain.notification.dto.UnreadCountResponseDTO;
import com.mechuragi.mechuragi_server.domain.notification.dto.VoteNotificationType;
import com.mechuragi.mechuragi_server.domain.notification.entity.Notification;
import com.mechuragi.mechuragi_server.domain.notification.repository.NotificationRepository;
import com.mechuragi.mechuragi_server.domain.vote.entity.VoteOption;
import com.mechuragi.mechuragi_server.domain.vote.entity.VotePost;
import com.mechuragi.mechuragi_server.domain.vote.repository.VotePostRepository;
import com.mechuragi.mechuragi_server.global.exception.BusinessException;
import com.mechuragi.mechuragi_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final VotePostRepository votePostRepository;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 알림 저장
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public Notification createNotification(Long memberId, Long voteId, String title, VoteNotificationType type) {
        log.debug("알림 저장 시도: memberId={}, voteId={}, type={}", memberId, voteId, type);

        // 중복 확인
        if (notificationRepository.existsByMemberIdAndVoteIdAndType(memberId, voteId, type)) {
            log.info("중복 알림 저장 방지: memberId={}, voteId={}, type={}", memberId, voteId, type);
            return null;
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 알림 타입에 따라 메시지 생성
        String notificationMessage = switch (type) {
            case ENDING_SOON -> "투표 마감 10분전입니다";
            case COMPLETED -> "투표 종료! 추천 메뉴: " + getWinningOptionText(voteId);
        };

        Notification notification = Notification.builder()
                .member(member)
                .voteId(voteId)
                .title(notificationMessage)
                .type(type)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.debug("알림 저장 완료: notificationId={}, memberId={}, voteId={}, type={}",
                saved.getId(), memberId, voteId, type);
        return saved;
    }

    /**
     * 알림 목록 조회 (페이징)
     */
    public Page<NotificationResponseDTO> getNotifications(Long memberId, Pageable pageable) {
        return notificationRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable)
                .map(NotificationResponseDTO::from);
    }

    /**
     * 특정 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long memberId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // 알림 소유자 확인
        if (!notification.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_FORBIDDEN);
        }

        notification.markAsRead();
    }

    /**
     * 안 읽은 알림 개수 조회
     */
    public UnreadCountResponseDTO getUnreadCount(Long memberId) {
        long count = notificationRepository.countByMemberIdAndIsReadFalse(memberId);
        return UnreadCountResponseDTO.builder()
                .unreadCount(count)
                .build();
    }

    /**
     * 가장 많이 득표한 옵션명 조회
     */
    private String getWinningOptionText(Long voteId) {
        VotePost votePost = votePostRepository.findById(voteId).orElse(null);
        if (votePost == null || votePost.getVoteOptions().isEmpty()) {
            return "없음";
        }

        return votePost.getVoteOptions().stream()
                .max(Comparator.comparingInt(opt -> getOptionVoteCount(voteId, opt.getId())))
                .map(VoteOption::getOptionText)
                .orElse("없음");
    }

    /**
     * 특정 옵션의 투표 수 조회 (Redis 우선, 없으면 DB)
     */
    private int getOptionVoteCount(Long voteId, Long optionId) {
        String optionKey = "vote:" + voteId + ":option:" + optionId + ":count";
        String countStr = redisTemplate.opsForValue().get(optionKey);
        if (countStr != null) {
            try {
                return Integer.parseInt(countStr);
            } catch (NumberFormatException e) {
                log.warn("Redis 옵션 카운트 파싱 실패: key={}, value={}", optionKey, countStr);
            }
        }
        return 0;
    }
}
