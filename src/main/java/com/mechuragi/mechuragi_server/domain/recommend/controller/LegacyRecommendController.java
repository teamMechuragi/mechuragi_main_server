package com.mechuragi.mechuragi_server.domain.recommend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/legacy")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "레거시 AI 추천", description = "성능 비교용 레거시 경로 AI 음식 추천 API (Main Server 경유)")
public class LegacyRecommendController {

    private final RestClient aiServerRestClient;

    @PostMapping("/recommend/food")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "레거시 경로 음식 추천",
        description = "Client → Nginx → Main Server(Spring Security) → AI Server → Bedrock 경로로 음식을 추천합니다. " +
                      "Direct Path(/recommend/food)와의 성능 비교를 위한 레거시 경로입니다."
    )
    public ResponseEntity<Object> legacyRecommend(
            @RequestHeader("Authorization") String authorization,
            @RequestBody Object requestBody,
            HttpServletRequest request) {

        Long requestStart = (Long) request.getAttribute("requestStartTime");
        if (requestStart == null) requestStart = System.currentTimeMillis();

        Long authTimeMs = (Long) request.getAttribute("authTimeMs");
        if (authTimeMs == null) authTimeMs = 0L;

        log.info("[성능] Main→AI 추천 요청 전송 시작");
        long aiStart = System.currentTimeMillis();

        Object aiResponse = aiServerRestClient.post()
                .uri("/recommend/food")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Object.class);

        log.info("[성능] AI→Main 추천 응답 수신 완료: {}ms", System.currentTimeMillis() - aiStart);

        long totalMs = System.currentTimeMillis() - requestStart;
        log.info("[성능] Spring Security 인증 처리: {}ms", authTimeMs);
        log.info("[성능] 레거시 경로 전체 소요: {}ms", totalMs);

        log.info("[성능] Main→Client 응답 전송 시작");
        return ResponseEntity.ok(aiResponse);
    }
}
