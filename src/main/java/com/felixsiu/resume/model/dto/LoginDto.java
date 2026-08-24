package com.felixsiu.resume.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginDto(
				@NotBlank(message = "username 不能為空")
				@Size(min = 3, max = 20, message = "username 長度必須在 3-20 個字元之間")
				@Pattern(
								regexp = "^[a-zA-Z0-9_-]+$",
								message = "username 只能包含英文字母、數字、底線或連字號"
				)
				String username,
				@NotBlank(message = "password 不能為空")
				@Size(min = 6, max = 50, message = "password 長度必須在 6-50 個字元之間")
				String password,
				@NotBlank(message = "captcha 不能為空")
				String captcha) {
}
