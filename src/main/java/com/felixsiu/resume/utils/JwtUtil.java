package com.felixsiu.resume.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;


/**
 * 使用Keys.hmacShaKeyFor，即代表使用HS256加密算法獲取簽名
 * 使用Jwts.builder()獲取JwtBuilder對象，生成了head  {"alg":"HS256", "typ":"JWT}，也可以使用setHeaderParam()添加自定義屬性
 * payload: 使用 .subject().expiration()
 * 加上簽名：.signWith(Key)
 * .compact() 最終生成 jwt
 */

@Component
public class JwtUtil {
		
		private final SecretKey secretKey;
		
		public JwtUtil(@Value("${jwt.secret}") String secretString) {
				if (secretString == null || secretString.length() < 32) {
						throw new IllegalArgumentException("JWT secret must be at least 32 characters long");
				}
				
				this.secretKey = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
		}
		
		/**
		 * 生成 Token（有效期 24 小時）
		 */
		public String generateToken(String id) {
				return Jwts.builder()
								.subject(id)
								.issuedAt(new Date())
								.expiration(new Date(System.currentTimeMillis() + 24L * 60 * 60 * 1000))  // 注意要加 L
								.signWith(secretKey)
								.compact();
		}
		
		/**
		 * 解析 Token（內部使用）
		 */
		private Claims parseToken(String token) {
				return Jwts.parser()
								.verifyWith(secretKey)
								.build()
								.parseSignedClaims(token)
								.getPayload();
		}
		
		/**
		 * 驗證 Token 是否有效（未過期且簽名正確）
		 */
		public boolean verifyToken(String token) {
				try {
						Claims claims = parseToken(token);
						Date expiration = claims.getExpiration();
						// ✅ 未過期才返回 true
						return expiration.after(new Date());
				} catch (ExpiredJwtException e) {
						// Token 已過期
						System.out.println("Token 已過期: " + e.getMessage());
						return false;
				} catch (SignatureException e) {
						// 簽名驗證失敗
						System.out.println("Token 簽名無效: " + e.getMessage());
						return false;
				} catch (Exception e) {
						// 其他異常
						System.out.println("Token 驗證失敗: " + e.getMessage());
						return false;
				}
		}
		
		/**
		 * 檢查 Token 是否已過期
		 */
		public boolean isTokenExpired(String token) {
				try {
						Claims claims = parseToken(token);
						return claims.getExpiration().before(new Date());
				} catch (ExpiredJwtException e) {
						return true;
				} catch (Exception e) {
						return true;
				}
		}
		
		/**
		 * 從 Token 中獲取用戶 ID（即 subject）
		 */
		public String getUserIdFromToken(String token) {
				try {
						Claims claims = parseToken(token);
						return claims.getSubject();
				} catch (Exception e) {
						return null;
				}
		}
		
}