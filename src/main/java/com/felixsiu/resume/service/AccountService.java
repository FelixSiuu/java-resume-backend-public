package com.felixsiu.resume.service;

import com.felixsiu.resume.config.MailProperties;
import com.felixsiu.resume.model.dto.ChangePasswordDto;
import com.felixsiu.resume.model.dto.EmailDto;
import com.felixsiu.resume.model.entity.User;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.model.vo.EmailVo;
import com.felixsiu.resume.service.account.AccountBaseService;
import com.felixsiu.resume.utils.EmailTokenUtil;
import com.felixsiu.resume.utils.Sha256Hex;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.NoSuchAlgorithmException;
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
		@Autowired
		private EmailVerificationTokenService emailVerificationTokenService;
		@Autowired
		private MailProperties mailProperties;
		@Autowired
		private JavaMailSender javaMailSender;
		
		/**
		 * clean 3 tokens & revoke db refresh token
		 *
		 * @param userId   user id
		 * @param response HttpServletResponse
		 */
		void actionCallback(String userId, HttpServletResponse response) {
				cookieService.clearTokenCookies(response);
				
				int result = refreshTokenService.revokeAllByUserId(userId);
				if (result == 0) {
						System.err.println("revoke all token failed");
				}
		}
		
		/**
		 * call JavaMailSender verify email
		 *
		 * @param toEmail receiver
		 * @param userId  user id
		 */
		public void sendVerifyTokenEmail(String toEmail, String userId) {
				int unusedTokenCount = emailVerificationTokenService.revokeUnusedByUserId(userId);
				System.out.println("clean up unused token count: " + unusedTokenCount);
				
				try {
						String rawEmailToken = EmailTokenUtil.generate();
						String tokenHash = Sha256Hex.toHexString(rawEmailToken);
						boolean insertSuccess = emailVerificationTokenService.insert(tokenHash, toEmail, userId);
						if (!insertSuccess) {
								throw new RuntimeException("獲取電子郵箱驗證碼失敗");
						}
						
						String text = "Email Verification Link: " + mailProperties.getFrontendBaseUrl() + "/verify-email?token=" + rawEmailToken + " The link expires in " + EmailVerificationTokenService.tokenExpiresInMinutes + " minutes.";
						SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
						simpleMailMessage.setFrom(mailProperties.getFrom());
						simpleMailMessage.setTo(toEmail);
						simpleMailMessage.setSubject("Email Verification 電子郵箱驗證");
						simpleMailMessage.setText(text);
						javaMailSender.send(simpleMailMessage);
						
				} catch (NoSuchAlgorithmException e) {
						throw new RuntimeException("獲取電子郵箱驗證碼失敗");
				}
		}
		
		/**
		 * set account delete time,
		 *
		 * @param userId   user id
		 * @param response HttpServletResponse
		 * @return void
		 */
		@Transactional(rollbackFor = Exception.class)
		public BaseVo<Void> deleteAccount(String userId, HttpServletResponse response) {
				User user = accountBaseService.selectByPK(userId);
				
				if (user == null) {
						throw new RuntimeException("找不到該用戶");
				}
				
				String username = user.getUsername();
				LocalDateTime now = LocalDateTime.now();
				
				boolean deleteSuccess = accountBaseService.setAccountDeleteTime(userId, username, now);
				
				if (!deleteSuccess) {
						throw new RuntimeException("刪除失敗");
				}
				
				actionCallback(userId, response);
				
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
						throw new RuntimeException("找不到該用戶");
				}
				
				String rawOldPassword = dto.oldPassword();
				String rawNewPassword = dto.newPassword();
				
				boolean isPwdMatch = passwordEncoder.matches(rawOldPassword, user.getPassword());
				
				if (!isPwdMatch) {
						throw new RuntimeException("密碼錯誤，請重新輸入");
				}
				
				if (rawOldPassword.equals(rawNewPassword)) {
						throw new RuntimeException("新舊密碼一致，請重新輸入");
				}
				
				String newPassword = passwordEncoder.encode(rawNewPassword);
				
				boolean updateSuccess = accountBaseService.updatePasswordByPK(userId, newPassword);
				
				if (!updateSuccess) {
						throw new RuntimeException("更新密碼失敗");
				}
				
				actionCallback(userId, response);
				
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
						throw new RuntimeException("找不到該用戶");
				}
				
				String newEmail = dto.email();
				String existingEmail = foundUser.getEmail();
				LocalDateTime existingEmailVerifiedAt = foundUser.getEmailVerifiedAt();
				
				if (newEmail.equals(existingEmail)) {
						if (existingEmailVerifiedAt != null) {
								throw new RuntimeException("該電子郵箱已驗證，無需重複綁定");
						}
						// same pending email → resend verification
						sendVerifyTokenEmail(newEmail, userId);
						return BaseVo.success(pendingEmailVo(newEmail));
				}
				
				Long thisEmailCount = accountBaseService.countEmail(newEmail);
				
				if (thisEmailCount > 0) {
						throw new RuntimeException("該電子郵箱已被佔用");
				}
				
				boolean updateSuccess = accountBaseService.updateEmailByPK(userId, newEmail, null);
				
				if (!updateSuccess) {
						throw new RuntimeException("更新電子郵箱失敗");
				}
				
				sendVerifyTokenEmail(newEmail, userId);
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
						throw new RuntimeException("找不到該用戶");
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
