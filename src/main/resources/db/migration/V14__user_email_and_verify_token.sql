ALTER TABLE `user`
ADD COLUMN `email` VARCHAR(255) NULL COMMENT '用戶郵箱' AFTER `password`,
ADD COLUMN `email_verified_at` DATETIME NULL AFTER `email`,
ADD UNIQUE INDEX `email_UNIQUE` (`email` ASC) VISIBLE;

CREATE TABLE `email_verification_token` (
  `user_id` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `token_hash` VARCHAR(128) NOT NULL,
  `email` VARCHAR(255) NOT NULL COMMENT '本次驗證的郵箱',
  `expires_at` DATETIME NOT NULL COMMENT '過期時間',
  `used_at` DATETIME NULL COMMENT '使用時間',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`token_hash`),
  INDEX `fk_email_verification_user_idx` (`user_id` ASC) VISIBLE,
  CONSTRAINT `fk_email_verification_user`
    FOREIGN KEY (`user_id`)
    REFERENCES `user` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
