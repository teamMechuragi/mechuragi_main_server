package com.mechuragi.mechuragi_server.global.scheduler;

import com.mechuragi.mechuragi_server.domain.vote.service.PopularMenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 실시간 인기 메뉴 자동 갱신 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PopularMenuScheduler {

    private final PopularMenuService popularMenuService;
    private final CacheManager cacheManager;

    /**
     * 10분마다 실시간 인기 메뉴 캐시를 자동 갱신
     * 홈화면에 표시되는 인기 메뉴가 항상 최신 상태로 유지됨
     */
    @Scheduled(fixedRate = 600000)  // 10분 (600,000ms)
    public void refreshPopularMenus() {
        try {
            // 1. 기존 캐시 무효화
            Cache cache = cacheManager.getCache("popularMenus");
            if (cache != null) {
                cache.evict("realtime");
                log.debug("인기 메뉴 캐시 무효화 완료");
            }

            // 2. 새로 계산 (캐시에 자동 저장됨)
            popularMenuService.getPopularMenus();

            log.info("실시간 인기 메뉴 자동 갱신 완료 (10분 주기)");
        } catch (Exception e) {
            log.error("실시간 인기 메뉴 자동 갱신 실패: {}", e.getMessage(), e);
        }
    }
}
