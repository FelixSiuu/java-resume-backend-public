package com.felixsiu.resume.handler;

import com.felixsiu.resume.exception.BusinessException;
import com.felixsiu.resume.model.ErrorMap;
import com.felixsiu.resume.model.vo.BaseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

		@ExceptionHandler(BusinessException.class)
		public ResponseEntity<BaseVo<?>> handleBusinessException(BusinessException e) {
				log.warn("Business error [{}]: {}", e.getCode(), e.getMessage());
				return ResponseEntity.status(e.getStatus())
								.body(BaseVo.fail(e.getCode(), e.getMessage(), e.getData()));
		}

		@ExceptionHandler(MethodArgumentNotValidException.class)
		public ResponseEntity<BaseVo<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
				FieldError fieldError = e.getBindingResult().getFieldError();
				String message = (fieldError != null) ? fieldError.getDefaultMessage() : ErrorMap.VALIDATION_FAILED.getMessage();
				log.warn("Validation failed: {}", message);
				return ResponseEntity.status(ErrorMap.VALIDATION_FAILED.getStatus())
								.body(BaseVo.fail(ErrorMap.VALIDATION_FAILED.getCode(), message));
		}

		@ExceptionHandler(DuplicateKeyException.class)
		public ResponseEntity<BaseVo<?>> handleDuplicateKeyException(DuplicateKeyException e) {
				log.warn("Duplicate key: {}", e.getMessage());
				ErrorMap error = ErrorMap.DUPLICATE_KEY;
				return ResponseEntity.status(error.getStatus())
								.body(BaseVo.fail(error.getCode(), error.getMessage()));
		}

		@ExceptionHandler(Exception.class)
		public ResponseEntity<BaseVo<?>> handleException(Exception e) {
				log.error("Unhandled exception", e);
				ErrorMap error = ErrorMap.INTERNAL_ERROR;
				return ResponseEntity.status(error.getStatus())
								.body(BaseVo.fail(error.getCode(), error.getMessage()));
		}
}
