package com.felixsiu.resume.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "contact", autoResultMap = true) // 加上 autoResultMap = true, 搭配 JacksonTypeHandler 處理JSON數據的輸出
public class Contact {
		
		@TableId(type = IdType.AUTO)
		private Integer id;
		private String userId;
		@TableField(updateStrategy = FieldStrategy.ALWAYS)
		private String displayName;
		@TableField(updateStrategy = FieldStrategy.ALWAYS)
		private String title;
		@TableField(updateStrategy = FieldStrategy.ALWAYS)
		private String email;
		@TableField(updateStrategy = FieldStrategy.ALWAYS)
		private String phone;
		@TableField(updateStrategy = FieldStrategy.ALWAYS)
		private String address;
		@TableField(updateStrategy = FieldStrategy.ALWAYS, typeHandler = JacksonTypeHandler.class)
		private Map<String, String> socialLinks;
		@TableField(updateStrategy = FieldStrategy.ALWAYS, typeHandler = JacksonTypeHandler.class)
		private Map<String, Boolean> publicMap;
		@TableField(updateStrategy = FieldStrategy.NEVER, insertStrategy = FieldStrategy.NEVER)
		private LocalDateTime createTime;
		@TableField(updateStrategy = FieldStrategy.NEVER, insertStrategy = FieldStrategy.NEVER)
		private LocalDateTime updateTime;
}
