package com.mechuragi.mechuragi_server.global.redis.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    /**
     * LocalDateTime 직렬화를 위한 공통 ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {

        // Redis 캐시 기본 설정: Key는 String, Value는 JSON으로 직렬화
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer(objectMapper)
                        )
                )
                .entryTtl(Duration.ofMinutes(5)); // 기본 TTL: 5분

        // 캐시별 커스텀 설정
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // popularMenus 캐시: 12분 TTL (10분 주기 스케줄러 + 2분 버퍼)
        cacheConfigurations.put("popularMenus",
                defaultCacheConfig.entryTtl(Duration.ofMinutes(12)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Redis Pub/Sub용 RedisTemplate
     * 투표 알림 메시지 발행에 사용
     */
    @Bean
    public RedisTemplate<String, Object> redisPubSubTemplate(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key: String 직렬화
        template.setKeySerializer(new StringRedisSerializer());

        // Value: JSON 직렬화 (Pub/Sub 메시지용)
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }
}
