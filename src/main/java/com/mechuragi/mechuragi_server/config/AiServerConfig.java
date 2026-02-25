package com.mechuragi.mechuragi_server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AiServerConfig {

    @Value("${ai-service.url}")
    private String aiServiceUrl;

    @Bean
    public RestClient aiServerRestClient() {
        return RestClient.builder()
                .baseUrl(aiServiceUrl)
                .build();
    }
}
