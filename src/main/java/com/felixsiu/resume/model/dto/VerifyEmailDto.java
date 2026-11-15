package com.felixsiu.resume.model.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailDto(
				@NotBlank(message = "token不能為空")
				String token
) {
}
