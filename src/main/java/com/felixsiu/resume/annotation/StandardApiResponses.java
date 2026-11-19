package com.felixsiu.resume.annotation;

import com.felixsiu.resume.model.vo.BaseVo;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.*;

/**
 * 受保護接口共用的 OpenAPI 回應說明。
 * 200 沿用方法回傳型別；4xx 必須自訂 content，否則會誤用成功 schema。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiResponses(value = {
				@ApiResponse(responseCode = "200", description = "成功"),
				@ApiResponse(
								responseCode = "401",
								description = "未授權，請先登入",
								content = @Content(
												mediaType = "application/json",
												schema = @Schema(implementation = BaseVo.class),
												examples = @ExampleObject(
																name = "unauthorized",
																value = """
																				{
																				  "isSuccess": false,
																				  "code": "UNAUTHORIZED",
																				  "message": "已過期，請重新登錄",
																				  "data": null
																				}
																				"""
												)
								)
				),
				@ApiResponse(
								responseCode = "403",
								description = "禁止存取（例如 XSRF 無效）",
								content = @Content(
												mediaType = "application/json",
												schema = @Schema(implementation = BaseVo.class),
												examples = @ExampleObject(
																name = "forbidden",
																value = """
																				{
																				  "isSuccess": false,
																				  "code": "INVALID_XSRF",
																				  "message": "XSRF token 無效或缺失",
																				  "data": null
																				}
																				"""
												)
								)
				)
})
public @interface StandardApiResponses {
}
