package com.felixsiu.resume.model.vo;

import lombok.Data;

@Data
public class BaseVo<T> {
		private Boolean isSuccess;
		private String code;
		private String message;
		private T data;
		
		private BaseVo(Boolean isSuccess, String code, String message, T data) {
				this.isSuccess = isSuccess;
				this.code = code;
				this.message = message;
				this.data = data;
		}
		
		public static <F> BaseVo<F> success(F data) {
				return new BaseVo<>(true, "OK", "success!", data);
		}
		
		public static <F> BaseVo<F> fail(String code, String message) {
				return new BaseVo<>(false, code, message, null);
		}
}
