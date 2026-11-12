ALTER TABLE `user`
 ADD COLUMN `deleted_at` DATETIME NULL COMMENT '刪除時間' AFTER `update_time` ;