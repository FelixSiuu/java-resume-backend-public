ALTER TABLE `refresh_token`
  ADD COLUMN `user_name` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT 'user name 用戶名稱' AFTER `user_id`,
  CHANGE COLUMN `revoked_at` `revoked` TINYINT NOT NULL DEFAULT 0 COMMENT '是否撤銷',
  ADD COLUMN `remember_me` TINYINT NOT NULL DEFAULT 0 COMMENT '是否記住我' AFTER `revoked`;
