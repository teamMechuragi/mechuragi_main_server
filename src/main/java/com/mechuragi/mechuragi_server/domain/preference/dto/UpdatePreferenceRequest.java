package com.mechuragi.mechuragi_server.domain.preference.dto;

import com.mechuragi.mechuragi_server.domain.preference.entity.FoodPreference;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class UpdatePreferenceRequest {

    private String preferenceName;

    @Positive(message = "인원 수는 1명 이상이어야 합니다")
    private Integer numberOfDiners;

    private String allergyInfo;

    private FoodPreference.DietStatus isOnDiet;

    private FoodPreference.VeganOption veganOption;

    private FoodPreference.SpiceLevel spiceLevel;

    private List<String> preferredFoodTypes;

    private List<String> preferredTastes;

    private List<String> dislikedFoods;

    public UpdatePreferenceRequest(String preferenceName, Integer numberOfDiners, String allergyInfo,
                                 FoodPreference.DietStatus isOnDiet, FoodPreference.VeganOption veganOption,
                                 FoodPreference.SpiceLevel spiceLevel, List<String> preferredFoodTypes,
                                 List<String> preferredTastes, List<String> dislikedFoods) {
        this.preferenceName = preferenceName;
        this.numberOfDiners = numberOfDiners;
        this.allergyInfo = allergyInfo;
        this.isOnDiet = isOnDiet;
        this.veganOption = veganOption;
        this.spiceLevel = spiceLevel;
        this.preferredFoodTypes = preferredFoodTypes;
        this.preferredTastes = preferredTastes;
        this.dislikedFoods = dislikedFoods;
    }
}