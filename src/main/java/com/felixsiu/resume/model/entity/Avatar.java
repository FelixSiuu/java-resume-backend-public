package com.felixsiu.resume.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "avatar")
public class Avatar {
		@TableId(type = IdType.AUTO)
		private Integer id;
		private String userId;
		private String contentType;
		private Integer fileSize;
		private byte[] imageData; // BLOB -> byte[]
		@TableField(updateStrategy = FieldStrategy.NEVER, insertStrategy = FieldStrategy.NEVER)
		private LocalDateTime createTime;
		@TableField(updateStrategy = FieldStrategy.NEVER, insertStrategy = FieldStrategy.NEVER)
		private LocalDateTime updateTime;
}
