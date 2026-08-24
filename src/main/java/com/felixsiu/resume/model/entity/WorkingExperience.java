package com.felixsiu.resume.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("working_experience")
@Data
public class WorkingExperience {
		@TableId(type = IdType.AUTO)
		private Integer id;
		private String userId;
		private String startDate;
		private String endDate;
		private String jobTitle;
		private String company;
		private String location;
		@TableField(updateStrategy = FieldStrategy.NEVER, insertStrategy = FieldStrategy.NEVER)
		private LocalDateTime createTime;
		@TableField(updateStrategy = FieldStrategy.NEVER, insertStrategy = FieldStrategy.NEVER)
		private LocalDateTime updateTime;
}
