package com.mechuragi.mechuragi_server.domain.vote.controller;

import com.mechuragi.mechuragi_server.domain.vote.dto.*;
import com.mechuragi.mechuragi_server.domain.vote.service.PopularMenuService;
import com.mechuragi.mechuragi_server.domain.vote.service.VoteCommentService;
import com.mechuragi.mechuragi_server.domain.vote.service.VoteLikeService;
import com.mechuragi.mechuragi_server.domain.vote.service.VoteParticipationService;
import com.mechuragi.mechuragi_server.domain.vote.service.VotePostService;
import com.mechuragi.mechuragi_server.global.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.mechuragi.mechuragi_server.auth.dto.CustomUserDetails;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
@Tag(name = "투표", description = "투표 관리 API")
public class VoteController {

    private final VotePostService votePostService;
    private final VoteParticipationService voteParticipationService;
    private final VoteCommentService voteCommentService;
    private final VoteLikeService voteLikeService;
    private final S3Service s3Service;
    private final PopularMenuService popularMenuService;

    @Operation(summary = "투표 생성")
    @PostMapping
    public ResponseEntity<VoteResponseDTO> createVote(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody VoteCreateRequestDTO request) {
        VoteResponseDTO response = votePostService.createVote(userDetails.getMemberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "투표 상세 조회")
    @GetMapping("/{voteId}")
    public ResponseEntity<VoteResponseDTO> getVote(@PathVariable Long voteId) {
        VoteResponseDTO response = votePostService.getVote(voteId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "활성화된 투표 목록 조회")
    @GetMapping("/active")
    public ResponseEntity<Page<VoteResponseDTO>> getActiveVotes(
            @PageableDefault(size = 10) Pageable pageable) {
        Page<VoteResponseDTO> response = votePostService.getActiveVotes(pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "top10 인기 투표 게시글 조회")
    @GetMapping("/hot")
    public ResponseEntity<List<VoteResponseDTO>> getHotVotes(
            @RequestParam(defaultValue = "10") int size) {
        List<VoteResponseDTO> response = votePostService.getHotVotes(size);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "오늘의 인기 메뉴 조회")
    @GetMapping("/popular-menus")
    public ResponseEntity<List<PopularMenuResponseDTO>> getPopularMenus() {
        List<PopularMenuResponseDTO> response = popularMenuService.getPopularMenus();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "종료된 투표 목록 조회")
    @GetMapping("/completed")
    public ResponseEntity<Page<VoteResponseDTO>> getCompletedVotes(
            @PageableDefault(size = 10) Pageable pageable) {
        Page<VoteResponseDTO> response = votePostService.getCompletedVotes(pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "사용자 투표 목록 조회")
    @GetMapping("/my")
    public ResponseEntity<Page<VoteResponseDTO>> getMyVotes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<VoteResponseDTO> response = votePostService.getUserVotes(userDetails.getMemberId(), pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "투표 게시글 수정")
    @PutMapping("/{voteId}")
    public ResponseEntity<VoteResponseDTO> updateVote(
            @PathVariable Long voteId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody VoteUpdateRequestDTO request) {
        VoteResponseDTO response = votePostService.updateVote(voteId, userDetails.getMemberId(), request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "투표 게시글 삭제")
    @DeleteMapping("/{voteId}")
    public ResponseEntity<Void> deleteVote(
            @PathVariable Long voteId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        votePostService.deleteVote(voteId, userDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "투표 이미지 업로드")
    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file) {
        String imageUrl = s3Service.uploadImage(file, "vote-images");
        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }

    @Operation(summary = "투표 참여")
    @PostMapping("/participate")
    public ResponseEntity<VoteParticipationResponseDTO> participateVote(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody VoteParticipationRequestDTO request) {
        VoteParticipationResponseDTO response = voteParticipationService.participateVote(
                userDetails.getMemberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "투표 취소")
    @DeleteMapping("/{voteId}/participate")
    public ResponseEntity<Void> cancelParticipation(
            @PathVariable Long voteId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        voteParticipationService.cancelParticipation(userDetails.getMemberId(), voteId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "사용자 투표 참여 상태 조회")
    @GetMapping("/{voteId}/my-participation")
    public ResponseEntity<VoteParticipationResponseDTO> getMyParticipation(
            @PathVariable Long voteId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        VoteParticipationResponseDTO response = voteParticipationService.getMyParticipation(
                userDetails.getMemberId(), voteId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "투표 참여자 존재 여부 조회")
    @GetMapping("/{voteId}/participated")
    public ResponseEntity<Map<String, Boolean>> hasParticipated(
            @PathVariable Long voteId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean participated = voteParticipationService.hasParticipated(
                userDetails.getMemberId(), voteId);
        return ResponseEntity.ok(Map.of("participated", participated));
    }

    // 투표 댓글

    @Operation(summary = "댓글 작성")
    @PostMapping("/comments")
    public ResponseEntity<VoteCommentResponseDTO> createComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody VoteCommentCreateRequestDTO request) {
        VoteCommentResponseDTO response = voteCommentService.createComment(
                userDetails.getMemberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "댓글 조회")
    @GetMapping("/{voteId}/comments")
    public ResponseEntity<Page<VoteCommentResponseDTO>> getComments(
            @PathVariable Long voteId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<VoteCommentResponseDTO> response = voteCommentService.getComments(voteId, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "댓글 수정")
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<VoteCommentResponseDTO> updateComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody VoteCommentUpdateRequestDTO request) {
        VoteCommentResponseDTO response = voteCommentService.updateComment(
                commentId, userDetails.getMemberId(), request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "댓글 삭제")
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        voteCommentService.deleteComment(commentId, userDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "댓글수 조회")
    @GetMapping("/{voteId}/comments/count")
    public ResponseEntity<Map<String, Integer>> getCommentCount(
            @PathVariable Long voteId) {
        int count = voteCommentService.getCommentCount(voteId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    // 투표 게시물 좋아요

    @Operation(summary = "좋아요 토글 요청")
    @PostMapping("/{voteId}/like")
    public ResponseEntity<Map<String, Boolean>> toggleLike(
            @PathVariable Long voteId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean isLiked = voteLikeService.toggleLike(userDetails.getMemberId(), voteId);
        return ResponseEntity.ok(Map.of("liked", isLiked));
    }

    @Operation(summary = "좋아요 여부 조회")
    @GetMapping("/{voteId}/liked")
    public ResponseEntity<Map<String, Boolean>> isLiked(
            @PathVariable Long voteId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean liked = voteLikeService.isLiked(userDetails.getMemberId(), voteId);
        return ResponseEntity.ok(Map.of("liked", liked));
    }

    @Operation(summary = "좋아요 수 조회")
    @GetMapping("/{voteId}/likes/count")
    public ResponseEntity<Map<String, Integer>> getLikeCount(
            @PathVariable Long voteId) {
        int count = voteLikeService.getLikeCount(voteId);
        return ResponseEntity.ok(Map.of("count", count));
    }
}