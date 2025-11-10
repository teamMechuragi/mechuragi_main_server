package com.mechuragi.mechuragi_server.domain.vote.service;

import com.mechuragi.mechuragi_server.domain.vote.dto.MenuOptionDTO;
import com.mechuragi.mechuragi_server.domain.vote.dto.MenuScoreDTO;
import com.mechuragi.mechuragi_server.domain.vote.dto.PopularMenuResponseDTO;
import com.mechuragi.mechuragi_server.domain.vote.dto.VoteResponseDTO;
import com.mechuragi.mechuragi_server.domain.vote.dto.VoteResponseDTO.VoteOptionResponseDTO;
import com.mechuragi.mechuragi_server.domain.vote.entity.VotePost;
import com.mechuragi.mechuragi_server.domain.vote.repository.VotePostRepository;
import com.mechuragi.mechuragi_server.domain.vote.service.calculator.MenuScoreCalculator;
import com.mechuragi.mechuragi_server.domain.vote.service.mapper.PopularMenuMapper;
import com.mechuragi.mechuragi_server.domain.vote.util.MenuNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PopularMenuService {

    private final VotePostRepository votePostRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final PopularMenuMapper popularMenuMapper;
    private final MenuScoreCalculator menuScoreCalculator;

    private static final String HOT_VOTE_SORTED_SET_KEY = "vote:hot";
    private static final int DEFAULT_HOT_VOTE_SIZE = 50;

    /**
     * 실시간 인기 메뉴 Top 10 조회
     * 1분 TTL 캐싱 적용 (RedisCacheConfig에서 설정)
     */
    @Cacheable(value = "popularMenus", key = "'realtime'")
    public List<PopularMenuResponseDTO> getPopularMenus() {
        long startTime = System.currentTimeMillis();
        List<PopularMenuResponseDTO> result = calculatePopularMenusRealtime();
        long duration = System.currentTimeMillis() - startTime;

        log.info("인기 메뉴 계산 완료: Top {}개, 소요시간 {}ms", result.size(), duration);
        return result;
    }

    /**
     * 실시간 인기 메뉴 계산 (캐시 우회)
     */
    private List<PopularMenuResponseDTO> calculatePopularMenusRealtime() {
        // Step 1: Hot 투표 데이터 가져오기 (실시간 - vote:hot 직접 조회)
        List<VoteResponseDTO> hotVotes = getHotVotesRealtime(DEFAULT_HOT_VOTE_SIZE);

        if (hotVotes.isEmpty()) {
            log.info("Hot 투표가 없습니다. 빈 리스트 반환");
            return Collections.emptyList();
        }

        // Step 2: 모든 메뉴 옵션 추출 및 실시간 투표율 계산
        List<MenuOptionDTO> allOptions = new ArrayList<>();

        for (VoteResponseDTO vote : hotVotes) {
            // 실시간 총 참여자 수 조회 (Redis)
            String participantsKey = "vote:" + vote.getId() + ":participants";
            int totalParticipants = Optional.ofNullable(redisTemplate.opsForValue().get(participantsKey))
                    .map(Integer::parseInt)
                    .orElse(0);

            for (VoteOptionResponseDTO option : vote.getOptions()) {
                // 실시간 투표율 재계산 (Redis 기반)
                double realtimeVotePercentage = totalParticipants > 0
                        ? (double) option.getVoteCount() / totalParticipants * 100.0
                        : 0.0;

                allOptions.add(new MenuOptionDTO(
                        option.getOptionText(),
                        option.getVoteCount(),
                        realtimeVotePercentage,
                        vote.getCreatedAt()
                ));
            }
        }

        log.debug("추출된 총 옵션 수: {}", allOptions.size());

        // Step 3 ~ 5: 메뉴별 그룹화, 점수 계산
        Map<String, MenuScoreDTO> menuMap = new HashMap<>();

        for (MenuOptionDTO option : allOptions) {
            // 정규화 및 동의어 처리
            String canonical = MenuNormalizer.normalize(option.getOptionText());

            // 빈 문자열은 필터링
            if (canonical.isEmpty()) {
                continue;
            }

            // 최근성 계산 (7일 기준)
            double recencyScore = menuScoreCalculator.calculateRecencyScore(option.getCreatedAt());

            // 메뉴별 데이터 누적
            menuMap.computeIfAbsent(canonical, k -> new MenuScoreDTO(canonical));
            menuMap.get(canonical).addData(option.getRealtimeVotePercentage(), recencyScore);
        }

        // Step 6: 상위 10개 메뉴 정렬 및 반환
        List<PopularMenuResponseDTO> topMenus = menuMap.values().stream()
                .sorted(Comparator.comparingDouble(MenuScoreDTO::getScore).reversed())
                .limit(10)
                .map(popularMenuMapper::toDTO)
                .collect(Collectors.toList());

        log.info("인기 메뉴 Top 10 계산 완료: Hot 투표 {}개, 총 옵션 {}개, 고유 메뉴 {}개",
                hotVotes.size(), allOptions.size(), menuMap.size());

        return topMenus;
    }

    /**
     * Hot 투표 실시간 조회 (캐시 우회)
     * vote:hot Sorted Set을 직접 조회하여 getHotVotes()의 5분 캐싱 우회
     */
    private List<VoteResponseDTO> getHotVotesRealtime(int size) {
        // Redis Sorted Set에서 상위 N개의 투표 ID 조회 (점수 높은 순)
        Set<String> topVoteIds = redisTemplate.opsForZSet()
                .reverseRange(HOT_VOTE_SORTED_SET_KEY, 0, size - 1);

        if (topVoteIds == null || topVoteIds.isEmpty()) {
            return Collections.emptyList();
        }

        // VotePost 조회
        List<Long> voteIds = topVoteIds.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());

        List<VotePost> hotVotes = votePostRepository.findAllById(voteIds);

        // Redis에서 가져온 순서대로 정렬
        Map<Long, VotePost> voteMap = hotVotes.stream()
                .collect(Collectors.toMap(VotePost::getId, v -> v));

        return voteIds.stream()
                .map(voteMap::get)
                .filter(Objects::nonNull)
                .map(v -> VoteResponseDTO.from(v, redisTemplate))
                .collect(Collectors.toList());
    }

}
