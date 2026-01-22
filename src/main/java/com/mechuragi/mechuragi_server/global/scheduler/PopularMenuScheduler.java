package com.mechuragi.mechuragi_server.global.scheduler;

import com.mechuragi.mechuragi_server.domain.vote.service.PopularMenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 홈화면 - 오늘 핫한 메뉴 자동 갱신 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PopularMenuScheduler {

    private final PopularMenuService popularMenuService;
    private final CacheManager cacheManager;

    // 매시 정각에 실행
    @Scheduled(cron = "0 0 * * * *")
    public void refreshPopularMenus() {
        try {
            // 1. 기존 캐시 무효화
            Cache cache = cacheManager.getCache("popularMenus");
            if (cache != null) {
                cache.evict("realtime");
                log.debug("오늘 핫한 메뉴 캐시 무효화 완료");
            }

            // 2. 새로 계산 (캐시에 자동 저장됨)
            popularMenuService.getPopularMenus();

            log.info("오늘 핫한 메뉴 자동 갱신 완료 (1시간 주기)");
        } catch (Exception e) {
            log.error("오늘 핫한 메뉴 자동 갱신 실패: {}", e.getMessage(), e);
        }
    }
}
