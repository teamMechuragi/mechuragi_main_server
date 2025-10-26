package com.mechuragi.mechuragi_server.domain.vote.controller;

import com.mechuragi.mechuragi_server.domain.vote.dto.VoteCreateRequestDTO;
import com.mechuragi.mechuragi_server.domain.vote.dto.VoteParticipationRequestDTO;
import com.mechuragi.mechuragi_server.domain.vote.dto.VoteParticipationResponseDTO;
import com.mechuragi.mechuragi_server.domain.vote.dto.VoteResponseDTO;
import com.mechuragi.mechuragi_server.domain.vote.dto.VoteUpdateRequestDTO;
import com.mechuragi.mechuragi_server.domain.vote.service.VoteParticipationService;
import com.mechuragi.mechuragi_server.domain.vote.service.VotePostService;
import com.mechuragi.mechuragi_server.global.service.S3Service;
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

import java.util.Map;

@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VotePostService votePostService;
    private final VoteParticipationService voteParticipationService;
    private final S3Service s3Service;

    @PostMapping
    public ResponseEntity<VoteResponseDTO> createVote(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody VoteCreateRequestDTO request) {
        VoteResponseDTO response = votePostService.createVote(userDetails.getMemberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{voteId}")
    public ResponseEntity<VoteResponseDTO> getVote(@PathVariable Long voteId) {
        VoteResponseDTO response = votePostService.getVote(voteId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<Page<VoteResponseDTO>> getActiveVotes(
            @PageableDefault(size = 10) Pageable pageable) {
        Page<VoteResponseDTO> response = votePostService.getActiveVotes(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/completed")
    public ResponseEntity<Page<VoteResponseDTO>> getCompletedVotes(
            @PageableDefault(size = 10) Pageable pageable) {
        Page<VoteResponseDTO> response = votePostService.getCompletedVotes(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<Page<VoteResponseDTO>> getMyVotes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<VoteResponseDTO> response = votePostService.getUserVotes(userDetails.getMemberId(), pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{voteId}")
    public ResponseEntity<VoteResponseDTO> updateVote(
            @PathVariable Long voteId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody VoteUpdateRequestDTO request) {
        VoteResponseDTO response = votePostService.updateVote(voteId, userDetails.getMemberId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{voteId}")
    public ResponseEntity<Void> deleteVote(
            @PathVariable Long voteId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        votePostService.deleteVote(voteId, userDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file) {
        String imageUrl = s3Service.uploadImage(file, "vote-images");
        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }

    @PostMapping("/participate")
    public ResponseEntity<VoteParticipationResponseDTO> participateVote(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody VoteParticipationRequestDTO request) {
        VoteParticipationResponseDTO response = voteParticipationService.participateVote(
                userDetails.getMemberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{voteId}/participate")
    public ResponseEntity<Void> cancelParticipation(
            @PathVariable Long voteId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        voteParticipationService.cancelParticipation(userDetails.getMemberId(), voteId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{voteId}/my-participation")
    public ResponseEntity<VoteParticipationResponseDTO> getMyParticipation(
            @PathVariable Long voteId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        VoteParticipationResponseDTO response = voteParticipationService.getMyParticipation(
                userDetails.getMemberId(), voteId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{voteId}/participated")
    public ResponseEntity<Map<String, Boolean>> hasParticipated(
            @PathVariable Long voteId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean participated = voteParticipationService.hasParticipated(
                userDetails.getMemberId(), voteId);
        return ResponseEntity.ok(Map.of("participated", participated));
    }
}