package com.mechuragi.mechuragi_server.domain.ai.service;

import com.mechuragi.mechuragi_server.domain.ai.client.AiServiceClient;
import com.mechuragi.mechuragi_server.domain.ai.dto.request.FoodPreferenceDto;
import com.mechuragi.mechuragi_server.domain.ai.dto.request.FoodRecommendationRequest;
import com.mechuragi.mechuragi_server.domain.ai.dto.response.FoodRecommendationResponse;
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
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        FoodPreference activePreference = foodPreferenceService.findActivePreference(member);

        FoodPreferenceDto preferenceDto = toFoodPreferenceDto(activePreference);
        FoodRecommendationRequest request = foodRecommendationMapper.createWeatherBasedRequest(
                preferenceDto,
                weatherConditions
        );

        return aiServiceClient.getFoodRecommendation(request);
    }

    public FoodRecommendationResponse getTimeBasedRecommendation(Long memberId, String timeOfDay) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        FoodPreference activePreference = foodPreferenceService.findActivePreference(member);

        FoodPreferenceDto preferenceDto = toFoodPreferenceDto(activePreference);
        FoodRecommendationRequest request = foodRecommendationMapper.createTimeBasedRequest(
                preferenceDto,
                timeOfDay
        );

        return aiServiceClient.getFoodRecommendation(request);
    }

    public FoodRecommendationResponse getIngredientsBasedRecommendation(Long memberId, List<String> ingredients) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        FoodPreference activePreference = foodPreferenceService.findActivePreference(member);

        FoodPreferenceDto preferenceDto = toFoodPreferenceDto(activePreference);
        FoodRecommendationRequest request = foodRecommendationMapper.createIngredientsBasedRequest(
                preferenceDto,
                ingredients
        );

        return aiServiceClient.getFoodRecommendation(request);
    }

    public FoodRecommendationResponse getFeelingBasedRecommendation(Long memberId, String feeling) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        FoodPreference activePreference = foodPreferenceService.findActivePreference(member);

        FoodPreferenceDto preferenceDto = toFoodPreferenceDto(activePreference);
        FoodRecommendationRequest request = foodRecommendationMapper.createFeelingBasedRequest(
                preferenceDto,
                feeling
        );

        return aiServiceClient.getFoodRecommendation(request);
    }


    public FoodRecommendationResponse getConversationBasedRecommendation(Long memberId, String message) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        FoodPreference activePreference = foodPreferenceService.findActivePreference(member);

        FoodPreferenceDto preferenceDto = toFoodPreferenceDto(activePreference);
        FoodRecommendationRequest request = foodRecommendationMapper.createConversationBasedRequest(
                preferenceDto,
                message
        );

        return aiServiceClient.getFoodRecommendation(request);
    }

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

        return foodRecommendationMapper.toFoodPreferenceDto(
                preference.getIsOnDiet().name(),
                preference.getVeganOption().name(),
                preference.getSpiceLevel().name(),
                foodTypes,
                tastes,
                dislikedFoods
        );
    }
}