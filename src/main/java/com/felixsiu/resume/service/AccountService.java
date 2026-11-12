package com.felixsiu.resume.service;

import com.felixsiu.resume.model.dto.ChangePasswordDto;
import com.felixsiu.resume.model.entity.User;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.service.account.AccountBaseService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AccountService {
		@Autowired
		private AccountBaseService accountBaseService;
		@Autowired
		private BCryptPasswordEncoder passwordEncoder;
		@Autowired
		private CookieService cookieService;
		@Autowired
		private RefreshTokenService refreshTokenService;
		
		/**
		 * clean 3 tokens & revoke db refresh token
		 * @param userId user id
		 * @param response HttpServletResponse
		 */
		void actionCallback(String userId, HttpServletResponse response){
				cookieService.clearTokenCookies(response);
				
				int result = refreshTokenService.revokeAllByUserId(userId);
				if (result == 0) {
						System.err.println("revoke all token failed");
				}
		}
		
		/**
		 * set account delete time,
		 * @param userId user id
		 * @param response HttpServletResponse
		 * @return void
		 */
		@Transactional(rollbackFor = Exception.class)
		public BaseVo<Void> deleteAccount(String userId, HttpServletResponse response){
				User user = accountBaseService.selectByPK(userId);
				
				if(user == null){
						throw new RuntimeException("找不到該用戶");
				}
				
				String username = user.getUsername();
				LocalDateTime now = LocalDateTime.now();
				
				boolean deleteSuccess = accountBaseService.setAccountDeleteTime(userId, username, now);
				
				if(!deleteSuccess) {
						throw new RuntimeException("刪除失敗");
				}
				
				actionCallback(userId, response);
				
				return BaseVo.success(null);
		}
		
		/**
		 * change password, clean 3 tokens & revoke db refresh token
		 * @param userId user id
		 * @param dto change password data from front end
		 * @param response HttpServletResponse
		 * @return void
		 */
		@Transactional(rollbackFor = Exception.class)
		public BaseVo<Void> changePassword(String userId,ChangePasswordDto dto, HttpServletResponse response){
				User user = accountBaseService.selectByPK(userId);
				
				if(user == null){
						throw new RuntimeException("找不到該用戶");
				}
				
				String rawOldPassword = dto.oldPassword();
				String rawNewPassword = dto.newPassword();
				
				boolean isPwdMatch = passwordEncoder.matches(rawOldPassword, user.getPassword());
				
				if(!isPwdMatch){
						throw new RuntimeException("密碼錯誤，請重新輸入");
				}
				
				if(rawOldPassword.equals(rawNewPassword)){
						throw new RuntimeException("新舊密碼一致，請重新輸入");
				}
				
				String newPassword = passwordEncoder.encode(rawNewPassword);
				
				boolean updateSuccess = accountBaseService.updatePasswordByPK(userId, newPassword);
				
				if(!updateSuccess) {
						throw new RuntimeException("更新密碼失敗");
				}
				
				actionCallback(userId, response);
				
				return BaseVo.success(null);
		}
}
