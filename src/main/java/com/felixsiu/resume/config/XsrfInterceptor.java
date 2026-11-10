package com.felixsiu.resume.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class XsrfInterceptor implements HandlerInterceptor {
		@Autowired
		CookieProperties cookieProperties;
		
		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
				String method = request.getMethod();
				if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
						return true;
				}
				
				String xsrfTokenFromHeader = request.getHeader("Xsrf-Token");
				String xsrfTokenFromCookie = null;
				String xsrfCookieName = cookieProperties.getNameXsrf();
				
				if (request.getCookies() != null) {
						for (Cookie c : request.getCookies()) {
								if (xsrfCookieName.equals(c.getName())) {
										xsrfTokenFromCookie = c.getValue();
										break;
								}
						}
				}
				
				if (!StringUtils.hasText(xsrfTokenFromHeader) || !StringUtils.hasText(xsrfTokenFromCookie)) {
						response.setStatus(403);
						return false;
				}
				
				if (!MessageDigest.isEqual(
								xsrfTokenFromHeader.getBytes(StandardCharsets.UTF_8),
								xsrfTokenFromCookie.getBytes(StandardCharsets.UTF_8))) {
						response.setStatus(403);
						return false;
				}
				
				return true;
		}
}
