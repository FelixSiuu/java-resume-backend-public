package com.felixsiu.resume.controller;

import com.felixsiu.resume.annotation.StandardApiResponses;
import com.felixsiu.resume.model.dto.ChangePasswordDto;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 帳戶設定
 */
@RestController
@RequestMapping("/api/v1/account")
@Tag(name = "Account", description = "帳戶相關接口")
public class AccountController {
		@Autowired
		private AccountService accountService;
		
		@DeleteMapping
		@Operation(summary = "刪除當前帳戶")
		@StandardApiResponses
		public BaseVo<Void> deleteAccount(
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId,
						@Parameter(hidden = true) HttpServletResponse response
		) {
				return accountService.deleteAccount(userId, response);
		}
		
		@PutMapping("/change-password")
		@Operation(summary = "更新帳戶的密碼")
		@StandardApiResponses
		public BaseVo<Void> changePassword(
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId,
						@RequestBody @Valid ChangePasswordDto changePasswordDto,
						@Parameter(hidden = true) HttpServletResponse response
		) {
				return accountService.changePassword(userId, changePasswordDto, response);
		}
}
