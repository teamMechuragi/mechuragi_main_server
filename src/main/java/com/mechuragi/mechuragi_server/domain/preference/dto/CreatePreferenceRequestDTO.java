package com.mechuragi.mechuragi_server.domain.preference.dto;

import com.mechuragi.mechuragi_server.domain.preference.entity.FoodPreference;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class CreatePreferenceRequestDTO {

    private String preferenceName;

    @NotNull(message = "인원 수는 필수입니다")
    @Positive(message = "인원 수는 1명 이상이어야 합니다")
    private Integer numberOfDiners;

    @NotNull(message = "다이어트 상태는 필수입니다")
    private FoodPreference.DietStatus dietStatus;

    @NotNull(message = "비건 옵션은 필수입니다")
    private FoodPreference.VeganOption veganOption;

    @NotNull(message = "매운맛 단계는 필수입니다")
    private FoodPreference.SpiceLevel spiceLevel;

    @NotEmpty(message = "선호 음식 유형은 최소 1개 이상 선택해야 합니다")
    private List<String> preferredFoodTypes;

    @NotEmpty(message = "선호 맛은 최소 1개 이상 선택해야 합니다")
    private List<String> preferredTastes;

    private List<String> avoidedFoods;

    private List<String> allergies;
}