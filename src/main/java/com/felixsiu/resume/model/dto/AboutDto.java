package com.felixsiu.resume.model.dto;

import jakarta.validation.constraints.NotBlank;

public record AboutDto(
				@NotBlank(message = "paragraph 不能為空")
				String paragraph) {
}
