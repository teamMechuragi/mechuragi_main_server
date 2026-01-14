package com.mechuragi.mechuragi_server.domain.diary.dto;

import com.mechuragi.mechuragi_server.domain.diary.entity.FoodDiary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class DiaryResponseDTO {

    private final Long id;
    private final String title;
    private final String content;
    private final BigDecimal rating;
    private final LocalDate diaryDate;
    private final List<DiaryImageResponseDTO> images;
    private final List<String> tags;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static DiaryResponseDTO from(FoodDiary diary) {
        List<DiaryImageResponseDTO> images = diary.getImages().stream()
                .sorted(Comparator.comparing(img -> img.getDisplayOrder()))
                .map(DiaryImageResponseDTO::from)
                .toList();

        List<String> tags = diary.getDiaryTags().stream()
                .map(diaryTag -> diaryTag.getTag().getName())
                .toList();

        return DiaryResponseDTO.builder()
                .id(diary.getId())
                .title(diary.getTitle())
                .content(diary.getContent())
                .rating(diary.getRating())
                .diaryDate(diary.getDiaryDate())
                .images(images)
                .tags(tags)
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .build();
    }
}
