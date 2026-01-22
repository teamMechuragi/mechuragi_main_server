package com.mechuragi.mechuragi_server.global.sse;

import com.mechuragi.mechuragi_server.auth.dto.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 연결 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
@Tag(name = "SSE", description = "Server-Sent Events 연결 API")
public class SseController {

    private static final Long SSE_TIMEOUT = 30 * 60 * 1000L; // 30분

    private final SseEmitterRepository sseEmitterRepository;

    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE 연결", description = "실시간 알림을 위한 SSE 연결을 생성합니다")
    public ResponseEntity<SseEmitter> connect(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long memberId = userDetails.getMemberId();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 기존 연결이 있으면 제거
        SseEmitter existingEmitter = sseEmitterRepository.findById(memberId);
        if (existingEmitter != null) {
            existingEmitter.complete();
            sseEmitterRepository.deleteById(memberId);
        }

        sseEmitterRepository.save(memberId, emitter);

        // 연결 종료 시 정리
        emitter.onCompletion(() -> {
            log.debug("[SSE] 연결 완료: memberId={}", memberId);
            sseEmitterRepository.deleteById(memberId);
        });

        emitter.onTimeout(() -> {
            log.debug("[SSE] 연결 타임아웃: memberId={}", memberId);
            emitter.complete();
            sseEmitterRepository.deleteById(memberId);
        });

        emitter.onError(e -> {
            log.warn("[SSE] 연결 에러: memberId={}, error={}", memberId, e.getMessage());
            sseEmitterRepository.deleteById(memberId);
        });

        // 연결 성공 이벤트 전송 (연결 확인용)
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected"));
            log.info("[SSE] 연결 성공: memberId={}", memberId);
        } catch (Exception e) {
            log.error("[SSE] 연결 이벤트 전송 실패: memberId={}", memberId, e);
            sseEmitterRepository.deleteById(memberId);
        }

        return ResponseEntity.ok(emitter);
    }
}
