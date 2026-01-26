package com.mechuragi.mechuragi_server.domain.recommend.service;

import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.member.repository.MemberRepository;
import com.mechuragi.mechuragi_server.domain.recommend.dto.common.response.BookmarkedSessionResponse;
import com.mechuragi.mechuragi_server.domain.recommend.dto.common.response.RecommendedFoodResponse;
import com.mechuragi.mechuragi_server.domain.recommend.entity.Bookmark;
import com.mechuragi.mechuragi_server.domain.recommend.entity.RecommendationSession;
import com.mechuragi.mechuragi_server.domain.recommend.repository.BookmarkRepository;
import com.mechuragi.mechuragi_server.domain.recommend.repository.RecommendationSessionRepository;
import com.mechuragi.mechuragi_server.domain.recommend.service.mapper.RecommendedFoodMapper;
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
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final RecommendationSessionRepository sessionRepository;
    private final MemberRepository memberRepository;
    private final RecommendedFoodMapper recommendedFoodMapper;

    @Transactional
    public void bookmarkLatestSession(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        RecommendationSession session = sessionRepository.findTopByMemberIdOrderByCreatedAtDesc(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));

        if (bookmarkRepository.existsByMemberIdAndSessionId(memberId, session.getId())) {
            log.info("이미 북마크된 세션 - sessionId: {}", session.getId());
            return;
        }

        Bookmark bookmark = Bookmark.builder()
                .member(member)
                .session(session)
                .build();
        bookmarkRepository.save(bookmark);
        log.info("추천 세션 북마크 완료 - sessionId: {}", session.getId());
    }

    @Transactional
    public void toggleBookmarkBySessionId(Long memberId, Long sessionId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        RecommendationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));

        if (!session.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        var existingBookmark = bookmarkRepository.findByMemberIdAndSessionId(memberId, sessionId);

        if (existingBookmark.isPresent()) {
            bookmarkRepository.delete(existingBookmark.get());
            log.info("추천 세션 북마크 해제 - sessionId: {}", sessionId);
        } else {
            Bookmark bookmark = Bookmark.builder()
                    .member(member)
                    .session(session)
                    .build();
            bookmarkRepository.save(bookmark);
            log.info("추천 세션 북마크 - sessionId: {}", sessionId);
        }
    }

    public List<BookmarkedSessionResponse> getBookmarkedSessions(Long memberId) {
        memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        List<RecommendationSession> sessions = bookmarkRepository.findBookmarkedSessionsByMemberId(memberId);

        return sessions.stream()
                .map(this::toBookmarkedSessionResponse)
                .collect(Collectors.toList());
    }

    private BookmarkedSessionResponse toBookmarkedSessionResponse(RecommendationSession session) {
        List<RecommendedFoodResponse> foods = session.getRecommendedFoods().stream()
                .map(food -> recommendedFoodMapper.toDto(food, true))
                .collect(Collectors.toList());

        return BookmarkedSessionResponse.builder()
                .sessionId(session.getId())
                .foods(foods)
                .createdAt(session.getCreatedAt())
                .build();
    }
}
