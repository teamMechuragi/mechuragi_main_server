package com.mechuragi.mechuragi_server.global.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
}
