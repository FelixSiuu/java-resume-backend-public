package com.felixsiu.resume.annotation;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiResponses(value = {
				@ApiResponse(responseCode = "200", description = "成功"),
				@ApiResponse(responseCode = "401", description = "未授權，請先登入")
})
public @interface StandardApiResponses {
}
