package com.felixsiu.resume.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProjectVo {
		private Integer id;
		private String slug;
		private Integer sortOrder;
		private String name;
		private String value;
		private String summary;
		private List<String> stacks;
		private Boolean featured;
		private LocalDateTime createTime;
		private LocalDateTime updateTime;
}
