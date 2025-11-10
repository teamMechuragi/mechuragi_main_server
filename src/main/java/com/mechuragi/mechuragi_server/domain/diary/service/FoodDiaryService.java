package com.mechuragi.mechuragi_server.domain.diary.service;

import com.mechuragi.mechuragi_server.domain.diary.dto.*;
import com.mechuragi.mechuragi_server.domain.diary.entity.FoodDiary;
import com.mechuragi.mechuragi_server.domain.diary.entity.FoodDiaryImage;
import com.mechuragi.mechuragi_server.domain.diary.repository.FoodDiaryRepository;
import com.mechuragi.mechuragi_server.domain.diary.validator.RatingValidator;
import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.member.repository.MemberRepository;
import com.mechuragi.mechuragi_server.global.exception.BusinessException;
import com.mechuragi.mechuragi_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FoodDiaryService {

    private final FoodDiaryRepository foodDiaryRepository;
    private final MemberRepository memberRepository;

    // 일기 등록
    @Transactional
    public DiaryResponseDTO createDiary(Long memberId, CreateDiaryRequestDTO request) {
        // 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 별점 검증
        if (!RatingValidator.isValid(request.getRating())) {
            throw new BusinessException(ErrorCode.INVALID_RATING);
        }

        // 미래 날짜 검증
        if (request.getDiaryDate().isAfter(LocalDate.now())) {
            throw new BusinessException(ErrorCode.FUTURE_DATE_NOT_ALLOWED);
        }

        // 이미지 개수 검증
        if (request.getImageUrls() != null && request.getImageUrls().size() > 4) {
            throw new BusinessException(ErrorCode.TOO_MANY_IMAGES);
        }

        // 같은 날짜에 일기가 이미 존재하는지 확인
        foodDiaryRepository.findByMemberIdAndDiaryDate(memberId, request.getDiaryDate())
                .ifPresent(diary -> {
                    throw new BusinessException(ErrorCode.DUPLICATE_DIARY_DATE);
                });

        // 일기 생성
        FoodDiary diary = FoodDiary.builder()
                .member(member)
                .title(request.getTitle())
                .content(request.getContent())
                .rating(request.getRating())
                .diaryDate(request.getDiaryDate())
                .build();

        // 이미지 추가
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                FoodDiaryImage image = FoodDiaryImage.builder()
                        .imageUrl(request.getImageUrls().get(i))
                        .displayOrder(i)
                        .build();
                diary.addImage(image);
            }
        }

        try {
            FoodDiary savedDiary = foodDiaryRepository.save(diary);
            log.info("일기 등록 성공 - diaryId: {}, memberId: {}, date: {}", savedDiary.getId(), memberId, request.getDiaryDate());
            return DiaryResponseDTO.from(savedDiary);
        } catch (DataIntegrityViolationException e) {
            log.error("일기 등록 실패 - 중복 날짜: memberId={}, date={}", memberId, request.getDiaryDate());
            throw new BusinessException(ErrorCode.DUPLICATE_DIARY_DATE);
        }
    }

    // 캘린더 월별 조회
    public DiaryCalendarResponseDTO getMonthlyDiaries(Long memberId, int year, int month) {
        // 월 범위 검증
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("월은 1~12 사이여야 합니다.");
        }

        List<FoodDiary> diaries = foodDiaryRepository.findByMemberIdAndYearMonth(memberId, year, month);

        List<DiaryCalendarResponseDTO.DiaryDateInfo> diaryDateInfos = diaries.stream()
                .map(DiaryCalendarResponseDTO.DiaryDateInfo::from)
                .toList();

        log.info("캘린더 조회 - memberId: {}, year: {}, month: {}, count: {}", memberId, year, month, diaryDateInfos.size());
        return DiaryCalendarResponseDTO.builder()
                .year(year)
                .month(month)
                .diaries(diaryDateInfos)
                .build();
    }

    // 일기 상세 조회
    public DiaryResponseDTO getDiaryDetail(Long memberId, Long diaryId) {
        FoodDiary diary = foodDiaryRepository.findByIdWithImages(diaryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_NOT_FOUND));

        // 권한 체크
        if (!diary.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.DIARY_ACCESS_DENIED);
        }

        log.info("일기 상세 조회 - diaryId: {}, memberId: {}", diaryId, memberId);
        return DiaryResponseDTO.from(diary);
    }

    // 일기 수정 (날짜는 변경 불가)
    @Transactional
    public DiaryResponseDTO updateDiary(Long memberId, Long diaryId, UpdateDiaryRequestDTO request) {
        // 일기 조회 및 권한 체크
        FoodDiary diary = foodDiaryRepository.findByIdAndMemberId(diaryId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_NOT_FOUND));

        // 별점 검증
        if (!RatingValidator.isValid(request.getRating())) {
            throw new BusinessException(ErrorCode.INVALID_RATING);
        }

        // 이미지 개수 검증
        if (request.getImageUrls() != null && request.getImageUrls().size() > 4) {
            throw new BusinessException(ErrorCode.TOO_MANY_IMAGES);
        }

        // 일기 내용 수정 (날짜는 변경되지 않음)
        diary.update(request.getTitle(), request.getContent(), request.getRating());

        // 기존 이미지 모두 삭제
        diary.clearImages();

        // 새로운 이미지 추가
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                FoodDiaryImage image = FoodDiaryImage.builder()
                        .imageUrl(request.getImageUrls().get(i))
                        .displayOrder(i)
                        .build();
                diary.addImage(image);
            }
        }

        log.info("일기 수정 성공 - diaryId: {}, memberId: {}", diaryId, memberId);
        return DiaryResponseDTO.from(diary);
    }

    // 일기 삭제
    @Transactional
    public void deleteDiary(Long memberId, Long diaryId) {
        // 일기 조회 및 권한 체크
        FoodDiary diary = foodDiaryRepository.findByIdAndMemberId(diaryId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_NOT_FOUND));

        foodDiaryRepository.delete(diary);
        log.info("일기 삭제 성공 - diaryId: {}, memberId: {}", diaryId, memberId);
    }
}
