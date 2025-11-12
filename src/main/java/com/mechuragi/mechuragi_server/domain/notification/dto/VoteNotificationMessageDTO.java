package com.mechuragi.mechuragi_server.domain.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteNotificationMessageDTO {
    private Long voteId;
    private String title;
    private VoteNotificationType type;
    private LocalDateTime timestamp;
    private Long memberId; // 알림을 받을 사용자 ID
}
