package com.mechuragi.mechuragi_server.domain.ai.service;

import com.mechuragi.mechuragi_server.domain.ai.client.AiServiceClient;
import com.mechuragi.mechuragi_server.domain.ai.dto.external.request.FoodPreferenceDto;
import com.mechuragi.mechuragi_server.domain.ai.dto.external.request.FoodRecommendationRequest;
import com.mechuragi.mechuragi_server.domain.ai.dto.common.response.FoodRecommendationResponse;
import com.mechuragi.mechuragi_server.domain.ai.service.mapper.FoodRecommendationMapper;
import com.mechuragi.mechuragi_server.domain.preference.entity.FoodPreference;
import com.mechuragi.mechuragi_server.domain.preference.repository.DislikedFoodRepository;
import com.mechuragi.mechuragi_server.domain.preference.repository.PreferenceFoodTypeRepository;
import com.mechuragi.mechuragi_server.domain.preference.repository.PreferenceTasteRepository;
import com.mechuragi.mechuragi_server.domain.preference.service.FoodPreferenceService;
import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.member.repository.MemberRepository;
import com.mechuragi.mechuragi_server.global.exception.BusinessException;
import com.mechuragi.mechuragi_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AiRecommendationService {

    private final AiServiceClient aiServiceClient;
    private final MemberRepository memberRepository;
    private final FoodPreferenceService foodPreferenceService;
    private final PreferenceFoodTypeRepository preferenceFoodTypeRepository;
    private final PreferenceTasteRepository preferenceTasteRepository;
    private final DislikedFoodRepository dislikedFoodRepository;
    private final FoodRecommendationMapper foodRecommendationMapper;

    public FoodRecommendationResponse getWeatherBasedRecommendation(Long memberId, List<String> weatherConditions) {
        FoodPreferenceDto preferenceDto = getMemberFoodPreferenceDto(memberId);
        FoodRecommendationRequest request = foodRecommendationMapper.createWeatherBasedRequest(
                preferenceDto,
                weatherConditions
        );

        return aiServiceClient.getFoodRecommendation(request);
    }

    public FoodRecommendationResponse getTimeBasedRecommendation(Long memberId, String timeOfDay) {
        FoodPreferenceDto preferenceDto = getMemberFoodPreferenceDto(memberId);
        FoodRecommendationRequest request = foodRecommendationMapper.createTimeBasedRequest(
                preferenceDto,
                timeOfDay
        );

        return aiServiceClient.getFoodRecommendation(request);
    }

    public FoodRecommendationResponse getIngredientsBasedRecommendation(Long memberId, List<String> ingredients) {
        FoodPreferenceDto preferenceDto = getMemberFoodPreferenceDto(memberId);
        FoodRecommendationRequest request = foodRecommendationMapper.createIngredientsBasedRequest(
                preferenceDto,
                ingredients
        );

        return aiServiceClient.getFoodRecommendation(request);
    }

    public FoodRecommendationResponse getFeelingBasedRecommendation(Long memberId, String feeling) {
        FoodPreferenceDto preferenceDto = getMemberFoodPreferenceDto(memberId);
        FoodRecommendationRequest request = foodRecommendationMapper.createFeelingBasedRequest(
                preferenceDto,
                feeling
        );

        return aiServiceClient.getFoodRecommendation(request);
    }


    public FoodRecommendationResponse getConversationBasedRecommendation(Long memberId, String message) {
        FoodPreferenceDto preferenceDto = getMemberFoodPreferenceDto(memberId);
        FoodRecommendationRequest request = foodRecommendationMapper.createConversationBasedRequest(
                preferenceDto,
                message
        );

        return aiServiceClient.getFoodRecommendation(request);
    }
     // 헬퍼 메서드: 멤버 ID로 회원 조회 → 활성화된 음식 취향 조회 → DTO 변환
    private FoodPreferenceDto getMemberFoodPreferenceDto(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        FoodPreference activePreference = foodPreferenceService.findActivePreference(member);
        return toFoodPreferenceDto(activePreference);
    }


     // 헬퍼 메서드: 음식 타입, 맛, 싫어하는 음식 목록 조회 -> FoodPreference 엔티티를 FoodPreferenceDto로 변환
    private FoodPreferenceDto toFoodPreferenceDto(FoodPreference preference) {
        List<String> foodTypes = preferenceFoodTypeRepository.findByPreferenceId(preference.getId())
                .stream()
                .map(foodType -> foodType.getFoodType().name())
                .collect(Collectors.toList());

        List<String> tastes = preferenceTasteRepository.findByPreferenceId(preference.getId())
                .stream()
                .map(taste -> taste.getTasteType().name())
                .collect(Collectors.toList());

        List<String> dislikedFoods = dislikedFoodRepository.findByPreferenceId(preference.getId())
                .stream()
                .map(disliked -> disliked.getFoodName())
                .collect(Collectors.toList());

        return FoodPreferenceDto.builder()
                .dietStatus(preference.getIsOnDiet().name())
                .veganOption(preference.getVeganOption().name())
                .spiceLevel(preference.getSpiceLevel().name())
                .foodTypes(foodTypes)
                .tastes(tastes)
                .dislikedFoods(dislikedFoods)
                .build();
    }
}