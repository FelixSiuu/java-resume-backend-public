package com.felixsiu.resume.handler;

import com.felixsiu.resume.model.vo.BaseVo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


import java.io.IOException;

@RestControllerAdvice   // 告訴 Spring 這是全域異常攔截器
public class GlobalExceptionHandler {
		/**
		 * 專門攔截前端傳參不完整、不合法（@Valid 觸發）的異常
		 */
		@ExceptionHandler(MethodArgumentNotValidException.class)
		public BaseVo<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
				// 抓取第一個校驗失敗的錯誤訊息
				FieldError fieldError = e.getBindingResult().getFieldError();
				String defaultMessage = (fieldError != null) ? fieldError.getDefaultMessage() : "前端參數校驗失敗";
				System.err.println("【前端傳參阻斷】: " + defaultMessage);
				return BaseVo.fail(null, defaultMessage);
		}
		
		/**
		 * 專門攔截業務中拋出的 RuntimeException
		 */
		@ExceptionHandler(RuntimeException.class)
		public BaseVo<Void> handleRuntimeException(RuntimeException e) {
				System.err.println("系統業務發生異常: " + e.getMessage());
				return BaseVo.fail(null, e.getMessage());
		}
		
		@ExceptionHandler(IOException.class)
		public BaseVo<Void> handleIOException(IOException e) {
				System.err.println("【全域攔截】驗證碼圖片生成或 I/O 操作失敗: " + e.getMessage());
				return BaseVo.fail(null, "驗證碼生成失敗，請重新整理頁面！");
		}
		
		@ExceptionHandler(DuplicateKeyException.class)
		public BaseVo<Void> handleDuplicateKeyException(DuplicateKeyException e) {
				System.err.println("【資料庫唯一鍵衝突】: " + e.getMessage());
				return BaseVo.fail(null, "請勿重複添加");
		}
		
		/**
		 * 攔截系統其他未知異常（例如資料庫斷線、空指標等）
		 */
		@ExceptionHandler(Exception.class)
		public BaseVo<Void> handleException(Exception e) {
				e.printStackTrace();
				return BaseVo.fail(null, "系統未知錯誤，請聯繫管理員");
		}
}
