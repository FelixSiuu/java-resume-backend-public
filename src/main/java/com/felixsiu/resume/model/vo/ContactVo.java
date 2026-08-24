package com.felixsiu.resume.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ContactVo {
		private String displayName;
		private String title;
		private String email;
		private String phone;
		private String address;
		private Map<String, String> socialLinks;
		private Map<String, Boolean> publicMap;
		private LocalDateTime createTime;
		private LocalDateTime updateTime;
}
