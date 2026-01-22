package com.mechuragi.mechuragi_server.global.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 연결 관리 저장소
 * 사용자별 SseEmitter를 관리합니다.
 */
@Slf4j
@Repository
public class SseEmitterRepository {

    // memberId -> SseEmitter
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter save(Long memberId, SseEmitter emitter) {
        emitters.put(memberId, emitter);
        log.debug("[SSE] Emitter 저장: memberId={}", memberId);
        return emitter;
    }

    public void deleteById(Long memberId) {
        emitters.remove(memberId);
        log.debug("[SSE] Emitter 삭제: memberId={}", memberId);
    }

    public SseEmitter findById(Long memberId) {
        return emitters.get(memberId);
    }

    public int getConnectionCount() {
        return emitters.size();
    }

    /**
     * 30초마다 모든 연결에 heartbeat 전송
     * 끊긴 연결은 자동으로 정리됨
     */
    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        if (emitters.isEmpty()) {
            return;
        }

        List<Long> deadEmitters = new ArrayList<>();

        emitters.forEach((memberId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("ping"));
            } catch (IOException e) {
                log.debug("[SSE] Heartbeat 실패, 연결 정리: memberId={}", memberId);
                deadEmitters.add(memberId);
            }
        });

        // 끊긴 연결 정리
        deadEmitters.forEach(this::deleteById);

        if (!deadEmitters.isEmpty()) {
            log.info("[SSE] Heartbeat로 {} 개의 끊긴 연결 정리, 현재 연결 수: {}",
                    deadEmitters.size(), emitters.size());
        }
    }
}
