package com.felixsiu.resume.utils;

import java.security.SecureRandom;
import java.util.Base64;

public final class XsrfTokenUtil {
		private static final SecureRandom SECURE_RANDOM = new SecureRandom();
		
		private XsrfTokenUtil() {
		}
		
		public static String generate() {
				byte[] buf = new byte[32];
				SECURE_RANDOM.nextBytes(buf);
				return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
		}
		
}
