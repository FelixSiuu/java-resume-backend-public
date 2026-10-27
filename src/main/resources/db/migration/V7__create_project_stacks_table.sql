CREATE TABLE IF NOT EXISTS `project_stacks` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `project_id` INT NOT NULL COMMENT '關聯 projects.id',
  `stack_name` VARCHAR(255) NOT NULL COMMENT '技術名稱',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '顯示順序',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_project_stack` (`project_id` ASC, `stack_name` ASC) VISIBLE,
  INDEX `idx_project_sort` (`project_id` ASC, `sort_order` ASC) VISIBLE,
  CONSTRAINT `fk_project_stacks_project_id`
    FOREIGN KEY (`project_id`)
    REFERENCES `projects` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
    );
