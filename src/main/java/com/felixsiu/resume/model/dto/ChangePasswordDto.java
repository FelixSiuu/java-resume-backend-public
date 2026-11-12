package com.felixsiu.resume.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordDto(
				@NotBlank(message = "舊密碼不能為空") String oldPassword,
				@NotBlank(message = "新密碼不能為空")
				@Size(min = 8, max = 20, message = "密碼長度必須為 8 到 20 位")
				@Pattern(
								regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]+$",
								message = "密碼必須同時包含字母和數字"
				)
				String newPassword
) {
}
