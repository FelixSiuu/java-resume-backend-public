package com.felixsiu.resume.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("user")
@Data
public class User {
		/**
		 * 使用 UUID 作为主键，让 MyBatis-Plus 自动生成，插入時 MyBatis-Plus 不應干預
		 * 數據庫負責，MyBatis-Plus 旁觀
		 * updateStrategy = FieldStrategy.NEVER
		 * insertStrategy =  FieldStrategy.NEVER
		 * 數據庫有的，MyBatis-Plus 就旁觀；數據庫沒有的，MyBatis-Plus 就主動填。
		 */
		@TableId(type = IdType.ASSIGN_UUID) // 或 IdType.NONE
		private String id;
		private String username;
		private String password;
		@TableField(value = "create_time", updateStrategy = FieldStrategy.NEVER, insertStrategy = FieldStrategy.NEVER)
		private LocalDateTime createTime;
		@TableField(value = "update_time", updateStrategy = FieldStrategy.NEVER, insertStrategy = FieldStrategy.NEVER)
		private LocalDateTime updateTime;
}
