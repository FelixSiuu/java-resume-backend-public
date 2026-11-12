package com.felixsiu.resume.service.account;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.felixsiu.resume.model.entity.User;
import com.felixsiu.resume.respository.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AccountBaseService extends ServiceImpl<UserMapper, User> {
		
		/**
		 * get user by id
		 *
		 * @param userId user id
		 * @return user
		 */
		public User selectByPK(String userId) {
				return this.getOne(new LambdaQueryWrapper<User>()
								.eq(User::getId, userId)
				);
		}
		
		/**
		 * set account delete time and new username (avoid register UNIQUE KEY)
		 *
		 * @param userId    user id
		 * @param deletedAt delete time
		 * @return bool
		 */
		public boolean setAccountDeleteTime(String userId, String userName, LocalDateTime deletedAt) {
				String newUserName = userName + "-deleted-" + userId;
				return this.update(new LambdaUpdateWrapper<User>()
								.eq(User::getId, userId)
								.set(User::getDeletedAt, deletedAt)
								.set(User::getUsername, newUserName)
				);
		}
		
		/**
		 * set account new password
		 *
		 * @param userId      user id
		 * @param newPassword new password
		 * @return bool
		 */
		public boolean updatePasswordByPK(String userId, String newPassword) {
				return this.update(new LambdaUpdateWrapper<User>()
								.eq(User::getId, userId)
								.set(User::getPassword, newPassword)
				);
		}
		
		/**
		 * scheduled to clean up not null `delete_at` account
		 * @return affected row
		 */
		public int cleanUpDeletedAccount() {
				LocalDateTime now = LocalDateTime.now();
				return this.getBaseMapper().delete(new LambdaQueryWrapper<User>()
								.isNotNull(User::getDeletedAt)
								.lt(User::getDeletedAt, now)
				);
		}
}
