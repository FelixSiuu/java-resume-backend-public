package com.felixsiu.resume.config;

import com.felixsiu.resume.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
		
		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
				
				// 無條件放行所有瀏覽器的 CORS 預檢探路請求
				if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
						return true;
				}
				
				// 讀取標準的 Authorization 請求頭
				String bearerToken = request.getHeader("Authorization");
				
				// 判斷：是否為空、長度是否足夠、是否以 "Bearer " 開頭
				if (bearerToken == null || bearerToken.isEmpty() || !bearerToken.startsWith("Bearer ")) {
						response.setStatus(401);
						return false;
				}
				
				// 提取出真正的純 JWT Token
				String actualToken = bearerToken.substring(7);
				
				if (!jwtUtil.verifyToken(actualToken)) {
						response.setStatus(401);
						return false;
				}
				
				// 在攔截器裡只解析一次
				String userId = jwtUtil.getUserIdFromToken(actualToken);
				// 存入 request 屬性
				request.setAttribute("userId", userId);
				
				return true;
		}
}
