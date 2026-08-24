package com.felixsiu.resume.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 過濾器
 *
 * @Component 註解 + implements Filter接口，spring boot就會識別這個類作為接口的過濾器
 * @Order() 註解，代表過濾器的執行順序，數字越小優先級越高
 * @WebFilter(urlPatterns = "") 和 @Component 一樣作為定義控制器的註解，兩者只能選其中一個，如果選擇了 @WebFilter 註解，代表沒有使用SprintBoot的註解，就需要在啟動類加上 @ServletComponentScan 註解，urlPatterns 為過濾器的作用範圍
 * 可以將ServletRequest強制轉化爲HttpServletRequest類型，使用更多api
 */
@Component
//@WebFilter(urlPatterns = "/*")
@Order(1)
public class MyFilter implements Filter {
		@Override
		public void init(FilterConfig filterConfig) {
				System.out.println("過濾器初始化");
		}
		
		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
				HttpServletRequest req = (HttpServletRequest) request;
				String method = req.getMethod();
				String path = req.getRequestURI();
				String remoteAddr = req.getRemoteAddr();
				
				// /favicon.ico 不輸出語句
				if ("/favicon.ico".equals(path)) {
						chain.doFilter(request, response);
						return;
				}
				
				System.out.println("過濾器過濾: " + "[" + method + "] " + path + " - 來自: " + remoteAddr);
				
				// 繼續傳遞請求
				chain.doFilter(request, response);
		}
		
		@Override
		public void destroy() {
				System.out.println("過濾器銷毀");
		}
}