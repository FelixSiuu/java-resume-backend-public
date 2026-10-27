package com.felixsiu.resume.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
		
		@Autowired
		private AuthInterceptor authInterceptor;
		
		@Autowired
		CorsProperties corsProperties;
		
		@Override
		
		public void addCorsMappings(CorsRegistry registry) {
				List<String> origins = corsProperties.getAllowedOrigins();
				
				registry
								.addMapping("/**") // 攔截所有後端接口路徑
								.allowedOrigins(origins.toArray(new String[0])) // 同時放行本地開發環境與線上的 Vercel 生產環境網址
								.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 放行這些 HTTP 方法
								.allowedHeaders("*")
								.allowCredentials(true) // 允許攜帶 Cookie 等憑證
								.maxAge(3600); // 讓瀏覽器快取這次的 CORS 結果 1 小時，不用每次都發 OPTIONS 請求，優化前端效能
		}
		
		@Override
		public void addInterceptors(InterceptorRegistry registry) {
				registry
								.addInterceptor(authInterceptor)
								.addPathPatterns("/api/v1/**")
								.excludePathPatterns("/api/v1/register", "/api/v1/login", "/api/v1/getCaptcha", "/api/v1/public/**");
		}
}
