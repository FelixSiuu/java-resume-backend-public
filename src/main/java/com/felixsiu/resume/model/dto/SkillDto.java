package com.felixsiu.resume.model.dto;

import jakarta.validation.constraints.NotBlank;

public record SkillDto(
				@NotBlank(message = "summary 不能為空")
				String summary,
				@NotBlank(message = "details 不能為空")
				String details) {
}
