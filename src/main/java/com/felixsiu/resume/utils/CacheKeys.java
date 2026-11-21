package com.felixsiu.resume.utils;

import java.util.List;

public class CacheKeys {
		
		private static final String PREFIX = "user:";
		
		public static String about(String userId) {
				return PREFIX + "about:" + userId;
		}
		
		public static String education(String userId) {
				return PREFIX + "education:" + userId;
		}
		
		public static String skills(String userId) {
				return PREFIX + "skills:" + userId;
		}
		
		public static String experience(String userId) {
				return PREFIX + "exp:" + userId;
		}
		
		public static String contact(String userId) {
				return PREFIX + "contact:" + userId;
		}
		
		public static String projects(String userId) {
				return PREFIX + "projects:" + userId;
		}
		
		public static String forgotPassword(String email) {
				return PREFIX + "forgot-password:" + email;
		}
		
		public static String deleteToken(String userId) {
				return PREFIX + "delete-token:" + userId;
		}
		
		/**
		 * 獲取某个用户的所有缓存 key
		 */
		public static List<String> allUserKeys(String userId) {
				return List.of(
								about(userId),
								education(userId),
								skills(userId),
								experience(userId),
								contact(userId),
								projects(userId),
								deleteToken(userId)
				);
		}
}