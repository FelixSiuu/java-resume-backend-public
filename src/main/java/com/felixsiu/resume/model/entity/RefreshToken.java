package com.felixsiu.resume.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("refresh_token")
@Data
public class RefreshToken {
		@TableId(type = IdType.AUTO)
		private Integer id;
		private String userId;
		private String userName;
		private String tokenHash;
		private LocalDateTime expiresAt;
		@TableField(updateStrategy = FieldStrategy.NEVER, insertStrategy = FieldStrategy.NEVER)
		private LocalDateTime createdAt;
		private Boolean revoked;
		private Boolean rememberMe;
		private Integer replacedBy;
		private LocalDateTime lastUsedAt;
		private String userAgent;
		private String ip;
}
