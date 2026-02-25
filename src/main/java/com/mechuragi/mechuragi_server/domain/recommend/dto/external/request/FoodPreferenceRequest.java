package com.mechuragi.mechuragi_server.domain.recommend.dto.external.request;

import com.mechuragi.mechuragi_server.domain.preference.entity.FoodPreference;
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
    private Integer numberOfDiners;
    private FoodPreference.DietStatus dietStatus;
    private FoodPreference.VeganOption veganOption;
    private FoodPreference.SpiceLevel spiceLevel;
    private List<String> foodTypes;
    private List<String> tastes;
    private List<String> avoidedFoods;
    private List<String> allergies;
}
