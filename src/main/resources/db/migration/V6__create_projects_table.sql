CREATE TABLE IF NOT EXISTS `projects` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` VARCHAR(255) NOT NULL COMMENT '用戶ID',
  `slug` VARCHAR(255) NOT NULL COMMENT '項目路徑',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '項目索引',
  `name` VARCHAR(255) NOT NULL COMMENT '項目名稱',
  `value` TEXT NOT NULL COMMENT '項目成果',
  `summary` TEXT NOT NULL COMMENT '項目摘要',
  `featured` TINYINT NOT NULL DEFAULT 0 COMMENT '該項目是否重點？',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `user_id_sort_order` (`user_id` ASC, `sort_order` ASC) VISIBLE,
  INDEX `user_id_featured` (`user_id` ASC, `featured` ASC) VISIBLE,
  UNIQUE INDEX `user_id_slug` (`user_id` ASC, `slug` ASC) VISIBLE
  );
