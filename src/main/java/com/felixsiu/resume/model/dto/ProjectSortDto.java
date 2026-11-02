package com.felixsiu.resume.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ProjectSortDto(
				@NotEmpty(message = "slugs 不能為 null")
				List<@NotBlank(message = "slug 不能為 null") String> slugs
) {
}
