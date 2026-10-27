package com.felixsiu.resume.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProjectDetailVo {
		private String slug;
		private String name;
		private String value;
		private String summary;
		private Boolean featured;
		private List<String> stacks;
		private String role;
		private String duration;
		private String challenge;
		private String solution;
		private String impact;
		private String liveUrl;
		private String repoUrl;
		private LocalDateTime createTime;
		private LocalDateTime updateTime;
}
