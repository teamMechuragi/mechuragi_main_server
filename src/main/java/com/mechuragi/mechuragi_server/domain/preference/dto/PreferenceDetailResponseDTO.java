package com.mechuragi.mechuragi_server.domain.preference.dto;

import com.mechuragi.mechuragi_server.domain.preference.entity.FoodPreference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferenceDetailResponseDTO {

    private Long id;
    private String preferenceName;
    private Integer numberOfDiners;
    private String allergyInfo;
    private FoodPreference.DietStatus isOnDiet;
    private FoodPreference.VeganOption veganOption;
    private FoodPreference.SpiceLevel spiceLevel;
    private List<String> preferredFoodTypes;
    private List<String> preferredTastes;
    private List<String> dislikedFoods;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}