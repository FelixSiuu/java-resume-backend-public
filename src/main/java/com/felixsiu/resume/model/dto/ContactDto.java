package com.felixsiu.resume.model.dto;

import com.felixsiu.resume.validator.ValidSocialLinks;
import jakarta.validation.constraints.Email;
import lombok.Data;

import java.util.Map;

@Data
public class ContactDto {
		private String displayName;
		private String title;
		@Email(message = "請輸入有效的電子郵件格式")
		private String email;
		private String phone;
		private String address;
		@ValidSocialLinks
		private Map<String, String> socialLinks;
		private Map<String, Boolean> publicMap;
		
}
