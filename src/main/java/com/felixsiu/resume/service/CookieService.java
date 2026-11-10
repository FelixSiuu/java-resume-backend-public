package com.felixsiu.resume.service;

import com.felixsiu.resume.config.CookieProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CookieService {
		@Autowired
		CookieProperties cookieProperties;
		
		final String refreshPath = "/api/v1/auth";
		
		/**
		 * ttl: null for session cookie
		 *
		 * @param response HttpServletResponse
		 * @param rawAccess access token
		 * @param accessTtl access token ttl
		 * @param rawRefresh refresh token
		 * @param refreshTtl refresh token ttl
		 * @param rawXsrf xsrf token
		 * @param xsrfTtl xsrf token ttl
		 */
		public void writeTokenToCookie(
						HttpServletResponse response,
						String rawAccess, Duration accessTtl,
						String rawRefresh, Duration refreshTtl,
						String rawXsrf, Duration xsrfTtl
		) {
				String accessCookieName = cookieProperties.getNameAccess();
				String refreshCookieName = cookieProperties.getNameRefresh();
				String xsrfCookieName = cookieProperties.getNameXsrf();
				Boolean secure = cookieProperties.getSecure();
				String sameSite = cookieProperties.getSameSite();
				String path = cookieProperties.getPath();
				
				ResponseCookie accessCookie = ResponseCookie.from(accessCookieName, rawAccess)
								.httpOnly(true)
								.secure(secure)
								.sameSite(sameSite)
								.path(path)
								.maxAge(accessTtl)
								.build();
				
				ResponseCookie.ResponseCookieBuilder refreshCookieBuilder = ResponseCookie.from(refreshCookieName, rawRefresh)
								.httpOnly(true)
								.secure(secure)
								.sameSite(sameSite)
								.path(refreshPath);
				
				if (refreshTtl != null) {
						refreshCookieBuilder.maxAge(refreshTtl);
				}
				
				ResponseCookie refreshCookie = refreshCookieBuilder.build();
				
				ResponseCookie xsrfCookie = ResponseCookie.from(xsrfCookieName, rawXsrf)
								.httpOnly(false)
								.secure(secure)
								.sameSite(sameSite)
								.path(path)
								.maxAge(xsrfTtl)
								.build();
				
				response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
				response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
				response.addHeader(HttpHeaders.SET_COOKIE, xsrfCookie.toString());
		}
		
		public void clearTokenCookies(HttpServletResponse response) {
				String accessCookieName = cookieProperties.getNameAccess();
				String refreshCookieName = cookieProperties.getNameRefresh();
				String xsrfCookieName = cookieProperties.getNameXsrf();
				Boolean secure = cookieProperties.getSecure();
				String sameSite = cookieProperties.getSameSite();
				String path = cookieProperties.getPath();
				
				ResponseCookie accessCookie = ResponseCookie.from(accessCookieName, "")
								.httpOnly(true)
								.secure(secure)
								.sameSite(sameSite)
								.path(path)
								.maxAge(0)
								.build();
				
				ResponseCookie refreshCookie = ResponseCookie.from(refreshCookieName, "")
								.httpOnly(true)
								.secure(secure)
								.sameSite(sameSite)
								.path(refreshPath)
								.maxAge(0)
								.build();
				
				ResponseCookie xsrfCookie = ResponseCookie.from(xsrfCookieName, "")
								.httpOnly(false)
								.secure(secure)
								.sameSite(sameSite)
								.path(path)
								.maxAge(0)
								.build();
				
				response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
				response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
				response.addHeader(HttpHeaders.SET_COOKIE, xsrfCookie.toString());
		}
}

