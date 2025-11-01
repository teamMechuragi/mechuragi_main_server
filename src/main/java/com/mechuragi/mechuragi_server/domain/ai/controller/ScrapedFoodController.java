package com.mechuragi.mechuragi_server.domain.ai.controller;

import com.mechuragi.mechuragi_server.auth.dto.CustomUserDetails;
import com.mechuragi.mechuragi_server.domain.ai.dto.internal.request.ScrapeFoodRequest;
import com.mechuragi.mechuragi_server.domain.ai.dto.common.response.ScrapedFoodResponse;
import com.mechuragi.mechuragi_server.domain.ai.service.ScrapFoodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scraped-foods")
@RequiredArgsConstructor
@Slf4j
public class ScrapedFoodController {

    private final ScrapFoodService scrapFoodService;

    /**
     * 음식 추천 스크랩 저장
     */
    @PostMapping
    public ResponseEntity<ScrapedFoodResponse> saveScrap(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ScrapeFoodRequest request) {

        Long memberId = userDetails.getMemberId();
        log.info("스크랩 저장 요청 - 사용자: {}, 음식명: {}", memberId, request.getName());

        ScrapedFoodResponse response = scrapFoodService.saveScrapRecommendation(memberId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 사용자의 스크랩 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<ScrapedFoodResponse>> getScrapedList(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long memberId = userDetails.getMemberId();
        log.info("스크랩 목록 조회 요청 - 사용자: {}", memberId);

        List<ScrapedFoodResponse> responses = scrapFoodService.getScrapedRecommendations(memberId);

        return ResponseEntity.ok(responses);
    }

    /**
     * 스크랩 상세 조회
     */
    @GetMapping("/{scrapId}")
    public ResponseEntity<ScrapedFoodResponse> getScrapDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long scrapId) {

        Long memberId = userDetails.getMemberId();
        log.info("스크랩 상세 조회 요청 - 사용자: {}, 스크랩 ID: {}", memberId, scrapId);

        ScrapedFoodResponse response = scrapFoodService.getScrapedRecommendationDetail(memberId, scrapId);

        return ResponseEntity.ok(response);
    }

    /**
     * 스크랩 삭제
     */
    @DeleteMapping("/{scrapId}")
    public ResponseEntity<Void> deleteScrap(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long scrapId) {

        Long memberId = userDetails.getMemberId();
        log.info("스크랩 삭제 요청 - 사용자: {}, 스크랩 ID: {}", memberId, scrapId);

        scrapFoodService.deleteScrapedRecommendation(memberId, scrapId);

        return ResponseEntity.noContent().build();
    }
}
