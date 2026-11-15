package com.felixsiu.resume.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterDto(
				@NotBlank(message = "username 不能為空")
				@Size(min = 3, max = 20, message = "username 長度必須在 3-20 個字元之間")
				@Pattern(
								regexp = "^[a-zA-Z0-9_-]+$",
								message = "username 只能包含英文字母、數字、底線或連字號"
				)
								String username,
				@NotBlank(message = "password 不能為空")
				@Size(min = 8, max = 20, message = "密碼長度必須為 8 到 20 位")
				@Pattern(
								regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]+$",
								message = "密碼必須同時包含字母和數字"
				)
				String password,
				@NotBlank(message = "電子郵箱不能為空")
				@Email(message = "請輸入有效的電子郵件格式")
				String email
) {
}
