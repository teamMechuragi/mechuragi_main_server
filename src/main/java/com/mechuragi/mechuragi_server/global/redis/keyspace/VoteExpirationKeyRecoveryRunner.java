package com.mechuragi.mechuragi_server.global.redis.keyspace;

import com.mechuragi.mechuragi_server.domain.vote.entity.VotePost;
import com.mechuragi.mechuragi_server.domain.vote.repository.VotePostRepository;
import com.mechuragi.mechuragi_server.domain.vote.service.VotePostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * 서버 시작 시 활성 투표의 Redis TTL 키 복구
 *
 * 서버 재시작 시 Redis 키가 유실될 수 있으므로,
 * 애플리케이션 시작 시점에 활성 투표들의 TTL 키를 재등록합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoteExpirationKeyRecoveryRunner implements ApplicationRunner {

    private final VotePostRepository votePostRepository;
    private final VotePostService votePostService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[VoteExpirationKeyRecoveryRunner] 활성 투표 Redis TTL 키 복구 시작");

        try {
            Instant now = Instant.now();
            List<VotePost> activeVotes = votePostRepository.findAllActiveVotes(now);

            log.info("[VoteExpirationKeyRecoveryRunner] 활성 투표 수: {}", activeVotes.size());

            int successCount = 0;
            int failCount = 0;

            for (VotePost votePost : activeVotes) {
                try {
                    votePostService.registerVoteExpirationKeys(votePost);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("[VoteExpirationKeyRecoveryRunner] TTL 키 복구 실패: voteId={}",
                            votePost.getId(), e);
                }
            }

            log.info("[VoteExpirationKeyRecoveryRunner] Redis TTL 키 복구 완료: 성공={}, 실패={}",
                    successCount, failCount);
        } catch (Exception e) {
            log.error("[VoteExpirationKeyRecoveryRunner] Redis TTL 키 복구 중 오류 발생", e);
        }
    }
}
