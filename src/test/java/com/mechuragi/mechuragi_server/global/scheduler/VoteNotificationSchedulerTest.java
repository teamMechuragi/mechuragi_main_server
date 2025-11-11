package com.mechuragi.mechuragi_server.global.scheduler;

import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.vote.entity.VotePost;
import com.mechuragi.mechuragi_server.domain.vote.repository.VotePostRepository;
import com.mechuragi.mechuragi_server.domain.vote.service.VotePostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VoteNotificationScheduler 단위 테스트")
class VoteNotificationSchedulerTest {

    @Mock
    private VotePostRepository votePostRepository;

    @Mock
    private VotePostService votePostService;

    @InjectMocks
    private VoteNotificationScheduler scheduler;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .email("test@example.com")
                .nickname("테스터")
                .build();
    }

    @Test
    @DisplayName("투표 종료 10분 전 알림 스케줄러 - 알림 발송 대상 있음")
    void notifyVotesEndingSoon_WithEndingSoonVotes() {
        // given
        VotePost endingSoonVote1 = createVotePost("투표 1", LocalDateTime.now().plusMinutes(10));
        VotePost endingSoonVote2 = createVotePost("투표 2", LocalDateTime.now().plusMinutes(10).plusSeconds(30));

        when(votePostRepository.findVotesEndingInTenMinutes(any(), any()))
                .thenReturn(List.of(endingSoonVote1, endingSoonVote2));

        // when
        scheduler.notifyVotesEndingSoon();

        // then
        verify(votePostRepository).findVotesEndingInTenMinutes(any(), any());
        verify(votePostService).notifyVoteEndingSoon(endingSoonVote1.getId(), endingSoonVote1.getTitle());
        verify(votePostService).notifyVoteEndingSoon(endingSoonVote2.getId(), endingSoonVote2.getTitle());
        verify(votePostService, times(2)).notifyVoteEndingSoon(any(), any());
    }

    @Test
    @DisplayName("투표 종료 10분 전 알림 스케줄러 - 알림 발송 대상 없음")
    void notifyVotesEndingSoon_NoEndingSoonVotes() {
        // given
        when(votePostRepository.findVotesEndingInTenMinutes(any(), any()))
                .thenReturn(List.of());

        // when
        scheduler.notifyVotesEndingSoon();

        // then
        verify(votePostRepository).findVotesEndingInTenMinutes(any(), any());
        verify(votePostService, never()).notifyVoteEndingSoon(any(), any());
    }

    @Test
    @DisplayName("만료된 투표 종료 처리 스케줄러 - 만료된 투표 있음")
    void completeExpiredVotes_WithExpiredVotes() {
        // given
        VotePost expiredVote1 = createVotePost("만료 투표 1", LocalDateTime.now().minusMinutes(10));
        VotePost expiredVote2 = createVotePost("만료 투표 2", LocalDateTime.now().minusHours(1));

        when(votePostRepository.findExpiredActiveVotes(any()))
                .thenReturn(List.of(expiredVote1, expiredVote2));

        // when
        scheduler.completeExpiredVotes();

        // then
        verify(votePostRepository).findExpiredActiveVotes(any());
        verify(votePostService).completeVoteAndNotify(expiredVote1.getId());
        verify(votePostService).completeVoteAndNotify(expiredVote2.getId());
        verify(votePostService, times(2)).completeVoteAndNotify(any());
    }

    @Test
    @DisplayName("만료된 투표 종료 처리 스케줄러 - 만료된 투표 없음")
    void completeExpiredVotes_NoExpiredVotes() {
        // given
        when(votePostRepository.findExpiredActiveVotes(any()))
                .thenReturn(List.of());

        // when
        scheduler.completeExpiredVotes();

        // then
        verify(votePostRepository).findExpiredActiveVotes(any());
        verify(votePostService, never()).completeVoteAndNotify(any());
    }

    @Test
    @DisplayName("스케줄러 실행 중 예외 발생 시에도 계속 실행")
    void scheduler_ContinuesOnException() {
        // given
        VotePost vote1 = createVotePost("투표 1", LocalDateTime.now().minusMinutes(10));
        VotePost vote2 = createVotePost("투표 2", LocalDateTime.now().minusMinutes(20));

        when(votePostRepository.findExpiredActiveVotes(any()))
                .thenReturn(List.of(vote1, vote2));

        doThrow(new RuntimeException("알림 발송 실패"))
                .when(votePostService).completeVoteAndNotify(vote1.getId());

        // when & then - 예외가 발생해도 스케줄러는 계속 실행됨
        scheduler.completeExpiredVotes();

        verify(votePostService).completeVoteAndNotify(vote1.getId());
        verify(votePostService).completeVoteAndNotify(vote2.getId());
    }

    @Test
    @DisplayName("대량의 투표 처리 - 성능 테스트")
    void scheduler_HandlesManyVotes() {
        // given
        List<VotePost> manyVotes = List.of(
                createVotePost("투표 1", LocalDateTime.now().minusMinutes(1)),
                createVotePost("투표 2", LocalDateTime.now().minusMinutes(2)),
                createVotePost("투표 3", LocalDateTime.now().minusMinutes(3)),
                createVotePost("투표 4", LocalDateTime.now().minusMinutes(4)),
                createVotePost("투표 5", LocalDateTime.now().minusMinutes(5))
        );

        when(votePostRepository.findExpiredActiveVotes(any()))
                .thenReturn(manyVotes);

        // when
        scheduler.completeExpiredVotes();

        // then
        verify(votePostService, times(5)).completeVoteAndNotify(any());
    }

    private VotePost createVotePost(String title, LocalDateTime deadline) {
        return VotePost.builder()
                .author(testMember)
                .title(title)
                .description("투표 설명")
                .deadline(deadline)
                .allowMultipleChoice(false)
                .build();
    }
}
