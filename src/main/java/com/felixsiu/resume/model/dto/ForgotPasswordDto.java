package com.felixsiu.resume.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordDto(
				@NotBlank(message = "電子郵箱不能為空")
				@Email(message = "請輸入有效的電子郵件格式")
				String email,
				@NotBlank(message = "captcha 不能為空")
				String captcha
) {
}
