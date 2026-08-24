package com.felixsiu.resume.model.dto;

import jakarta.validation.constraints.NotBlank;

public record EduDto(
				@NotBlank(message = "startDate 不能為空")
				String startDate,
				@NotBlank(message = "endDate 不能為空")
				String endDate,
				@NotBlank(message = "institution 不能為空")
				String institution,
				String degree,
				@NotBlank(message = "major 不能為空")
				String major,
				@NotBlank(message = "location 不能為空")
				String location
) {
}
