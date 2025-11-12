package com.mechuragi.mechuragi_server.domain.notification.controller;

import com.mechuragi.mechuragi_server.auth.dto.CustomUserDetails;
import com.mechuragi.mechuragi_server.domain.notification.dto.NotificationResponseDTO;
import com.mechuragi.mechuragi_server.domain.notification.dto.UnreadCountResponseDTO;
import com.mechuragi.mechuragi_server.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 알림 목록 조회 (페이징)
     */
    @GetMapping
    public ResponseEntity<Page<NotificationResponseDTO>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long memberId = userDetails.getMemberId();
        Page<NotificationResponseDTO> notifications = notificationService.getNotifications(memberId, pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 특정 알림 읽음 처리
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationId) {
        Long memberId = userDetails.getMemberId();
        notificationService.markAsRead(memberId, notificationId);
        return ResponseEntity.ok().build();
    }

    /**
     * 안 읽은 알림 개수 조회
     */
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponseDTO> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        UnreadCountResponseDTO response = notificationService.getUnreadCount(memberId);
        return ResponseEntity.ok(response);
    }
}
