package com.felixsiu.resume.model.dto;

import jakarta.validation.constraints.NotBlank;

public record ProjectStackDto(
				@NotBlank(message = "stack 不能為空")
				String stackName
) {
}
