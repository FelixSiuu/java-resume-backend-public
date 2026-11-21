package com.felixsiu.resume.controller;

import com.felixsiu.resume.annotation.StandardApiResponses;
import com.felixsiu.resume.model.dto.ChangePasswordDto;
import com.felixsiu.resume.model.dto.DeleteAccountDto;
import com.felixsiu.resume.model.dto.EmailDto;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.model.vo.EmailVo;
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
						@Parameter(hidden = true) HttpServletResponse response,
						@Valid @RequestBody DeleteAccountDto deleteAccountDto
		) {
				return accountService.deleteAccount(userId, response, deleteAccountDto);
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
		
		@PutMapping("/update-email")
		@Operation(summary = "更新帳戶的電子郵箱")
		@StandardApiResponses
		public BaseVo<EmailVo> updateEmail(
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId,
						@RequestBody @Valid EmailDto emailDto
		) {
				return accountService.updateEmail(userId, emailDto);
		}
		
		@GetMapping("/email")
		@Operation(summary = "更新帳戶的電子郵箱狀態")
		@StandardApiResponses
		public BaseVo<EmailVo> getEmail(
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId
		) {
				return accountService.queryEmailStatus(userId);
		}
}
