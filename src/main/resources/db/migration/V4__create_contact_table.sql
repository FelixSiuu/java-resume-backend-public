CREATE TABLE IF NOT EXISTS `contact` (
    `id`           INT          NOT NULL AUTO_INCREMENT,
    `user_id`      VARCHAR(255) NOT NULL DEFAULT 'user' COMMENT '用戶ID',
    `display_name` VARCHAR(100) NULL DEFAULT NULL COMMENT '顯示名稱',
    `title`        VARCHAR(100) NULL DEFAULT NULL COMMENT '頭銜',
    `email`        VARCHAR(255) NULL DEFAULT NULL COMMENT '電子郵件',
    `phone`        VARCHAR(50) NULL DEFAULT NULL COMMENT '手機號碼',
    `social_links` JSON NULL DEFAULT NULL COMMENT '社交媒體連結',
    `public_map`   JSON NULL DEFAULT NULL COMMENT '各欄位是否公開',
    `address`      VARCHAR(255) NULL DEFAULT NULL COMMENT '地址',
    `create_time`  DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX          `idx_user_id` (`user_id` ASC) VISIBLE
);
