package com.mechuragi.mechuragi_server.domain.notification.service;

import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.member.repository.MemberRepository;
import com.mechuragi.mechuragi_server.domain.notification.dto.NotificationResponseDTO;
import com.mechuragi.mechuragi_server.domain.notification.dto.UnreadCountResponseDTO;
import com.mechuragi.mechuragi_server.domain.notification.dto.VoteNotificationType;
import com.mechuragi.mechuragi_server.domain.notification.entity.Notification;
import com.mechuragi.mechuragi_server.domain.notification.repository.NotificationRepository;
import com.mechuragi.mechuragi_server.global.exception.BusinessException;
import com.mechuragi.mechuragi_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;

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
            case COMPLETED -> "투표가 마감되었습니다";
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
            throw new BusinessException(ErrorCode.FORBIDDEN);
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
}
