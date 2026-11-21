package com.felixsiu.resume.model.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountDto(
				@NotBlank(message = "Verification code 不能為空")
				String code
) {
}
