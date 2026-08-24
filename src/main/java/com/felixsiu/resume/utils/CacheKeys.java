package com.felixsiu.resume.utils;

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
		
		/**
		 * 批量删除某个用户的所有缓存
		 */
		public static String[] allUserKeys(String userId) {
				return new String[]{
								about(userId),
								education(userId),
								skills(userId),
								experience(userId),
								contact(userId)
				};
		}
}