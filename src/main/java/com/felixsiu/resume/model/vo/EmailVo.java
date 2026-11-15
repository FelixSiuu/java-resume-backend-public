package com.felixsiu.resume.model.vo;

import lombok.Data;

@Data
public class EmailVo {
		private String email;
		private Boolean verified;
		private String nextStepMessage;
}
