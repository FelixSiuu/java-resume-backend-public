package com.felixsiu.resume.exception;

import com.felixsiu.resume.model.ErrorMap;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 業務例外：預設取自 {@link ErrorMap}，可覆寫 message、附帶 data。
 */
@Getter
public class BusinessException extends RuntimeException {

		private final ErrorMap error;
		private final Object data;

		public BusinessException(ErrorMap error) {
				super(error.getMessage());
				this.error = error;
				this.data = null;
		}

		public BusinessException(ErrorMap error, String message) {
				super(message);
				this.error = error;
				this.data = null;
		}

		public BusinessException(ErrorMap error, Object data) {
				super(error.getMessage());
				this.error = error;
				this.data = data;
		}

		public BusinessException(ErrorMap error, String message, Object data) {
				super(message);
				this.error = error;
				this.data = data;
		}

		public HttpStatus getStatus() {
				return error.getStatus();
		}

		public String getCode() {
				return error.getCode();
		}
}
