package com.mechuragi.mechuragi_server.global.redis.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mechuragi.mechuragi_server.domain.notification.dto.VoteNotificationMessageDTO;
import com.mechuragi.mechuragi_server.domain.notification.dto.VoteNotificationType;
import com.mechuragi.mechuragi_server.domain.notification.metrics.VoteNotificationMetrics;
import com.mechuragi.mechuragi_server.domain.notification.service.VoteNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VoteNotificationSubscriber 단위 테스트")
class VoteNotificationSubscriberTest {

    @Mock
    private VoteNotificationService voteNotificationService;

    @Mock
    private VoteNotificationMetrics metrics;

    private ObjectMapper objectMapper;
    private VoteNotificationSubscriber subscriber;

    @BeforeEach
    void setUp() {
        // ObjectMapper 설정
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // LocalDateTime을 ISO-8601 형식으로 직렬화/역직렬화
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Mock 객체들을 주입하여 Subscriber 생성
        subscriber = new VoteNotificationSubscriber(voteNotificationService, objectMapper, metrics);
    }

    @Test
    @DisplayName("Redis 메시지 수신 및 처리 성공 - 투표 종료")
    void onMessage_VoteCompleted_Success() throws Exception {
        // given
        VoteNotificationMessageDTO message = VoteNotificationMessageDTO.builder()
                .voteId(1L)
                .title("점심 메뉴 투표")
                .type(VoteNotificationType.COMPLETED)
                .timestamp(LocalDateTime.now())
                .build();

        byte[] body = objectMapper.writeValueAsBytes(message);
        // DefaultMessage(channel, body) 순서로 생성
        Message redisMessage = new DefaultMessage("vote:end".getBytes(), body);

        // when
        subscriber.onMessage(redisMessage, null);

        // then
        ArgumentCaptor<VoteNotificationMessageDTO> captor =
                ArgumentCaptor.forClass(VoteNotificationMessageDTO.class);
        verify(voteNotificationService).sendNotification(captor.capture());

        VoteNotificationMessageDTO captured = captor.getValue();
        assertEquals(1L, captured.getVoteId());
        assertEquals("점심 메뉴 투표", captured.getTitle());
        assertEquals(VoteNotificationType.COMPLETED, captured.getType());
    }

    @Test
    @DisplayName("Redis 메시지 수신 및 처리 성공 - 투표 종료 10분 전")
    void onMessage_VoteEndingSoon_Success() throws Exception {
        // given
        VoteNotificationMessageDTO message = VoteNotificationMessageDTO.builder()
                .voteId(2L)
                .title("저녁 메뉴 투표")
                .type(VoteNotificationType.ENDING_SOON)
                .timestamp(LocalDateTime.now())
                .build();

        byte[] body = objectMapper.writeValueAsBytes(message);
        // DefaultMessage(channel, body) 순서로 생성
        Message redisMessage = new DefaultMessage("vote:before10min".getBytes(), body);

        // when
        subscriber.onMessage(redisMessage, null);

        // then
        ArgumentCaptor<VoteNotificationMessageDTO> captor =
                ArgumentCaptor.forClass(VoteNotificationMessageDTO.class);
        verify(voteNotificationService, times(1)).sendNotification(captor.capture());

        VoteNotificationMessageDTO captured = captor.getValue();
        assertEquals(2L, captured.getVoteId());
        assertEquals("저녁 메뉴 투표", captured.getTitle());
        assertEquals(VoteNotificationType.ENDING_SOON, captured.getType());
    }

    @Test
    @DisplayName("잘못된 메시지 형식 처리 시 예외 처리")
    void onMessage_InvalidMessageFormat() {
        // given
        byte[] invalidBody = "invalid json".getBytes();
        // DefaultMessage(channel, body) 순서로 생성
        Message redisMessage = new DefaultMessage("vote:end".getBytes(), invalidBody);

        // when & then
        assertDoesNotThrow(() -> subscriber.onMessage(redisMessage, null));
        verify(voteNotificationService, never()).sendNotification(any());
    }

    @Test
    @DisplayName("알림 서비스 호출 실패 시 예외 처리")
    void onMessage_ServiceFailure() throws Exception {
        // given
        VoteNotificationMessageDTO message = VoteNotificationMessageDTO.builder()
                .voteId(1L)
                .title("테스트 투표")
                .type(VoteNotificationType.COMPLETED)
                .timestamp(LocalDateTime.now())
                .build();

        byte[] body = objectMapper.writeValueAsBytes(message);
        // DefaultMessage(channel, body) 순서로 생성
        Message redisMessage = new DefaultMessage("vote:end".getBytes(), body);

        doThrow(new RuntimeException("STOMP 전송 실패"))
                .when(voteNotificationService).sendNotification(any());

        // when & then
        assertDoesNotThrow(() -> subscriber.onMessage(redisMessage, null));
        verify(voteNotificationService).sendNotification(any());
    }

    @Test
    @DisplayName("여러 메시지 연속 처리")
    void onMessage_MultipleMessages() throws Exception {
        // given & when
        for (int i = 1; i <= 5; i++) {
            VoteNotificationMessageDTO message = VoteNotificationMessageDTO.builder()
                    .voteId((long) i)
                    .title("투표 " + i)
                    .type(VoteNotificationType.COMPLETED)
                    .timestamp(LocalDateTime.now())
                    .build();

            byte[] body = objectMapper.writeValueAsBytes(message);
            // DefaultMessage(channel, body) 순서로 생성
            Message redisMessage = new DefaultMessage("vote:end".getBytes(), body);

            subscriber.onMessage(redisMessage, null);
        }

        // then
        verify(voteNotificationService, times(5)).sendNotification(any());
    }
}
