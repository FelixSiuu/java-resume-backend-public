package com.felixsiu.resume.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CoreSkillVo {
		private Integer id;
		private String summary;
		private String details;
		private LocalDateTime createTime;
		private LocalDateTime updateTime;
}
