package com.felixsiu.resume.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("project_detail")
@Data
public class ProjectDetail {
		@TableId(value = "project_id", type = IdType.INPUT)
		private Integer projectId;
		private String role;
		private String duration;
		private String challenge;
		private String solution;
		private String impact;
		private String liveUrl;
		private String repoUrl;
		@TableField(updateStrategy = FieldStrategy.NEVER, insertStrategy = FieldStrategy.NEVER)
		private LocalDateTime createTime;
		@TableField(updateStrategy = FieldStrategy.NEVER, insertStrategy = FieldStrategy.NEVER)
		private LocalDateTime updateTime;
}
