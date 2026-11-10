package com.felixsiu.resume.config;

import com.felixsiu.resume.utils.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 攔截器
 * 會在過濾器之後調用
 * preHandle: 發生在controller的方法調用之前，
 * postHandle: 發生在controller的方法調用後，但渲染視圖之前
 * afterCompletion: 整個請求完成後調用，包括視圖也渲染完成
 * 重要：需要定義Spring MVC全局配置，添加攔截器
 */

@Component
public class AuthInterceptor implements HandlerInterceptor {
		@Autowired
		JwtUtil jwtUtil;
		@Autowired
		CookieProperties cookieProperties;
		
		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
				
				// 無條件放行所有瀏覽器的 CORS 預檢探路請求
				if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
						return true;
				}
				
				String cookieName = cookieProperties.getNameAccess();
				String token = null;
				
				if (request.getCookies() != null) {
						for (Cookie c : request.getCookies()) {
								if (cookieName.equals(c.getName())) {
										token = c.getValue();
										break;
								}
						}
				}
				
				if (token == null || token.isBlank()) {
						String bearer = request.getHeader("Authorization");
						if(bearer != null && bearer.startsWith("Bearer ")) {
								token = bearer.substring(7);
						}
				}
				
				if(token == null || !jwtUtil.verifyToken(token)){
						response.setStatus(401);
						return false;
				}
				
				String userId = jwtUtil.getUserIdFromToken(token);
				// 存入 request 屬性
				request.setAttribute("userId", userId);
				
				return true;
		}
}
