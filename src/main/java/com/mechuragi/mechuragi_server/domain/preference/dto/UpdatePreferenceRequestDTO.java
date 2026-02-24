package com.mechuragi.mechuragi_server.domain.preference.dto;

import com.mechuragi.mechuragi_server.domain.preference.entity.FoodPreference;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePreferenceRequestDTO {

    private String preferenceName;

    @Positive(message = "인원 수는 1명 이상이어야 합니다")
    private Integer numberOfDiners;

    private FoodPreference.DietStatus dietStatus;

    private FoodPreference.VeganOption veganOption;

    private FoodPreference.SpiceLevel spiceLevel;

    private List<String> preferredFoodTypes;

    private List<String> preferredTastes;

    private List<String> avoidedFoods;

    private List<String> allergies;
}