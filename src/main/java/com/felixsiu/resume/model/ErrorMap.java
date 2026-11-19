package com.felixsiu.resume.model;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 業務錯誤目錄：HTTP 狀態碼 + 機器可讀 code + 預設訊息。
 * 拋例外時可覆寫 message，並可選附帶 response data（例如 CoolDownVo）。
 */
@Getter
public enum ErrorMap {

		// ---- 400 Bad Request（前端留在當前頁顯示錯誤，不導回登入）----
		VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "參數校驗失敗"),
		MISSING_PARAM(HttpStatus.BAD_REQUEST, "MISSING_PARAM", "參數缺失"),
		INVALID_CAPTCHA(HttpStatus.BAD_REQUEST, "INVALID_CAPTCHA", "captcha 錯誤"),
		/** 含：缺用戶名、密碼錯誤、username/password 不符等；可覆寫 message */
		INVALID_CREDENTIALS(HttpStatus.BAD_REQUEST, "INVALID_CREDENTIALS", "username 或 password錯誤"),
		INVALID_TOKEN(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "token 無效或已失效"),
		PASSWORD_UNCHANGED(HttpStatus.BAD_REQUEST, "PASSWORD_UNCHANGED", "新舊密碼一致，請重新輸入"),
		TOKEN_EMPTY(HttpStatus.BAD_REQUEST, "TOKEN_EMPTY", "token不能為空"),
		INVALID_SLUG(HttpStatus.BAD_REQUEST, "INVALID_SLUG", "請輸入 slug"),
		DUPLICATE_SLUG_IN_REQUEST(HttpStatus.BAD_REQUEST, "DUPLICATE_SLUG_IN_REQUEST", "傳入的 slug 包含重複項"),
		SLUG_MISSING_OR_EXTRA(HttpStatus.BAD_REQUEST, "SLUG_MISSING_OR_EXTRA", "slug 缺失或多餘，請重新整理後重試"),
		SLUG_MISMATCH(HttpStatus.BAD_REQUEST, "SLUG_MISMATCH", "slug 不匹配，包含無效的項目"),
		NO_PROJECTS(HttpStatus.BAD_REQUEST, "NO_PROJECTS", "暫無項目，請添加項目後重試"),
		FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "FILE_TOO_LARGE", "圖片不能超過 2MB"),
		UNSUPPORTED_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "UNSUPPORTED_IMAGE_TYPE", "僅支持 JPG/PNG 格式"),
		CONTENT_UNCHANGED(HttpStatus.BAD_REQUEST, "CONTENT_UNCHANGED", "修改失敗，資料不存在或內容未變更"),

		// ---- 401 / 403（前端導回重新登錄）----
		UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "已過期，請重新登錄"),
		INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "refresh token 錯誤"),

		// ---- 403 Forbidden ----
		FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "未經授權的活動"),
		INVALID_XSRF(HttpStatus.FORBIDDEN, "INVALID_XSRF", "XSRF token 無效或缺失"),

		// ---- 404 Not Found ----
		USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "找不到該用戶"),
		RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "資料不存在"),
		PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "未找到對應項目"),
		PROJECT_DETAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT_DETAIL_NOT_FOUND", "未找到對應項目詳情"),

		// ---- 409 Conflict ----
		EMAIL_TAKEN(HttpStatus.CONFLICT, "EMAIL_TAKEN", "該電子郵箱已被佔用"),
		USERNAME_TAKEN(HttpStatus.CONFLICT, "USERNAME_TAKEN", "該用戶已存在"),
		SLUG_TAKEN(HttpStatus.CONFLICT, "SLUG_TAKEN", "該 slug 已被使用，請更換一個"),
		EMAIL_ALREADY_VERIFIED(HttpStatus.CONFLICT, "EMAIL_ALREADY_VERIFIED", "該電子郵箱已驗證，無需重複綁定"),
		DUPLICATE_KEY(HttpStatus.CONFLICT, "DUPLICATE_KEY", "操作失敗：該數據已存在，請更換後重試"),

		// ---- 429 Too Many Requests ----
		RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "Too many requests, please try again later."),

		// ---- 500 Internal Server Error ----
		CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CREATE_FAILED", "添加失敗"),
		UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "UPDATE_FAILED", "更新失敗"),
		DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "DELETE_FAILED", "刪除失敗"),
		ACCOUNT_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ACCOUNT_CREATE_FAILED", "創建帳號失敗"),
		REFRESH_TOKEN_WRITE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "REFRESH_TOKEN_WRITE_FAILED", "refresh token 寫入失敗"),
		EMAIL_CODE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL_CODE_FAILED", "獲取電子郵箱驗證碼失敗"),
		EMAIL_VERIFY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL_VERIFY_FAILED", "驗證電子郵箱出現了錯誤"),
		CAPTCHA_GENERATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CAPTCHA_GENERATE_FAILED", "驗證碼生成失敗，請重新整理頁面！"),
		INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "系統未知錯誤，請聯繫管理員");

		private final HttpStatus status;
		/** BaseVo.code / 前端分支用 */
		private final String code;
		/** 預設對使用者顯示的訊息（可在拋例外時覆寫） */
		private final String message;

		ErrorMap(HttpStatus status, String code, String message) {
				this.status = status;
				this.code = code;
				this.message = message;
		}

		public int getStatusCode() {
				return status.value();
		}
}
