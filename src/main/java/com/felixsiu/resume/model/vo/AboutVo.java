package com.felixsiu.resume.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AboutVo {
		private Integer id;
		private String paragraph;
		private LocalDateTime createTime;
		private LocalDateTime updateTime;
}
