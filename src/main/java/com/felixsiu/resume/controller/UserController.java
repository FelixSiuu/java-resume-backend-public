package com.felixsiu.resume.controller;

import com.felixsiu.resume.model.dto.LoginDto;
import com.felixsiu.resume.model.dto.RegisterDto;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.model.vo.LoginVo;
import com.felixsiu.resume.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * 用戶接口
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "User", description = "用戶的相關接口")
public class UserController {
		@Autowired
		UserService userService;
		
		@PostMapping("/register")
		@Operation(summary = "註冊新的用戶")
		public BaseVo<Void> register(@Valid @RequestBody RegisterDto registerDto) {
				return userService.register(registerDto);
		}
		
		@PostMapping("/login")
		@Operation(summary = "登入")
		public BaseVo<LoginVo> login(
						@Valid @RequestBody LoginDto loginDto,
						@Parameter(hidden = true)
						HttpSession httpSession
		) {
				return userService.login(loginDto, httpSession);
		}
		
		@GetMapping("/getCaptcha")
		@Operation(summary = "獲取圖形驗證碼")
		public BaseVo<String> getCaptcha(
						@Parameter(hidden = true)
						HttpSession httpSession
		) throws IOException {
				return userService.createCaptcha(httpSession);
		}
}
