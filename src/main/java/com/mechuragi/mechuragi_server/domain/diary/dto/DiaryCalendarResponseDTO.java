package com.mechuragi.mechuragi_server.domain.diary.dto;

import com.mechuragi.mechuragi_server.domain.diary.entity.FoodDiary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class DiaryCalendarResponseDTO {

    private final int year;
    private final int month;
    private final List<DiaryDateInfo> diaries;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class DiaryDateInfo {
        private final Long diaryId;
        private final LocalDate diaryDate;
        private final List<String> thumbnails;

        public static DiaryDateInfo from(FoodDiary diary) {
            List<String> thumbnails = diary.getImages().stream()
                    .sorted(Comparator.comparing(img -> img.getDisplayOrder()))
                    .map(img -> img.getImageUrl())
                    .toList();

            return DiaryDateInfo.builder()
                    .diaryId(diary.getId())
                    .diaryDate(diary.getDiaryDate())
                    .thumbnails(thumbnails)
                    .build();
        }
    }
}
