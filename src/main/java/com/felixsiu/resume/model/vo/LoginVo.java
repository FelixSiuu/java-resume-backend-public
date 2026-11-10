package com.felixsiu.resume.model.vo;

import lombok.Data;

@Data
public class LoginVo {
		private String id;
		private String xsrfToken;
		private String username;
}
