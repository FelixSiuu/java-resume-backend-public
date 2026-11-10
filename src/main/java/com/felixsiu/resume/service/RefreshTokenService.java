package com.felixsiu.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.felixsiu.resume.model.entity.RefreshToken;
import com.felixsiu.resume.respository.RefreshTokenMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RefreshTokenService extends ServiceImpl<RefreshTokenMapper, RefreshToken> {
		
		/**
		 * insert refresh token
		 *
		 * @param entity Refresh token entity
		 * @return bool
		 */
		public boolean insert(RefreshToken entity) {
				return this.save(entity);
		}
		
		/**
		 * get refresh token hash by hash
		 *
		 * @param hash query token hash
		 * @return Refresh token entity
		 */
		public RefreshToken selectOneByHash(String hash) {
				return this.getOne(new LambdaQueryWrapper<RefreshToken>()
								.eq(RefreshToken::getTokenHash, hash)
				);
		}
		
		
		/**
		 * update refresh token by id
		 *
		 * @param entity new refresh token entity
		 * @return bool
		 */
		public boolean updateByPK(RefreshToken entity) {
				return this.updateById(entity);
		}
		
		/**
		 * 樂觀鎖 set XX when XX from XX
		 *
		 * @param id         token id
		 * @param replacedBy new token id
		 * @return bool
		 */
		public boolean revokeIfActive(Integer id, Integer replacedBy) {
				return this.update(new LambdaUpdateWrapper<RefreshToken>()
								.eq(RefreshToken::getId, id)
								.eq(RefreshToken::getRevoked, false)
								.set(RefreshToken::getRevoked, true)
								.set(RefreshToken::getReplacedBy, replacedBy)
				);
		}
		
		/**
		 * revoke current user all refresh token
		 *
		 * @param userId user id
		 * @return affected row (should > 0)
		 */
		public int revokeAllByUserId(String userId) {
				return this.getBaseMapper().update(null, new LambdaUpdateWrapper<RefreshToken>()
								.eq(RefreshToken::getUserId, userId)
								.eq(RefreshToken::getRevoked, false)
								.set(RefreshToken::getRevoked, true)
				);
		}
		
		
		/**
		 * clean up expired token or (revoked and created before 30 days)
		 * @return affected row
		 */
		public int cleanUpExpiredTokens() {
				LocalDateTime now = LocalDateTime.now();
				LocalDateTime retentionDate = now.minusDays(30);
				
				return this.getBaseMapper().delete(new LambdaQueryWrapper<RefreshToken>()
								.lt(RefreshToken::getExpiresAt, now)
								.or()
								.eq(RefreshToken::getRevoked, true)
								.lt(RefreshToken::getCreatedAt, retentionDate)
				);
		}
}
