package com.wb.rules.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.kie.api.KieBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {
    @Value("${app.cache.rule-prefix}")
    private String rulePrefix;
    @Value("${app.cache.version-prefix}")
    private String versionPrefix;

    @Bean
    public Cache<String, KieBase> kieBaseCache() {
        return Caffeine.newBuilder()
                .maximumSize(100) // 根据实际情况调整，缓存最多100个KieBase
                .expireAfterAccess(2, TimeUnit.HOURS) // 2小时未访问则过期
                .recordStats() // 开启统计，用于监控命中率
                .build();
    }
}
