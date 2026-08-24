package com.felixsiu.resume.model.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record WorkingExpDto(
				@NotBlank(message = "startDate 不能為空")
				String startDate,
				String endDate,
				@NotBlank(message = "jobTitle 不能為空")
				String jobTitle,
				@NotBlank(message = "company 不能為空")
				String company,
				@NotBlank(message = "location 不能為空")
				String location,
				List<KeyRespDto> keyResponsibilities
) {
}
