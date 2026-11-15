package com.felixsiu.resume.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("email_verification_token")
@Data
public class EmailVerificationToken {
		@TableId(value = "token_hash", type = IdType.INPUT)
		private String tokenHash;
		private String userId;
		private String email;
		private LocalDateTime expiresAt;
		private LocalDateTime usedAt;
		@TableField(updateStrategy = FieldStrategy.NEVER, insertStrategy = FieldStrategy.NEVER)
		private LocalDateTime createTime;
}
