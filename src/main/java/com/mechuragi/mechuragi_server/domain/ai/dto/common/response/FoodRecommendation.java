package com.mechuragi.mechuragi_server.domain.ai.dto.common.response;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodRecommendation {
    private String name;
    private String description;
    private String reason;
    private String ingredients;
    private String cookingTime;
    private String difficulty;
}