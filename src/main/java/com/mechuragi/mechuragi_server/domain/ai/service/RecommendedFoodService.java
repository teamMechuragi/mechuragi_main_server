package com.mechuragi.mechuragi_server.domain.ai.service;

import com.mechuragi.mechuragi_server.domain.ai.dto.common.response.RecommendedFoodResponse;
import com.mechuragi.mechuragi_server.domain.ai.dto.internal.request.SaveRecommendedFoodRequest;
import com.mechuragi.mechuragi_server.domain.ai.entity.RecommendedFood;
import com.mechuragi.mechuragi_server.domain.ai.repository.RecommendedFoodRepository;
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
public class RecommendedFoodService {

    private final RecommendedFoodRepository recommendedFoodRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void saveRecommendations(Long memberId, List<SaveRecommendedFoodRequest> recommendations) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        List<RecommendedFood> recommendedFoods = recommendations.stream()
                .map(req -> RecommendedFood.builder()
                        .member(member)
                        .recommendationType(req.getRecommendationType())
                        .name(req.getName())
                        .description(req.getDescription())
                        .reason(req.getReason())
                        .ingredients(req.getIngredients())
                        .cookingTime(req.getCookingTime())
                        .difficulty(req.getDifficulty())
                        .isScrapped(false)
                        .build())
                .collect(Collectors.toList());

        recommendedFoodRepository.saveAll(recommendedFoods);
        log.info("추천 결과 저장 완료 - 회원: {}, 개수: {}", memberId, recommendedFoods.size());
    }

    public List<RecommendedFoodResponse> getAllRecommendations(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        List<RecommendedFood> recommendations = recommendedFoodRepository.findByMemberOrderByCreatedAtDesc(member);

        return recommendations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<RecommendedFoodResponse> getScrappedRecommendations(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        List<RecommendedFood> scrappedRecommendations =
                recommendedFoodRepository.findByMemberAndIsScrapedTrueOrderByCreatedAtDesc(member);

        return scrappedRecommendations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void toggleScrap(Long memberId, Long recommendedFoodId) {
        RecommendedFood recommendedFood = recommendedFoodRepository.findById(recommendedFoodId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!recommendedFood.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if (recommendedFood.getIsScrapped()) {
            recommendedFood.unscrap();
            log.info("추천 음식 스크랩 해제 - ID: {}", recommendedFoodId);
        } else {
            recommendedFood.scrap();
            log.info("추천 음식 스크랩 - ID: {}", recommendedFoodId);
        }
    }

    private RecommendedFoodResponse toResponse(RecommendedFood food) {
        return RecommendedFoodResponse.builder()
                .id(food.getId())
                .recommendationType(food.getRecommendationType())
                .name(food.getName())
                .description(food.getDescription())
                .reason(food.getReason())
                .ingredients(food.getIngredients())
                .cookingTime(food.getCookingTime())
                .difficulty(food.getDifficulty())
                .isScrapped(food.getIsScrapped())
                .createdAt(food.getCreatedAt())
                .build();
    }
}
