package com.mechuragi.mechuragi_server.domain.recommend.service;

import com.mechuragi.mechuragi_server.domain.recommend.dto.common.response.RecommendedFoodResponse;
import com.mechuragi.mechuragi_server.domain.recommend.dto.external.request.SaveRecommendationsRequest;
import com.mechuragi.mechuragi_server.domain.recommend.entity.RecommendationSession;
import com.mechuragi.mechuragi_server.domain.recommend.entity.RecommendedFood;
import com.mechuragi.mechuragi_server.domain.recommend.repository.RecommendationSessionRepository;
import com.mechuragi.mechuragi_server.domain.recommend.repository.RecommendedFoodRepository;
import com.mechuragi.mechuragi_server.domain.recommend.service.mapper.RecommendedFoodMapper;
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
    private final RecommendationSessionRepository sessionRepository;
    private final MemberRepository memberRepository;
    private final RecommendedFoodMapper recommendedFoodMapper;

    @Transactional
    public void saveRecommendations(Long memberId, SaveRecommendationsRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 세션 생성
        RecommendationSession session = recommendedFoodMapper.toSessionEntity(request, member);
        sessionRepository.save(session);

        // 추천 음식 저장
        List<RecommendedFood> recommendedFoods = request.getRecommendations().stream()
                .map(req -> recommendedFoodMapper.toEntity(req, member))
                .collect(Collectors.toList());

        recommendedFoods.forEach(session::addRecommendedFood);
        recommendedFoodRepository.saveAll(recommendedFoods);

        log.info("추천 결과 저장 완료 - 회원: {}, 세션: {}, 개수: {}", memberId, session.getId(), recommendedFoods.size());
    }

    public List<RecommendedFoodResponse> getAllRecommendations(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        List<RecommendedFood> recommendations = recommendedFoodRepository.findByMemberOrderByCreatedAtDesc(member);

        return recommendations.stream()
                .map(recommendedFoodMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<RecommendedFoodResponse> getScrappedRecommendations(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        List<RecommendedFood> scrappedRecommendations =
                recommendedFoodRepository.findByMemberAndIsScrappedTrueOrderByCreatedAtDesc(member);

        return scrappedRecommendations.stream()
                .map(recommendedFoodMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void toggleScrap(Long memberId, Long recommendedFoodId) {
        RecommendedFood recommendedFood = recommendedFoodRepository.findById(recommendedFoodId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));

        if (!recommendedFood.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        if (recommendedFood.getIsScrapped()) {
            recommendedFood.unscrap();
            log.info("추천 음식 스크랩 해제 - ID: {}", recommendedFoodId);
        } else {
            recommendedFood.scrap();
            log.info("추천 음식 스크랩 - ID: {}", recommendedFoodId);
        }
    }
}
