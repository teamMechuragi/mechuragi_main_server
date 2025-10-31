package com.mechuragi.mechuragi_server.domain.ai.service;

import com.mechuragi.mechuragi_server.domain.ai.dto.internal.request.ScrapeFoodRequest;
import com.mechuragi.mechuragi_server.domain.ai.dto.common.response.ScrapedFoodResponse;
import com.mechuragi.mechuragi_server.domain.ai.entity.ScrapedFood;
import com.mechuragi.mechuragi_server.domain.ai.repository.ScrapedFoodRepository;
import com.mechuragi.mechuragi_server.domain.ai.service.mapper.ScrapFoodMapper;
import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.member.repository.MemberRepository;
import com.mechuragi.mechuragi_server.global.exception.BusinessException;
import com.mechuragi.mechuragi_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapFoodService {
    private final MemberRepository memberRepository;
    private final ScrapedFoodRepository scrapedFoodRepository;
    private final ScrapFoodMapper scrapFoodMapper;

    /**
     * 음식 추천 스크랩 저장
     */
    @Transactional
    public ScrapedFoodResponse saveScrapRecommendation(Long memberId, ScrapeFoodRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        ScrapedFood recommendation = scrapFoodMapper.toEntity(request, member);
        ScrapedFood savedRecommendation = scrapedFoodRepository.save(recommendation);

        return scrapFoodMapper.toResponse(savedRecommendation);
    }

    /**
     * 사용자의 스크랩된 음식 추천 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ScrapedFoodResponse> getScrapedRecommendations(Long memberId) {
        List<ScrapedFood> recommendations = scrapedFoodRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
        return scrapFoodMapper.toResponseList(recommendations);
    }

    /**
     * 스크랩된 음식 추천 상세 조회
     */
    @Transactional(readOnly = true)
    public ScrapedFoodResponse getScrapedRecommendationDetail(Long memberId, Long recommendationId) {
        ScrapedFood recommendation = scrapedFoodRepository.findById(recommendationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));

        // 본인의 스크랩인지 검증
        if (!recommendation.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.SCRAP_FORBIDDEN);
        }

        return scrapFoodMapper.toResponse(recommendation);
    }

    /**
     * 스크랩된 음식 추천 삭제
     */
    @Transactional
    public void deleteScrapedRecommendation(Long memberId, Long recommendationId) {
        ScrapedFood recommendation = scrapedFoodRepository.findById(recommendationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));

        // 본인의 스크랩인지 검증
        if (!recommendation.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.SCRAP_FORBIDDEN);
        }

        scrapedFoodRepository.delete(recommendation);
    }
}
