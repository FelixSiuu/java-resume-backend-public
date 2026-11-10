CREATE TABLE IF NOT EXISTS `refresh_token` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `token_hash` VARCHAR(128) NOT NULL COMMENT 'SHA-256 hex',
  `expires_at` DATETIME NOT NULL COMMENT '過期時間',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `revoked_at` DATETIME NULL COMMENT '撤銷時間，NULL 表示仍有效',
  `replaced_by` BIGINT NULL COMMENT '輪換時指向新的 token id',
  `last_used_at` DATETIME NULL,
  `user_agent` VARCHAR(512) NULL,
  `ip` VARCHAR(45) NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token_hash` (`token_hash`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_expires_at` (`expires_at`),
  CONSTRAINT `fk_refresh_user`
    FOREIGN KEY (`user_id`)
    REFERENCES `user` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
