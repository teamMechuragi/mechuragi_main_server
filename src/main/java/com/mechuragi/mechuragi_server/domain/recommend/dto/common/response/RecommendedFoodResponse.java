package com.mechuragi.mechuragi_server.domain.recommend.dto.common.response;

import com.mechuragi.mechuragi_server.domain.recommend.entity.type.RecommendationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedFoodResponse {

    private Long id;
    private RecommendationType recommendationType;
    private String name;
    private String reason;
    private Boolean isBookmarked;
    private LocalDateTime createdAt;
}
