package com.taller.test.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis cache configuration for payment data.
 * Configures TTL and serialization for different cache types.
 * Only enabled when cache type is redis (disabled for tests).
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class CacheConfig {

    /**
     * Configure Redis cache manager with custom TTL settings.
     * Uses custom ObjectMapper for proper Java record serialization.
     * The ObjectMapper is created locally (not as a bean) to prevent Spring Boot
     * from using it for HTTP message conversion.
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Create Redis-specific ObjectMapper locally (not as a Spring bean)
        // This prevents Spring Boot's auto-configuration from picking it up for HTTP
        ObjectMapper redisMapper = new ObjectMapper();
        redisMapper.registerModule(new ParameterNamesModule());
        redisMapper.registerModule(new JavaTimeModule());

        // Enable default typing for Redis - required for records and polymorphic types
        // Use As.PROPERTY format to match GenericJackson2JsonRedisSerializer expectations
        // Use the ObjectMapper's default validator (LaissezFaireSubTypeValidator) for maximum compatibility
        // This won't affect HTTP because this ObjectMapper is not a Spring bean
        redisMapper.activateDefaultTyping(
            redisMapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer serializer =
            new GenericJackson2JsonRedisSerializer(redisMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(serializer)
                )
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("paymentsByStatus",
                    defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("paymentsSorted",
                    defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("paymentStatistics",
                    defaultConfig.entryTtl(Duration.ofMinutes(2)))
                .build();
    }
}
