package com.mechuragi.mechuragi_server.domain.diary.controller;

import com.mechuragi.mechuragi_server.auth.dto.CustomUserDetails;
import com.mechuragi.mechuragi_server.domain.diary.dto.*;
import com.mechuragi.mechuragi_server.domain.diary.service.FoodDiaryService;
import com.mechuragi.mechuragi_server.global.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
@Tag(name = "먹방 일기", description = "먹방 일기 관리 API")
public class FoodDiaryController {

    private final FoodDiaryService foodDiaryService;
    private final S3Service s3Service;

    @PostMapping
    @Operation(summary = "먹방 일기 등록", description = "새로운 먹방 일기를 작성합니다.")
    public ResponseEntity<DiaryResponseDTO> createDiary(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateDiaryRequestDTO diaryRequest) {

        DiaryResponseDTO response = foodDiaryService.createDiary(userDetails.getMemberId(), diaryRequest);
        return ResponseEntity
                .created(URI.create("/api/diaries/" + response.getId()))
                .body(response);
    }

    @GetMapping("/calendar")
    @Operation(summary = "먹방 일기 캘린더 조회", description = "특정 년월의 일기 목록을 캘린더 형식으로 조회합니다.")
    public ResponseEntity<DiaryCalendarResponseDTO> getMonthlyDiaries(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam int year,
            @RequestParam int month) {

        DiaryCalendarResponseDTO response = foodDiaryService.getMonthlyDiaries(userDetails.getMemberId(), year, month);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{diaryId}")
    @Operation(summary = "먹방 일기 상세 조회", description = "특정 먹방 일기의 상세 정보를 조회합니다.")
    public ResponseEntity<DiaryResponseDTO> getDiaryDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long diaryId) {

        DiaryResponseDTO response = foodDiaryService.getDiaryDetail(userDetails.getMemberId(), diaryId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{diaryId}")
    @Operation(summary = "먹방 일기 수정", description = "자신이 작성한 먹방 일기를 수정합니다.")
    public ResponseEntity<DiaryResponseDTO> updateDiary(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long diaryId,
            @Valid @RequestBody UpdateDiaryRequestDTO updateRequest) {

        DiaryResponseDTO response = foodDiaryService.updateDiary(userDetails.getMemberId(), diaryId, updateRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{diaryId}")
    @Operation(summary = "먹방 일기 삭제", description = "자신이 작성한 먹방 일기를 삭제합니다.")
    public ResponseEntity<Void> deleteDiary(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long diaryId) {

        foodDiaryService.deleteDiary(userDetails.getMemberId(), diaryId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "먹방 일기 이미지 업로드 (기존 방식 - deprecated)", description = "먹방 일기에 사용할 이미지를 S3에 업로드합니다.")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file) {
        String imageUrl = s3Service.uploadImage(file, "diary");
        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }

    @PostMapping("/presigned-url")
    @Operation(summary = "일기 이미지 업로드용 Pre-signed URL 발급", description = "클라이언트가 S3에 직접 업로드할 수 있는 Pre-signed URL을 발급합니다.")
    public ResponseEntity<Map<String, String>> getPresignedUploadUrl(
            @RequestParam("filename") String filename,
            @RequestParam("contentType") String contentType) {
        Map<String, String> result = s3Service.generatePresignedUploadUrl("diary", filename, contentType);
        return ResponseEntity.ok(result);
    }
}
