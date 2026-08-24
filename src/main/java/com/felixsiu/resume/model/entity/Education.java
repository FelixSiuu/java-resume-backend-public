package com.felixsiu.resume.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("education")
@Data
public class Education {
		@TableId(type = IdType.AUTO)
		private Integer id;
		private String userId;
		private String startDate;
		private String endDate;
		private String institution;
		private String degree;
		private String major;
		private String location;
}