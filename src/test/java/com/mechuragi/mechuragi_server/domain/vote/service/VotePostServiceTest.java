package com.mechuragi.mechuragi_server.domain.vote.service;

import com.mechuragi.mechuragi_server.domain.member.entity.Member;
import com.mechuragi.mechuragi_server.domain.notification.dto.VoteNotificationMessageDTO;
import com.mechuragi.mechuragi_server.domain.notification.dto.VoteNotificationType;
import com.mechuragi.mechuragi_server.domain.notification.event.VoteCompletedEvent;
import com.mechuragi.mechuragi_server.domain.vote.entity.VotePost;
import com.mechuragi.mechuragi_server.domain.vote.entity.VotePost.VoteStatus;
import com.mechuragi.mechuragi_server.domain.vote.repository.VotePostRepository;
import com.mechuragi.mechuragi_server.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VotePostService 단위 테스트")
class VotePostServiceTest {

    @Mock
    private VotePostRepository votePostRepository;

    @Mock
    private RedisTemplate<String, Object> redisPubSubTemplate;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private VotePostService votePostService;

    private Member testMember;
    private VotePost testVotePost;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .email("test@example.com")
                .nickname("테스터")
                .build();

        testVotePost = VotePost.builder()
                .author(testMember)
                .title("점심 메뉴 투표")
                .description("오늘 점심 메뉴를 투표해주세요")
                .deadline(LocalDateTime.now().plusHours(1))
                .allowMultipleChoice(false)
                .build();

        // ReflectionTestUtils를 사용하여 id 필드 설정
        ReflectionTestUtils.setField(testVotePost, "id", 1L);
    }

    @Test
    @DisplayName("투표 종료 처리 및 이벤트 발행 성공")
    void completeVoteAndNotify_Success() {
        // given
        Long voteId = 1L;
        when(votePostRepository.findById(voteId)).thenReturn(Optional.of(testVotePost));
        when(votePostRepository.save(any(VotePost.class))).thenReturn(testVotePost);

        // when
        votePostService.completeVoteAndNotify(voteId);

        // then
        assertEquals(VoteStatus.COMPLETED, testVotePost.getStatus());
        verify(votePostRepository).save(testVotePost);

        ArgumentCaptor<VoteCompletedEvent> eventCaptor = ArgumentCaptor.forClass(VoteCompletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        VoteCompletedEvent capturedEvent = eventCaptor.getValue();
        assertEquals(voteId, capturedEvent.getVoteId());
        assertEquals("점심 메뉴 투표", capturedEvent.getTitle());
    }

    @Test
    @DisplayName("존재하지 않는 투표 종료 시 예외 발생")
    void completeVoteAndNotify_VoteNotFound() {
        // given
        Long voteId = 999L;
        when(votePostRepository.findById(voteId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(BusinessException.class,
                () -> votePostService.completeVoteAndNotify(voteId));

        verify(votePostRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("투표 종료 10분 전 알림 발행 성공")
    void notifyVoteEndingSoon_Success() {
        // given
        Long voteId = 1L;
        String title = "점심 메뉴 투표";

        // when
        votePostService.notifyVoteEndingSoon(voteId, title);

        // then
        ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<VoteNotificationMessageDTO> messageCaptor =
                ArgumentCaptor.forClass(VoteNotificationMessageDTO.class);

        verify(redisPubSubTemplate).convertAndSend(
                channelCaptor.capture(),
                messageCaptor.capture()
        );

        assertEquals("vote:before10min", channelCaptor.getValue());

        VoteNotificationMessageDTO message = messageCaptor.getValue();
        assertEquals(voteId, message.getVoteId());
        assertEquals(title, message.getTitle());
        assertEquals(VoteNotificationType.ENDING_SOON, message.getType());
        assertNotNull(message.getTimestamp());
    }

    @Test
    @DisplayName("만료된 투표들 일괄 종료 처리")
    void completeExpiredVotes_Success() {
        // given
        VotePost expiredVote1 = VotePost.builder()
                .author(testMember)
                .title("만료된 투표 1")
                .deadline(LocalDateTime.now().minusHours(1))
                .allowMultipleChoice(false)
                .build();
        ReflectionTestUtils.setField(expiredVote1, "id", 2L);

        VotePost expiredVote2 = VotePost.builder()
                .author(testMember)
                .title("만료된 투표 2")
                .deadline(LocalDateTime.now().minusHours(2))
                .allowMultipleChoice(false)
                .build();
        ReflectionTestUtils.setField(expiredVote2, "id", 3L);

        when(votePostRepository.findExpiredActiveVotes(any(LocalDateTime.class)))
                .thenReturn(List.of(expiredVote1, expiredVote2));

        // when
        votePostService.completeExpiredVotes();

        // then
        assertEquals(VoteStatus.COMPLETED, expiredVote1.getStatus());
        assertEquals(VoteStatus.COMPLETED, expiredVote2.getStatus());
    }

    @Test
    @DisplayName("만료된 투표가 없을 때 정상 동작")
    void completeExpiredVotes_NoExpiredVotes() {
        // given
        when(votePostRepository.findExpiredActiveVotes(any(LocalDateTime.class)))
                .thenReturn(List.of());

        // when & then
        assertDoesNotThrow(() -> votePostService.completeExpiredVotes());
    }
}
