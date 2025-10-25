package com.mechuragi.mechuragi_server.domain.ai.service;

import com.mechuragi.mechuragi_server.domain.ai.client.AiServiceClient;
import com.mechuragi.mechuragi_server.domain.ai.dto.FoodRecommendationRequest;
import com.mechuragi.mechuragi_server.domain.ai.dto.FoodRecommendationResponse;
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

    public FoodRecommendationResponse getWeatherBasedRecommendation(Long memberId, List<String> weatherConditions) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        FoodPreference activePreference = foodPreferenceService.findActivePreference(member);

        FoodRecommendationRequest request = buildRequest(
            FoodRecommendationRequest.RecommendationType.WEATHER,
            activePreference,
            weatherConditions,
            null,
            null
        );

        return aiServiceClient.getFoodRecommendation(request);
    }

    public FoodRecommendationResponse getTimeBasedRecommendation(Long memberId, String timeOfDay) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        FoodPreference activePreference = foodPreferenceService.findActivePreference(member);

        FoodRecommendationRequest request = buildRequest(
            FoodRecommendationRequest.RecommendationType.TIME_BASED,
            activePreference,
            null,
            timeOfDay,
            null
        );

        return aiServiceClient.getFoodRecommendation(request);
    }

    public FoodRecommendationResponse getConversationBasedRecommendation(Long memberId, String message) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        FoodPreference activePreference = foodPreferenceService.findActivePreference(member);

        FoodRecommendationRequest request = buildRequest(
            FoodRecommendationRequest.RecommendationType.CONVERSATION,
            activePreference,
            null,
            null,
            message
        );

        return aiServiceClient.getFoodRecommendation(request);
    }

    private FoodRecommendationRequest buildRequest(
            FoodRecommendationRequest.RecommendationType type,
            FoodPreference preference,
            List<String> weatherConditions,
            String timeOfDay,
            String message) {

        FoodRecommendationRequest.UserPreference userPref = new FoodRecommendationRequest.UserPreference();
        userPref.setDietStatus(preference.getIsOnDiet().name());
        userPref.setVeganOption(preference.getVeganOption().name());
        userPref.setSpiceLevel(preference.getSpiceLevel().name());

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

        userPref.setFoodTypes(foodTypes);
        userPref.setTastes(tastes);
        userPref.setDislikedFoods(dislikedFoods);

        FoodRecommendationRequest.ContextInfo context = new FoodRecommendationRequest.ContextInfo();
        context.setWeatherConditions(weatherConditions);
        context.setTimeOfDay(timeOfDay);

        FoodRecommendationRequest request = new FoodRecommendationRequest();
        request.setType(type);
        request.setUserPreference(userPref);
        request.setContext(context);
        request.setUserMessage(message);

        return request;
    }
}