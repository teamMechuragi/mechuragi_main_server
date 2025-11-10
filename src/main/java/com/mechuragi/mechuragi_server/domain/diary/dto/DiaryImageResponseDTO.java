package com.mechuragi.mechuragi_server.domain.diary.dto;

import com.mechuragi.mechuragi_server.domain.diary.entity.FoodDiaryImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class DiaryImageResponseDTO {

    private final Long id;
    private final String imageUrl;
    private final Integer displayOrder;

    public static DiaryImageResponseDTO from(FoodDiaryImage image) {
        return DiaryImageResponseDTO.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .displayOrder(image.getDisplayOrder())
                .build();
    }
}
