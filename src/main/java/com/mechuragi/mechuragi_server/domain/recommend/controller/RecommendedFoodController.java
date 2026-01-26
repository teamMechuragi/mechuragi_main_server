package com.mechuragi.mechuragi_server.domain.recommend.controller;

import com.mechuragi.mechuragi_server.auth.dto.CustomUserDetails;
import com.mechuragi.mechuragi_server.domain.recommend.dto.common.response.BookmarkedSessionResponse;
import com.mechuragi.mechuragi_server.domain.recommend.dto.common.response.RecommendedFoodResponse;
import com.mechuragi.mechuragi_server.domain.recommend.dto.external.request.SaveRecommendationsRequest;
import com.mechuragi.mechuragi_server.domain.recommend.service.BookmarkService;
import com.mechuragi.mechuragi_server.domain.recommend.service.RecommendedFoodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/recommended-foods")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI 추천 음식 관리", description = "AI가 추천한 음식 조회 및 북마크 관리 API")
public class RecommendedFoodController {

    private final RecommendedFoodService recommendedFoodService;
    private final BookmarkService bookmarkService;

    @PostMapping("/save")
    @Operation(summary = "추천 결과 저장", description = "AI 추천 결과를 저장합니다")
    public ResponseEntity<Void> saveRecommendations(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SaveRecommendationsRequest request) {

        Long memberId = userDetails.getMemberId();
        log.info("추천 결과 저장 요청 - 회원: {}, 개수: {}",
                memberId, request.getRecommendations().size());

        recommendedFoodService.saveRecommendations(memberId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @Operation(summary = "모든 추천 음식 조회", description = "사용자의 모든 AI 추천 음식 목록을 조회합니다")
    public ResponseEntity<List<RecommendedFoodResponse>> getAllRecommendations(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long memberId = userDetails.getMemberId();
        log.info("전체 추천 음식 조회 - 회원: {}", memberId);

        List<RecommendedFoodResponse> recommendations = recommendedFoodService.getAllRecommendations(memberId);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/bookmarks")
    @Operation(summary = "북마크한 추천 세션 조회", description = "사용자가 북마크한 AI 추천 세션 목록을 조회합니다")
    public ResponseEntity<List<BookmarkedSessionResponse>> getBookmarkedSessions(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long memberId = userDetails.getMemberId();
        log.info("북마크 추천 세션 조회 - 회원: {}", memberId);

        List<BookmarkedSessionResponse> bookmarkedSessions = bookmarkService.getBookmarkedSessions(memberId);
        return ResponseEntity.ok(bookmarkedSessions);
    }

    @PostMapping("/bookmark")
    @Operation(summary = "최근 추천 북마크", description = "가장 최근 추천 세션을 북마크합니다")
    public ResponseEntity<Void> bookmarkLatestSession(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long memberId = userDetails.getMemberId();
        log.info("최근 추천 북마크 - 회원: {}", memberId);

        bookmarkService.bookmarkLatestSession(memberId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sessions/{sessionId}/bookmark")
    @Operation(summary = "추천 세션 북마크 토글", description = "특정 추천 세션의 북마크 상태를 토글합니다")
    public ResponseEntity<Void> toggleSessionBookmark(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sessionId) {

        Long memberId = userDetails.getMemberId();
        log.info("추천 세션 북마크 토글 - 회원: {}, 세션 ID: {}", memberId, sessionId);

        bookmarkService.toggleBookmarkBySessionId(memberId, sessionId);
        return ResponseEntity.ok().build();
    }
}
