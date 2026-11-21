package com.felixsiu.resume.service;

import com.felixsiu.resume.exception.BusinessException;
import com.felixsiu.resume.model.EmailTokenMap;
import com.felixsiu.resume.model.ErrorMap;
import com.felixsiu.resume.model.dto.ChangePasswordDto;
import com.felixsiu.resume.model.dto.DeleteAccountDto;
import com.felixsiu.resume.model.dto.EmailDto;
import com.felixsiu.resume.model.entity.EmailToken;
import com.felixsiu.resume.model.entity.User;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.model.vo.EmailVo;
import com.felixsiu.resume.service.account.AccountBaseService;
import com.felixsiu.resume.service.emailtoken.EmailTokenBaseService;
import com.felixsiu.resume.utils.CacheKeys;
import com.felixsiu.resume.utils.MyRedisTemplate;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class AccountService {
		@Autowired
		private AccountBaseService accountBaseService;
		@Autowired
		private BCryptPasswordEncoder passwordEncoder;
		@Autowired
		private AuthService authService;
		@Autowired
		private EmailTokenService emailTokenService;
		@Autowired
		private EmailTokenBaseService emailTokenBaseService;
		@Autowired
		private MyRedisTemplate myRedisTemplate;
		
		/**
		 * set account delete time,
		 *
		 * @param userId   user id
		 * @param response HttpServletResponse
		 * @return void
		 */
		@Transactional(rollbackFor = Exception.class)
		public BaseVo<Void> deleteAccount(String userId, HttpServletResponse response, DeleteAccountDto dto) {
				EmailToken validToken = emailTokenService.findValidEmailToken(dto.code(), EmailTokenMap.DELETE);
				
				User user = accountBaseService.selectByPK(userId);
				
				if (user == null) {
						throw new BusinessException(ErrorMap.USER_NOT_FOUND);
				}
				
				// code must belong to this user and the current email (re-request after email change)
				if (!validToken.getUserId().equals(userId)
								|| user.getEmail() == null
								|| !user.getEmail().equals(validToken.getEmail())) {
						throw new BusinessException(ErrorMap.INVALID_TOKEN);
				}
				
				String username = user.getUsername();
				LocalDateTime now = LocalDateTime.now();
				
				boolean deleteSuccess = accountBaseService.setAccountDeleteTime(userId, username, now);
				
				if (!deleteSuccess) {
						throw new BusinessException(ErrorMap.DELETE_FAILED);
				}
				
				boolean updateHashUsedAtSuccess = emailTokenBaseService.updateHashUsedAt(validToken.getTokenHash());
				
				if (!updateHashUsedAtSuccess) {
						throw new BusinessException(ErrorMap.UPDATE_FAILED, "刪除帳戶出現了錯誤");
				}
				
				myRedisTemplate.deleteAll(CacheKeys.allUserKeys(userId));
				
				authService.invalidateSession(userId, response);
				
				return BaseVo.success(null);
		}
		
		/**
		 * change password, clean 3 tokens & revoke db refresh token
		 *
		 * @param userId   user id
		 * @param dto      change password data from front end
		 * @param response HttpServletResponse
		 * @return void
		 */
		@Transactional(rollbackFor = Exception.class)
		public BaseVo<Void> changePassword(String userId, ChangePasswordDto dto, HttpServletResponse response) {
				User user = accountBaseService.selectByPK(userId);
				
				if (user == null) {
						throw new BusinessException(ErrorMap.USER_NOT_FOUND);
				}
				
				String rawOldPassword = dto.oldPassword();
				String rawNewPassword = dto.newPassword();
				
				boolean isPwdMatch = passwordEncoder.matches(rawOldPassword, user.getPassword());
				
				if (!isPwdMatch) {
						throw new BusinessException(ErrorMap.INVALID_CREDENTIALS, "密碼錯誤，請重新輸入");
				}
				
				if (rawOldPassword.equals(rawNewPassword)) {
						throw new BusinessException(ErrorMap.PASSWORD_UNCHANGED);
				}
				
				String newPassword = passwordEncoder.encode(rawNewPassword);
				
				boolean updateSuccess = accountBaseService.updatePasswordByPK(userId, newPassword);
				
				if (!updateSuccess) {
						throw new BusinessException(ErrorMap.UPDATE_FAILED, "更新密碼失敗");
				}
				
				authService.invalidateSession(userId, response);
				
				return BaseVo.success(null);
		}
		
		/**
		 * Bind or change email and send verification mail.
		 * Same unverified email → resend only (acts as /resend).
		 *
		 * @param userId user id
		 * @param dto    email data from front end
		 * @return update email vo
		 */
		@Transactional(rollbackFor = Exception.class)
		public BaseVo<EmailVo> updateEmail(String userId, EmailDto dto) {
				User foundUser = accountBaseService.selectByPK(userId);
				
				if (foundUser == null) {
						throw new BusinessException(ErrorMap.USER_NOT_FOUND);
				}
				
				String newEmail = dto.email();
				String existingEmail = foundUser.getEmail();
				LocalDateTime existingEmailVerifiedAt = foundUser.getEmailVerifiedAt();
				
				if (newEmail.equals(existingEmail)) {
						if (existingEmailVerifiedAt != null) {
								throw new BusinessException(ErrorMap.EMAIL_ALREADY_VERIFIED);
						}
						// same pending email → resend verification
						emailTokenService.sendVerifyTokenEmail(newEmail, userId, EmailTokenMap.VERIFY);
						return BaseVo.success(pendingEmailVo(newEmail));
				}
				
				Long thisEmailCount = accountBaseService.countEmail(newEmail);
				
				if (thisEmailCount > 0) {
						throw new BusinessException(ErrorMap.EMAIL_TAKEN);
				}
				
				boolean updateSuccess = accountBaseService.updateEmailByPK(userId, newEmail, null);
				
				if (!updateSuccess) {
						throw new BusinessException(ErrorMap.UPDATE_FAILED, "更新電子郵箱失敗");
				}
				
				emailTokenService.sendVerifyTokenEmail(newEmail, userId, EmailTokenMap.VERIFY);
				return BaseVo.success(pendingEmailVo(newEmail));
		}
		
		private static EmailVo pendingEmailVo(String email) {
				EmailVo vo = new EmailVo();
				vo.setEmail(email);
				vo.setVerified(false);
				vo.setNextStepMessage("We have sent a verification link to your email address. Please check your inbox (and spam folder).");
				return vo;
		}
		
		/**
		 * query user email status
		 *
		 * @param userId user id
		 * @return Email vo
		 */
		public BaseVo<EmailVo> queryEmailStatus(String userId) {
				User foundUser = accountBaseService.selectByPK(userId);
				
				if (foundUser == null) {
						throw new BusinessException(ErrorMap.USER_NOT_FOUND);
				}
				
				String email = foundUser.getEmail();
				LocalDateTime emailVerifiedAt = foundUser.getEmailVerifiedAt();
				Boolean verified = emailVerifiedAt != null;
				String nextStep = null;
				
				if (email == null) {
						nextStep = "For your account security, please bind your email address.";
				} else if (emailVerifiedAt == null) {
						nextStep = "Please verify your email address.";
				} else {
						nextStep = "This email address is already verified.";
				}
				
				EmailVo vo = new EmailVo();
				vo.setEmail(email);
				vo.setVerified(verified);
				vo.setNextStepMessage(nextStep);
				return BaseVo.success(vo);
		}
}
