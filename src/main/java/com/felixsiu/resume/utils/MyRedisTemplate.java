package com.felixsiu.resume.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class MyRedisTemplate {
		@Autowired
		private RedisTemplate<String, Object> redisTemplate;
		
		@Autowired
		private ObjectMapper objectMapper;
		
		/**
		 * 从缓存获取数据（支持所有泛型类型，如 List<T>、Map<K,V> 等）
		 *
		 * @param key     缓存 Key
		 * @param typeRef 类型引用，如 new TypeReference<List<About>>() {}
		 * @param <T>     目标类型
		 * @return 缓存数据，不存在时返回 null
		 */
		public <T> T get(String key, TypeReference<T> typeRef) {
				String jsonString = (String) redisTemplate.opsForValue().get(key);
				if (jsonString == null) {
						return null;
				}
				try {
						return objectMapper.readValue(jsonString, typeRef);
				} catch (JsonProcessingException e) {
						System.err.println("❌ 反序列化失败 [key=" + key + "]: " + e.getMessage());
						return null;
				}
		}
		
		/**
		 * 存入缓存（自动序列化为 JSON 字符串）
		 *
		 * @param key     缓存 Key
		 * @param value   缓存数据（自动序列化为 JSON）
		 * @param timeout 过期时间
		 * @param unit    时间单位
		 */
		public void set(String key, Object value, long timeout, TimeUnit unit) {
				try {
						String jsonString = objectMapper.writeValueAsString(value);
						redisTemplate.opsForValue().set(key, jsonString, timeout, unit);
				} catch (JsonProcessingException e) {
						System.err.println("❌ 序列化失败 [key=" + key + "]: " + e.getMessage());
				}
		}
		
		/**
		 * 删除缓存
		 */
		public Boolean delete(String key) {
				Boolean deleted = redisTemplate.delete(key);
				if (deleted) {
						System.out.println("🗑️ 已清除缓存: " + key);
				}
				return deleted;
		}
		
		/**
		 * 判断缓存是否存在
		 */
		public Boolean hasKey(String key) {
				return redisTemplate.hasKey(key);
		}
}
