package com.felixsiu.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.felixsiu.resume.model.entity.EmailToken;
import com.felixsiu.resume.respository.EmailTokenMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EmailTokenService extends ServiceImpl<EmailTokenMapper, EmailToken> {
		public static Integer tokenExpiresInMinutes = 15;
		
		/**
		 * add token hash record
		 *
		 * @param tokenHash token hash
		 * @param email     email
		 * @param userId    user id
		 * @return bool
		 */
		public boolean insert(String tokenHash, String email, String userId, String purpose) {
				LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(tokenExpiresInMinutes);
				EmailToken entity = new EmailToken();
				entity.setEmail(email);
				entity.setUserId(userId);
				entity.setTokenHash(tokenHash);
				entity.setExpiresAt(expiresAt);
				entity.setPurpose(purpose);
				
				return this.save(entity);
		}
		
		/**
		 * find valid token hash: token match & purpose match & before expires time & used time null
		 *
		 * @param tokenHash token hash
		 * @param purpose   VERIFY or RESET
		 * @return EmailToken
		 */
		public EmailToken findByValidHash(String tokenHash, String purpose) {
				LocalDateTime now = LocalDateTime.now();
				return this.getOne(new LambdaQueryWrapper<EmailToken>()
								.eq(EmailToken::getTokenHash, tokenHash)
								.eq(EmailToken::getPurpose, purpose)
								.gt(EmailToken::getExpiresAt, now)
								.isNull(EmailToken::getUsedAt)
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
				return this.update(new LambdaUpdateWrapper<EmailToken>()
								.eq(EmailToken::getTokenHash, tokenHash)
								.isNull(EmailToken::getUsedAt)
								.set(EmailToken::getUsedAt, now)
				);
		}
		
		/**
		 * revoke unused token by user id (email verify or password reset)
		 *
		 * @param userId user id
		 * @return affected row
		 */
		public int revokeUnusedByUserId(String userId, String purpose) {
				return this.baseMapper.update(new LambdaUpdateWrapper<EmailToken>()
								.eq(EmailToken::getUserId, userId)
								.eq(EmailToken::getPurpose, purpose)
								.isNull(EmailToken::getUsedAt)
								.set(EmailToken::getUsedAt, LocalDateTime.now())
				);
		}
}
