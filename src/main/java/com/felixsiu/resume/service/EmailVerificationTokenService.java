package com.felixsiu.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.felixsiu.resume.model.entity.EmailVerificationToken;
import com.felixsiu.resume.respository.EmailVerificationTokenMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EmailVerificationTokenService extends ServiceImpl<EmailVerificationTokenMapper, EmailVerificationToken> {
		public static Integer tokenExpiresInMinutes = 15;
		
		/**
		 * add token hash record
		 *
		 * @param tokenHash token hash
		 * @param email     email
		 * @param userId    user id
		 * @return bool
		 */
		public boolean insert(String tokenHash, String email, String userId) {
				LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(tokenExpiresInMinutes);
				EmailVerificationToken entity = new EmailVerificationToken();
				entity.setEmail(email);
				entity.setUserId(userId);
				entity.setTokenHash(tokenHash);
				entity.setExpiresAt(expiresAt);
				
				return this.save(entity);
		}
		
		/**
		 * find valid token hash: token match & before expires time & used time null
		 *
		 * @param tokenHash token hash
		 * @return EmailVerificationToken
		 */
		public EmailVerificationToken findByValidHash(String tokenHash) {
				LocalDateTime now = LocalDateTime.now();
				return this.getOne(new LambdaQueryWrapper<EmailVerificationToken>()
								.eq(EmailVerificationToken::getTokenHash, tokenHash)
								.gt(EmailVerificationToken::getExpiresAt, now)
								.isNull(EmailVerificationToken::getUsedAt)
				);
		}
		
		/**
		 * update token hash used at (single-use: only if still unused)
		 *
		 * @param tokenHash token hash
		 * @return bool
		 */
		public boolean updateHashUsedAt(String tokenHash) {
				LocalDateTime now = LocalDateTime.now();
				return this.update(new LambdaUpdateWrapper<EmailVerificationToken>()
								.eq(EmailVerificationToken::getTokenHash, tokenHash)
								.isNull(EmailVerificationToken::getUsedAt)
								.set(EmailVerificationToken::getUsedAt, now)
				);
		}
		
		/**
		 * revoke unused token by user id
		 *
		 * @param userId user id
		 * @return affected row
		 */
		public int revokeUnusedByUserId(String userId) {
				return this.baseMapper.update(new LambdaUpdateWrapper<EmailVerificationToken>()
								.eq(EmailVerificationToken::getUserId, userId)
								.isNull(EmailVerificationToken::getUsedAt)
								.set(EmailVerificationToken::getUsedAt, LocalDateTime.now())
				);
		}
}
