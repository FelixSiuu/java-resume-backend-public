package com.felixsiu.resume.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record KeyRespDto(
				@NotNull(message = "workingId 不能為空")
				Integer workingId,
				String title,
				@NotBlank(message = "responsibility 不能為空")
				String responsibility
) {
}
