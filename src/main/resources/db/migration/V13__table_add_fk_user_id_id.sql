-- projects 建表時吃到 server collation utf8mb4_unicode_ci，
-- 與 user.id 的 utf8mb4_0900_ai_ci 不相容，加 FK 前先對齊。
ALTER TABLE `projects`
  CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

ALTER TABLE `about`
ADD INDEX `fk_about_user_idx` (`user_id` ASC) VISIBLE;

ALTER TABLE `about`
ADD CONSTRAINT `fk_about_user`
  FOREIGN KEY (`user_id`)
  REFERENCES `user` (`id`)
  ON DELETE CASCADE
  ON UPDATE CASCADE;

ALTER TABLE `avatar`
ADD INDEX `fk_avatar_user_idx` (`user_id` ASC) VISIBLE;

ALTER TABLE `avatar`
ADD CONSTRAINT `fk_avatar_user`
  FOREIGN KEY (`user_id`)
  REFERENCES `user` (`id`)
  ON DELETE CASCADE
  ON UPDATE CASCADE;

ALTER TABLE `contact`
ADD INDEX `fk_contact_user_idx` (`user_id` ASC) VISIBLE;

ALTER TABLE `contact`
ADD CONSTRAINT `fk_contact_user`
  FOREIGN KEY (`user_id`)
  REFERENCES `user` (`id`)
  ON DELETE CASCADE
  ON UPDATE CASCADE;

ALTER TABLE `core_skills`
ADD INDEX `fk_skills_user_idx` (`user_id` ASC) VISIBLE;

ALTER TABLE `core_skills`
ADD CONSTRAINT `fk_skills_user`
  FOREIGN KEY (`user_id`)
  REFERENCES `user` (`id`)
  ON DELETE CASCADE
  ON UPDATE CASCADE;

ALTER TABLE `education`
ADD INDEX `fk_education_user_idx` (`user_id` ASC) VISIBLE;

ALTER TABLE `education`
ADD CONSTRAINT `fk_education_user`
  FOREIGN KEY (`user_id`)
  REFERENCES `user` (`id`)
  ON DELETE CASCADE
  ON UPDATE CASCADE;

ALTER TABLE `projects`
ADD INDEX `fk_projects_user_idx` (`user_id` ASC) VISIBLE;

ALTER TABLE `projects`
ADD CONSTRAINT `fk_projects_user`
  FOREIGN KEY (`user_id`)
  REFERENCES `user` (`id`)
  ON DELETE CASCADE
  ON UPDATE CASCADE;

ALTER TABLE `key_responsibilities`
ADD INDEX `fk_responsibilities_user_idx` (`user_id` ASC) VISIBLE;

ALTER TABLE `key_responsibilities`
ADD CONSTRAINT `fk_responsibilities_user`
  FOREIGN KEY (`user_id`)
  REFERENCES `user` (`id`)
  ON DELETE CASCADE
  ON UPDATE CASCADE;

ALTER TABLE `working_experience`
ADD INDEX `fk_experience_user_idx` (`user_id` ASC) VISIBLE;

ALTER TABLE `working_experience`
ADD CONSTRAINT `fk_experience_user`
  FOREIGN KEY (`user_id`)
  REFERENCES `user` (`id`)
  ON DELETE CASCADE
  ON UPDATE CASCADE;
