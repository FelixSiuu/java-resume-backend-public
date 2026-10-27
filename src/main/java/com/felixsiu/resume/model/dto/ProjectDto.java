package com.felixsiu.resume.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record ProjectDto(
				@NotBlank(message = "slug 不能為 null")
				@Pattern(
								regexp = "^[a-zA-Z0-9_-]+$",
								message = "slug 只能包含英文字母、數字、底線或連字號"
				)
				String slug,
				@NotBlank(message = "name 不能為 null")
				String name,
				@NotBlank(message = "value 不能為 null")
				String value,
				@NotBlank(message = "summary 不能為 null")
				String summary,
				@NotNull(message = "featured 不能為 null")
				Boolean featured,
				@NotNull(message = "stacks 不能為 null")
				List<ProjectStackDto> stacks,
				String role,
				String duration,
				String challenge,
				String solution,
				String impact,
				@Pattern(
								regexp = "^https?://[\\w.-]+(?:\\.[\\w.-]+)+[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=]*$",
								message = "請輸入有效的網址（必須以 http:// 或 https:// 開頭）"
				)
				String liveUrl,
				@Pattern(
								regexp = "^https?://[\\w.-]+(?:\\.[\\w.-]+)+[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=]*$",
								message = "請輸入有效的網址（必須以 http:// 或 https:// 開頭）"
				)
				String repoUrl
) {
}
