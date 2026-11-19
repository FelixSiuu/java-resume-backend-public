package com.felixsiu.resume.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.felixsiu.resume.model.ErrorMap;
import com.felixsiu.resume.model.vo.BaseVo;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 攔截器無法進入 {@code @RestControllerAdvice} 時，用同一套 BaseVo 寫入錯誤回應。
 */
@Component
@RequiredArgsConstructor
public class ErrorResponseWriter {

		private final ObjectMapper objectMapper;

		public void write(HttpServletResponse response, ErrorMap error) throws IOException {
				write(response, error, error.getMessage(), null);
		}

		public void write(HttpServletResponse response, ErrorMap error, String message, Object data) throws IOException {
				if (response.isCommitted()) {
						return;
				}
				response.setStatus(error.getStatusCode());
				response.setCharacterEncoding(StandardCharsets.UTF_8.name());
				response.setContentType(MediaType.APPLICATION_JSON_VALUE);
				BaseVo<?> body = BaseVo.fail(error.getCode(), message, data);
				objectMapper.writeValue(response.getWriter(), body);
		}
}
