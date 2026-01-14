package com.mechuragi.mechuragi_server.domain.recommend.dto.external.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FoodPreferenceRequest {
    private String dietStatus;
    private String veganOption;
    private String spiceLevel;
    private List<String> foodTypes;
    private List<String> tastes;
    private List<String> dislikedFoods;
}
