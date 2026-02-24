package com.mechuragi.mechuragi_server.domain.preference.service;

import com.mechuragi.mechuragi_server.domain.preference.dto.*;
import com.mechuragi.mechuragi_server.domain.preference.entity.FoodPreference;
import com.mechuragi.mechuragi_server.domain.preference.repository.FoodPreferenceRepository;
import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.member.repository.MemberRepository;
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
    private final MemberRepository memberRepository;

    @Transactional
    public Long createPreference(Long memberId, CreatePreferenceRequestDTO request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        String preferenceName = generatePreferenceName(member, request.getPreferenceName());
        boolean isFirstPreference = foodPreferenceRepository.countByMemberId(member.getId()) == 0;

        FoodPreference preference = FoodPreference.builder()
                .member(member)
                .preferenceName(preferenceName)
                .isActive(isFirstPreference)
                .numberOfDiners(request.getNumberOfDiners())
                .dietStatus(request.getDietStatus())
                .veganOption(request.getVeganOption())
                .spiceLevel(request.getSpiceLevel())
                .preferredFoodTypes(request.getPreferredFoodTypes())
                .preferredTastes(request.getPreferredTastes())
                .avoidedFoods(request.getAvoidedFoods())
                .allergies(request.getAllergies())
                .build();

        return foodPreferenceRepository.save(preference).getId();
    }

    public List<PreferenceListResponseDTO> getPreferenceList(Long memberId) {
        return foodPreferenceRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(p -> PreferenceListResponseDTO.builder()
                        .id(p.getId())
                        .preferenceName(p.getPreferenceName())
                        .isActive(p.getIsActive())
                        .build())
                .collect(Collectors.toList());
    }

    public PreferenceDetailResponseDTO getPreferenceDetail(Long memberId, Long preferenceId) {
        FoodPreference preference = foodPreferenceRepository.findByIdAndMemberId(preferenceId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PREFERENCE_NOT_FOUND));

        return PreferenceDetailResponseDTO.builder()
                .id(preference.getId())
                .preferenceName(preference.getPreferenceName())
                .numberOfDiners(preference.getNumberOfDiners())
                .dietStatus(preference.getDietStatus())
                .veganOption(preference.getVeganOption())
                .spiceLevel(preference.getSpiceLevel())
                .preferredFoodTypes(preference.getPreferredFoodTypes())
                .preferredTastes(preference.getPreferredTastes())
                .avoidedFoods(preference.getAvoidedFoods())
                .allergies(preference.getAllergies())
                .createdAt(preference.getCreatedAt())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }

    @Transactional
    public void updatePreference(Long memberId, Long preferenceId, UpdatePreferenceRequestDTO request) {
        FoodPreference preference = foodPreferenceRepository.findByIdAndMemberId(preferenceId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PREFERENCE_NOT_FOUND));

        preference.updatePreference(
                request.getPreferenceName(),
                request.getNumberOfDiners(),
                request.getDietStatus(),
                request.getVeganOption(),
                request.getSpiceLevel(),
                request.getPreferredFoodTypes(),
                request.getPreferredTastes(),
                request.getAvoidedFoods(),
                request.getAllergies()
        );
    }

    @Transactional
    public void deletePreference(Long memberId, Long preferenceId) {
        foodPreferenceRepository.findByIdAndMemberId(preferenceId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PREFERENCE_NOT_FOUND));
        // @ElementCollection은 부모 삭제 시 컬렉션 테이블 데이터도 자동 삭제됨
        foodPreferenceRepository.deleteByIdAndMemberId(preferenceId, memberId);
    }

    public FoodPreference findActivePreference(Member member) {
        return foodPreferenceRepository.findByMemberAndIsActiveTrue(member)
                .orElseThrow(() -> new BusinessException(ErrorCode.PREFERENCE_NOT_FOUND));
    }

    @Transactional
    public void toggleActivePreference(Long memberId, Long preferenceId) {
        foodPreferenceRepository.findByIdAndMemberId(preferenceId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PREFERENCE_NOT_FOUND));

        List<FoodPreference> allPreferences = foodPreferenceRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
        allPreferences.forEach(p -> {
            if (p.getId().equals(preferenceId)) p.activate();
            else p.deactivate();
        });
    }

    private String generatePreferenceName(Member member, String requestedName) {
        if (requestedName != null && !requestedName.trim().isEmpty()) {
            return requestedName.trim();
        }
        int count = foodPreferenceRepository.countByMemberId(member.getId()) + 1;
        return "취향 " + count;
    }
}
