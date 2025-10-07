package com.mechuragi.mechuragi_server.domain.preference.dto;

import com.mechuragi.mechuragi_server.domain.preference.entity.FoodPreference;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class PreferenceDetailResponse {

    private final Long id;
    private final String preferenceName;
    private final Integer numberOfDiners;
    private final String allergyInfo;
    private final FoodPreference.DietStatus isOnDiet;
    private final FoodPreference.VeganOption veganOption;
    private final FoodPreference.SpiceLevel spiceLevel;
    private final List<String> preferredFoodTypes;
    private final List<String> preferredTastes;
    private final List<String> dislikedFoods;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public PreferenceDetailResponse(Long id, String preferenceName, Integer numberOfDiners, String allergyInfo,
                                  FoodPreference.DietStatus isOnDiet, FoodPreference.VeganOption veganOption,
                                  FoodPreference.SpiceLevel spiceLevel, List<String> preferredFoodTypes,
                                  List<String> preferredTastes, List<String> dislikedFoods,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.preferenceName = preferenceName;
        this.numberOfDiners = numberOfDiners;
        this.allergyInfo = allergyInfo;
        this.isOnDiet = isOnDiet;
        this.veganOption = veganOption;
        this.spiceLevel = spiceLevel;
        this.preferredFoodTypes = preferredFoodTypes;
        this.preferredTastes = preferredTastes;
        this.dislikedFoods = dislikedFoods;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}