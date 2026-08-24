package com.felixsiu.resume.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

// 1. 元註解：定義註解的「用途」與「生命週期」
@Target({ElementType.FIELD})    // 指定這個註解只能用在欄位 (Field) 上
@Retention(RetentionPolicy.RUNTIME)     // 指定這個註解在運行時可以通過反射讀取
@Constraint(validatedBy = SocialLinksValidator.class)
@Documented // 包含在文檔中：讓 Javadoc 工具可以生成這個註解的說明文檔
// 2. 註解的「屬性」定義
public @interface ValidSocialLinks {
		
		// 可以在使用時指定錯誤訊息，並帶有預設值
		String message() default "社交連結格式無效";
		
		Class<?>[] groups() default {};
		
		Class<? extends Payload>[] payload() default {};
}