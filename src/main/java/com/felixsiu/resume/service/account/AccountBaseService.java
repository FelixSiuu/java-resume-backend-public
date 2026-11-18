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
		 * Soft-deleted accounts are hard-deleted after this many days.
		 */
		public static final int ACCOUNT_PURGE_GRACE_DAYS = 30;
		
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
		 * get user (verified email & not soft deleted)
		 *
		 * @param email email
		 * @return User
		 */
		public User selectByVerifiedEmail(String email) {
				return this.getOne(new LambdaQueryWrapper<User>()
								.eq(User::getEmail, email)
								.isNotNull(User::getEmailVerifiedAt)
								.isNull(User::getDeletedAt)
				);
		}
		
		/**
		 * set account delete time and new username and null email (avoid register UK, update email UK)
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
								.set(User::getEmail, null)
								.set(User::getEmailVerifiedAt, null)
				);
		}
		
		/**
		 * set account new password
		 *
		 * @param userId      user id
		 * @param newPassword new encoded password
		 * @return bool
		 */
		public boolean updatePasswordByPK(String userId, String newPassword) {
				return this.update(new LambdaUpdateWrapper<User>()
								.eq(User::getId, userId)
								.set(User::getPassword, newPassword)
				);
		}
		
		/**
		 * Hard-delete accounts soft-deleted for at least {@link #ACCOUNT_PURGE_GRACE_DAYS} days.
		 *
		 * @return affected row
		 */
		public int cleanUpDeletedAccount() {
				LocalDateTime cutoff = LocalDateTime.now().minusDays(ACCOUNT_PURGE_GRACE_DAYS);
				return this.getBaseMapper().delete(new LambdaQueryWrapper<User>()
								.isNotNull(User::getDeletedAt)
								.lt(User::getDeletedAt, cutoff)
				);
		}
		
		public Long countEmail(String email) {
				return this.count(new LambdaQueryWrapper<User>()
								.eq(User::getEmail, email)
				);
		}
		
		/**
		 * set account email
		 *
		 * @param userId          user id
		 * @param email           email
		 * @param emailVerifiedAt email verify time (null for pending verify)
		 * @return bool
		 */
		public boolean updateEmailByPK(String userId, String email, LocalDateTime emailVerifiedAt) {
				return this.update(new LambdaUpdateWrapper<User>()
								.eq(User::getId, userId)
								.set(User::getEmail, email)
								.set(User::getEmailVerifiedAt, emailVerifiedAt)
				);
		}
}
