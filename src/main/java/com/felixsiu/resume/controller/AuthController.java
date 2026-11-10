package com.felixsiu.resume.controller;

import com.felixsiu.resume.model.dto.LoginDto;
import com.felixsiu.resume.model.dto.RegisterDto;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.model.vo.LoginVo;
import com.felixsiu.resume.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * 用戶接口
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "User", description = "用戶的相關接口")
public class AuthController {
		@Autowired
		AuthService authService;
		
		@PostMapping("/register")
		@Operation(summary = "註冊新的用戶")
		public BaseVo<Void> register(
						@Valid @RequestBody RegisterDto registerDto
		) {
				return authService.register(registerDto);
		}
		
		@PostMapping("/login")
		@Operation(summary = "登入")
		public BaseVo<LoginVo> login(
						@Valid @RequestBody LoginDto loginDto,
						@Parameter(hidden = true) HttpSession httpSession,
						@Parameter(hidden = true) HttpServletResponse response
		) {
				
				return authService.login(loginDto, httpSession, response);
		}
		
		@GetMapping("/captcha")
		@Operation(summary = "獲取圖形驗證碼")
		public BaseVo<String> getCaptcha(
						@Parameter(hidden = true) HttpSession httpSession
		) throws IOException {
				return authService.createCaptcha(httpSession);
		}
		
		@PostMapping("/logout")
		@Operation(summary = "登出")
		public BaseVo<Void> logout(
						@Parameter(hidden = true) HttpServletRequest request,
						@Parameter(hidden = true) HttpServletResponse response
		) {
				return authService.logout(request, response);
		}
		
		@PostMapping("/refresh")
		@Operation(summary = "refresh token")
		public BaseVo<LoginVo> refreshToken(
						@Parameter(hidden = true) HttpServletRequest request,
						@Parameter(hidden = true) HttpServletResponse response
		) {
				return authService.refreshToken(request, response);
		}
}
