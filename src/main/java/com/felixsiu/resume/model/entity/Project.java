package com.felixsiu.resume.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("projects")
@Data
public class Project {
		@TableId(type = IdType.AUTO)
		private Integer id;
		private String userId;
		private String slug;
		private Integer sortOrder;
		private String name;
		private String value;
		private String summary;
		private Boolean featured;
		@TableField(updateStrategy = FieldStrategy.NEVER, insertStrategy = FieldStrategy.NEVER)
		private LocalDateTime createTime;
		@TableField(updateStrategy = FieldStrategy.NEVER, insertStrategy = FieldStrategy.NEVER)
		private LocalDateTime updateTime;
}
