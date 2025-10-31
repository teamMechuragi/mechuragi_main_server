package com.mechuragi.mechuragi_server.domain.ai.dto.common.response;

import com.mechuragi.mechuragi_server.domain.ai.type.RecommendationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScrapedFoodResponse {
    private Long id;
    private RecommendationType recommendationType;
    private String name;
    private String description;
    private String reason;
    private String ingredients;
    private String cookingTime;
    private String difficulty;
    private LocalDateTime createdAt;
}
