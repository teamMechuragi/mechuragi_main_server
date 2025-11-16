package com.mechuragi.mechuragi_server.global.redis.subscriber;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
@RequiredArgsConstructor
public class RedisSubscriberConfig {
    private final VoteNotificationSubscriber voteNotificationSubscriber;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 투표 종료 채널 구독
        container.addMessageListener(
                new MessageListenerAdapter(voteNotificationSubscriber),
                new PatternTopic("vote:end")
        );

        // 투표 종료 10분 전 채널 구독
        container.addMessageListener(
                new MessageListenerAdapter(voteNotificationSubscriber),
                new PatternTopic("vote:before10min")
        );

        return container;
    }
}
