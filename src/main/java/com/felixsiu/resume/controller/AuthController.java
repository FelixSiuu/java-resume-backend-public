package com.felixsiu.resume.controller;

import com.felixsiu.resume.annotation.StandardApiResponses;
import com.felixsiu.resume.model.dto.*;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.model.vo.CoolDownVo;
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

/**
 * 校驗接口
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "校驗的相關接口")
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
		) {
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
		
		@PostMapping("/verify-email")
		@Operation(summary = "驗證電子郵箱")
		public BaseVo<Void> verifyEmail(
						@Valid @RequestBody ValidateEmailTokenDto validateEmailTokenDto
		) {
				return authService.verifyEmail(validateEmailTokenDto);
		}
		
		@PostMapping("/forgot-password")
		@Operation(summary = "忘記密碼 請求發信")
		public BaseVo<CoolDownVo> forgotPassword(
						@Parameter(hidden = true) HttpSession httpSession,
						@Valid @RequestBody ForgotPasswordDto forgotPasswordDto
		) {
				return authService.forgotPassword(httpSession, forgotPasswordDto);
		}
		
		@PostMapping("/validate-reset-token")
		@Operation(summary = "重置密碼token校驗")
		public BaseVo<Void> validateResetToken(
						@Valid @RequestBody ValidateEmailTokenDto validateEmailTokenDto
		) {
				return authService.validateResetToken(validateEmailTokenDto);
		}
		
		@PostMapping("/reset-password")
		@Operation(summary = "重置密碼")
		public BaseVo<Void> resetPassword(
						@Valid @RequestBody ResetPasswordDto resetPasswordDto,
						@Parameter(hidden = true) HttpServletResponse response
		) {
				return authService.resetPassword(resetPasswordDto, response);
		}
		
		@GetMapping("/delete-token")
		@Operation(summary = "獲取刪除帳戶的驗證碼")
		@StandardApiResponses
		public BaseVo<CoolDownVo> getDeleteToken(
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId
		) {
				return authService.getDeleteToken(userId);
		}
}
