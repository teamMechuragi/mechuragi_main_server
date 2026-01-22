package com.mechuragi.mechuragi_server.global.redis.keyspace;

import com.mechuragi.mechuragi_server.domain.vote.entity.VotePost;
import com.mechuragi.mechuragi_server.domain.vote.repository.VotePostRepository;
import com.mechuragi.mechuragi_server.domain.vote.service.VotePostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Redis 키 만료 이벤트 리스너
 *
 * 키 패턴:
 * - vote:expire:{voteId} → 투표 종료 처리
 * - vote:soon:{voteId} → 10분 전 알림 발송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoteKeyExpiredListener implements MessageListener {

    private static final String VOTE_EXPIRE_PREFIX = "vote:expire:";
    private static final String VOTE_SOON_PREFIX = "vote:soon:";

    private final VotePostService votePostService;
    private final VotePostRepository votePostRepository;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = new String(message.getBody());

        log.debug("[VoteKeyExpiredListener] 키 만료 이벤트 수신: {}", expiredKey);

        try {
            if (expiredKey.startsWith(VOTE_EXPIRE_PREFIX)) {
                handleVoteExpiration(expiredKey);
            } else if (expiredKey.startsWith(VOTE_SOON_PREFIX)) {
                handleVoteEndingSoon(expiredKey);
            }
        } catch (Exception e) {
            log.error("[VoteKeyExpiredListener] 키 만료 이벤트 처리 실패: key={}", expiredKey, e);
        }
    }

    /**
     * 투표 종료 처리
     */
    private void handleVoteExpiration(String expiredKey) {
        Long voteId = extractVoteId(expiredKey, VOTE_EXPIRE_PREFIX);
        if (voteId == null) {
            log.warn("[VoteKeyExpiredListener] 잘못된 키 형식: {}", expiredKey);
            return;
        }

        log.info("[VoteKeyExpiredListener] 투표 종료 처리 시작: voteId={}", voteId);

        Optional<VotePost> votePostOpt = votePostRepository.findById(voteId);
        if (votePostOpt.isEmpty()) {
            log.warn("[VoteKeyExpiredListener] 투표를 찾을 수 없음: voteId={}", voteId);
            return;
        }

        VotePost votePost = votePostOpt.get();
        if (votePost.getStatus() == VotePost.VoteStatus.COMPLETED) {
            log.debug("[VoteKeyExpiredListener] 이미 종료된 투표: voteId={}", voteId);
            return;
        }

        votePostService.completeVoteAndNotify(voteId);
        log.info("[VoteKeyExpiredListener] 투표 종료 처리 완료: voteId={}", voteId);
    }

    /**
     * 투표 종료 10분 전 알림 발송
     */
    private void handleVoteEndingSoon(String expiredKey) {
        Long voteId = extractVoteId(expiredKey, VOTE_SOON_PREFIX);
        if (voteId == null) {
            log.warn("[VoteKeyExpiredListener] 잘못된 키 형식: {}", expiredKey);
            return;
        }

        log.info("[VoteKeyExpiredListener] 투표 종료 10분 전 알림 처리 시작: voteId={}", voteId);

        Optional<VotePost> votePostOpt = votePostRepository.findById(voteId);
        if (votePostOpt.isEmpty()) {
            log.warn("[VoteKeyExpiredListener] 투표를 찾을 수 없음: voteId={}", voteId);
            return;
        }

        VotePost votePost = votePostOpt.get();
        if (votePost.getStatus() == VotePost.VoteStatus.COMPLETED) {
            log.debug("[VoteKeyExpiredListener] 이미 종료된 투표 - 10분 전 알림 건너뜀: voteId={}", voteId);
            return;
        }

        if (votePost.getNotified10MinBefore()) {
            log.debug("[VoteKeyExpiredListener] 이미 10분 전 알림 발송됨: voteId={}", voteId);
            return;
        }

        votePostService.notifyVoteEndingSoon(voteId, votePost.getTitle());
        log.info("[VoteKeyExpiredListener] 투표 종료 10분 전 알림 완료: voteId={}", voteId);
    }

    /**
     * 키에서 voteId 추출
     */
    private Long extractVoteId(String key, String prefix) {
        try {
            String idStr = key.substring(prefix.length());
            return Long.parseLong(idStr);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            log.error("[VoteKeyExpiredListener] voteId 추출 실패: key={}", key, e);
            return null;
        }
    }
}
