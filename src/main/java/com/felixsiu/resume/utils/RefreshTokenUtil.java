package com.felixsiu.resume.utils;

import java.security.SecureRandom;
import java.util.Base64;

public final class RefreshTokenUtil {
		private static final SecureRandom SECURE_RANDOM = new SecureRandom();
		
		private RefreshTokenUtil() {
		}
		
		public static String generate() {
				byte[] buf = new byte[32];
				SECURE_RANDOM.nextBytes(buf);
				return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
		}
		
}
