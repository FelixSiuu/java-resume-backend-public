package com.felixsiu.resume.utils;

import java.security.SecureRandom;
import java.util.Base64;

public class EmailTokenUtil {
		private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		private static final SecureRandom SECURE_RANDOM = new SecureRandom();
		
		private EmailTokenUtil() {
		}
		
		public static String generate() {
				byte[] buf = new byte[32];
				SECURE_RANDOM.nextBytes(buf);
				return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
		}
		
		public static String generateShortCode(int length) {
				StringBuilder sb = new StringBuilder(length);
				for (int i = 0; i < length; i++) {
						int index = SECURE_RANDOM.nextInt(CHARACTERS.length());
						sb.append(CHARACTERS.charAt(index));
				}
				return sb.toString();
		}
}
