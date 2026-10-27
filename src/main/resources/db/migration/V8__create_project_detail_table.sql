CREATE TABLE IF NOT EXISTS `project_detail` (
  `project_id` INT NOT NULL COMMENT '關聯 projects.id',
  `role` VARCHAR(255) NULL COMMENT '項目扮演的角色',
  `duration` VARCHAR(255) NULL COMMENT '項目持續時間',
  `challenge` TEXT NULL COMMENT '項目遇到的挑戰',
  `solution` TEXT NULL COMMENT '項目挑戰的解決方案',
  `impact` TEXT NULL COMMENT '項目的影響',
  `live_url` VARCHAR(255) NULL COMMENT '項目的線上url',
  `repo_url` VARCHAR(255) NULL COMMENT '項目的倉庫url',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `ik_project_detail_id_projects_id` (`project_id` ASC) VISIBLE,
  PRIMARY KEY (`project_id`),
  CONSTRAINT `fk_project_detail_id_projects_id`
    FOREIGN KEY (`project_id`)
    REFERENCES `projects` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE);
