package com.felixsiu.resume.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("project_stacks")
@Data
public class ProjectStack {
		@TableId(type = IdType.AUTO)
		private Integer id;
		private Integer projectId;
		private String stackName;
		private Integer sortOrder;
}
