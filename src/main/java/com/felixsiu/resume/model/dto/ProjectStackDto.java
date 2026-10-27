package com.felixsiu.resume.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ProjectStackDto(
				@NotBlank(message = "stack 不能為空")
				String stackName,
				@NotBlank(message = "sortOrder 不能為空")
				@Pattern(
								regexp = "^0$|^[1-9][0-9]*$",
								message = "sortOrder 只能輸入非負整數，且不能以 0 開頭（除非是 0 本身）"
				)
				Integer sortOrder
) {
}
