package com.felixsiu.resume.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class Sha256Hex {
		public static String toHexString(String raw) throws NoSuchAlgorithmException {
				byte[] dig = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
				return HexFormat.of().formatHex(dig);
		}
}
