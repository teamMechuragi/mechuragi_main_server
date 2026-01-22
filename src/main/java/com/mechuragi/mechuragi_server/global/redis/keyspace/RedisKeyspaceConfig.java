package com.mechuragi.mechuragi_server.global.redis.keyspace;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis Keyspace Notifications 설정
 *
 * Redis 서버에서 Keyspace Notifications 활성화 필요:
 * redis-cli CONFIG SET notify-keyspace-events Ex
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisKeyspaceConfig {

    private final VoteKeyExpiredListener voteKeyExpiredListener;

    @Bean
    public RedisMessageListenerContainer keyspaceListenerContainer(
            RedisConnectionFactory connectionFactory) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // __keyevent@*__:expired 채널 구독 (모든 DB의 키 만료 이벤트)
        container.addMessageListener(
                voteKeyExpiredListener,
                new PatternTopic("__keyevent@*__:expired")
        );

        log.info("[RedisKeyspaceConfig] Redis Keyspace Notifications 리스너 등록 완료");

        return container;
    }
}
