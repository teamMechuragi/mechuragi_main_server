package com.mechuragi.mechuragi_server.domain.preference.service;

import com.mechuragi.mechuragi_server.domain.preference.dto.*;
import com.mechuragi.mechuragi_server.domain.preference.entity.*;
import com.mechuragi.mechuragi_server.domain.preference.repository.*;
import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.global.exception.BusinessException;
import com.mechuragi.mechuragi_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FoodPreferenceService {

    private final FoodPreferenceRepository foodPreferenceRepository;
    private final PreferenceFoodTypeRepository preferenceFoodTypeRepository;
    private final PreferenceTasteRepository preferenceTasteRepository;
    private final DislikedFoodRepository dislikedFoodRepository;

    // 새로운 음식 취향 등록
    @Transactional
    public Long createPreference(Member member, CreatePreferenceRequest request) {
        String preferenceName = generatePreferenceName(member, request.getPreferenceName());

        // 첫 번째 취향이면 자동 활성화
        boolean isFirstPreference = foodPreferenceRepository.countByMemberId(member.getId()) == 0;

        FoodPreference preference = FoodPreference.builder()
                .member(member)
                .preferenceName(preferenceName)
                .isActive(isFirstPreference)
                .numberOfDiners(request.getNumberOfDiners())
                .allergyInfo(request.getAllergyInfo())
                .isOnDiet(request.getIsOnDiet())
                .veganOption(request.getVeganOption())
                .spiceLevel(request.getSpiceLevel())
                .build();

        FoodPreference savedPreference = foodPreferenceRepository.save(preference);

        saveFoodTypes(savedPreference, request.getPreferredFoodTypes());
        saveTastes(savedPreference, request.getPreferredTastes());
        saveDislikedFoods(savedPreference, request.getDislikedFoods());

        return savedPreference.getId();
    }

    // 모든 음식 취향 목록 조회
    public List<PreferenceListResponse> getPreferenceList(Long memberId) {
        List<FoodPreference> preferences = foodPreferenceRepository.findByMemberIdOrderByCreatedAtDesc(memberId);

        return preferences.stream()
                .map(preference -> new PreferenceListResponse(
                        preference.getId(),
                        preference.getPreferenceName(),
                        preference.getIsActive()
                ))
                .collect(Collectors.toList());
    }

    // 특정 음식 취향 상세 정보 조회
    public PreferenceDetailResponse getPreferenceDetail(Long memberId, Long preferenceId) {
        FoodPreference preference = foodPreferenceRepository.findByIdAndMemberId(preferenceId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PREFERENCE_NOT_FOUND));

        List<String> foodTypes = preferenceFoodTypeRepository.findByPreferenceId(preferenceId)
                .stream()
                .map(type -> type.getFoodType().name())
                .collect(Collectors.toList());

        List<String> tastes = preferenceTasteRepository.findByPreferenceId(preferenceId)
                .stream()
                .map(taste -> taste.getTasteType().name())
                .collect(Collectors.toList());

        List<String> dislikedFoods = dislikedFoodRepository.findByPreferenceId(preferenceId)
                .stream()
                .map(DislikedFood::getFoodName)
                .collect(Collectors.toList());

        return new PreferenceDetailResponse(
                preference.getId(),
                preference.getPreferenceName(),
                preference.getNumberOfDiners(),
                preference.getAllergyInfo(),
                preference.getIsOnDiet(),
                preference.getVeganOption(),
                preference.getSpiceLevel(),
                foodTypes,
                tastes,
                dislikedFoods,
                preference.getCreatedAt(),
                preference.getUpdatedAt()
        );
    }

    // 음식 취향 정보 수정
    @Transactional
    public void updatePreference(Long memberId, Long preferenceId, UpdatePreferenceRequest request) {
        FoodPreference preference = foodPreferenceRepository.findByIdAndMemberId(preferenceId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PREFERENCE_NOT_FOUND));

        preference.updatePreference(
                request.getPreferenceName(),
                request.getNumberOfDiners(),
                request.getAllergyInfo(),
                request.getIsOnDiet(),
                request.getVeganOption(),
                request.getSpiceLevel()
        );

        if (request.getPreferredFoodTypes() != null) {
            preferenceFoodTypeRepository.deleteByPreferenceId(preferenceId);
            saveFoodTypes(preference, request.getPreferredFoodTypes());
        }

        if (request.getPreferredTastes() != null) {
            preferenceTasteRepository.deleteByPreferenceId(preferenceId);
            saveTastes(preference, request.getPreferredTastes());
        }

        if (request.getDislikedFoods() != null) {
            dislikedFoodRepository.deleteByPreferenceId(preferenceId);
            saveDislikedFoods(preference, request.getDislikedFoods());
        }
    }

    // 음식 취향 삭제
    @Transactional
    public void deletePreference(Long memberId, Long preferenceId) {
        FoodPreference preference = foodPreferenceRepository.findByIdAndMemberId(preferenceId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PREFERENCE_NOT_FOUND));

        preferenceFoodTypeRepository.deleteByPreferenceId(preferenceId);
        preferenceTasteRepository.deleteByPreferenceId(preferenceId);
        dislikedFoodRepository.deleteByPreferenceId(preferenceId);
        foodPreferenceRepository.deleteByIdAndMemberId(preferenceId, memberId);
    }

    // 취향 이름 자동 생성 or 사용자 입력값 검증
    private String generatePreferenceName(Member member, String requestedName) {
        if (requestedName != null && !requestedName.trim().isEmpty()) {
            return requestedName.trim();
        }

        int count = foodPreferenceRepository.countByMemberId(member.getId()) + 1;
        return "취향 " + count;
    }

    // 선호하는 음식 유형들 저장
    private void saveFoodTypes(FoodPreference preference, List<String> foodTypes) {
        if (foodTypes != null && !foodTypes.isEmpty()) {
            List<PreferenceFoodType> entities = foodTypes.stream()
                    .map(type -> PreferenceFoodType.builder()
                            .preference(preference)
                            .foodType(PreferenceFoodType.FoodType.valueOf(type))
                            .build())
                    .collect(Collectors.toList());
            preferenceFoodTypeRepository.saveAll(entities);
        }
    }

    // 선호하는 맛들 저장
    private void saveTastes(FoodPreference preference, List<String> tastes) {
        if (tastes != null && !tastes.isEmpty()) {
            List<PreferenceTaste> entities = tastes.stream()
                    .map(taste -> PreferenceTaste.builder()
                            .preference(preference)
                            .tasteType(PreferenceTaste.TasteType.valueOf(taste))
                            .build())
                    .collect(Collectors.toList());
            preferenceTasteRepository.saveAll(entities);
        }
    }

    // 기피하는 음식들 저장
    private void saveDislikedFoods(FoodPreference preference, List<String> dislikedFoods) {
        if (dislikedFoods != null && !dislikedFoods.isEmpty()) {
            List<DislikedFood> entities = dislikedFoods.stream()
                    .map(food -> DislikedFood.builder()
                            .preference(preference)
                            .foodName(food)
                            .build())
                    .collect(Collectors.toList());
            dislikedFoodRepository.saveAll(entities);
        }
    }

    // 활성화된 음식 취향 조회 (AI 추천용)
    public FoodPreference findActivePreference(Member member) {
        return foodPreferenceRepository.findByMemberAndIsActiveTrue(member)
                .orElseThrow(() -> new BusinessException(ErrorCode.PREFERENCE_NOT_FOUND));
    }

    // 음식 취향 활성화 토글 (한 번에 하나만 활성화)
    @Transactional
    public void toggleActivePreference(Long memberId, Long preferenceId) {
        // 해당 취향이 사용자 소유인지 확인
        FoodPreference targetPreference = foodPreferenceRepository.findByIdAndMemberId(preferenceId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PREFERENCE_NOT_FOUND));

        // 사용자의 모든 취향 조회
        List<FoodPreference> allPreferences = foodPreferenceRepository.findByMemberIdOrderByCreatedAtDesc(memberId);

        // 선택한 취향만 활성화, 나머지는 비활성화
        allPreferences.forEach(preference -> {
            if (preference.getId().equals(preferenceId)) {
                preference.activate();
            } else {
                preference.deactivate();
            }
        });
    }
}