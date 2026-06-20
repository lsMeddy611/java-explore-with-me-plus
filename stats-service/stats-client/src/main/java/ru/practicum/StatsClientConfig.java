package ru.practicum;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class StatsClientConfig {

    @Value("${stats.server.url}")
    private String statsServerUrl;

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl(statsServerUrl)
                .build();
    }
}