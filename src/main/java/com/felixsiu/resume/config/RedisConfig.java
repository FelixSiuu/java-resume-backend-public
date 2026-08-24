package com.felixsiu.resume.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
		
		@Bean
		public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
				RedisTemplate<String, Object> template = new RedisTemplate<>();
				template.setConnectionFactory(factory);
				
				// 所有 Key 和 Value 都用 StringRedisSerializer
				StringRedisSerializer stringSerializer = new StringRedisSerializer();
				
				template.setKeySerializer(stringSerializer);
				template.setHashKeySerializer(stringSerializer);
				template.setValueSerializer(stringSerializer);
				template.setHashValueSerializer(stringSerializer);
				
				template.afterPropertiesSet();
				return template;
		}
}
