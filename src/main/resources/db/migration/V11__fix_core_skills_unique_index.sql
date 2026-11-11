ALTER TABLE `core_skills`
  DROP INDEX `type_UNIQUE`,
  ADD UNIQUE INDEX `uk_user_summary` (`user_id` ASC, `summary` ASC);
