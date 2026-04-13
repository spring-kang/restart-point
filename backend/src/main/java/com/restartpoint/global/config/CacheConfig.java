package com.restartpoint.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PROJECT_REVIEW_ANALYSIS_CACHE = "projectReviewAnalysis";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(PROJECT_REVIEW_ANALYSIS_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(10)));
        return cacheManager;
    }
}
