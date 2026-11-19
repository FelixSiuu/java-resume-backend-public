package com.felixsiu.resume.service;

import com.felixsiu.resume.config.MailProperties;
import com.felixsiu.resume.exception.BusinessException;
import com.felixsiu.resume.model.ErrorMap;
import com.felixsiu.resume.model.EmailTokenMap;
import com.felixsiu.resume.model.dto.ChangePasswordDto;
import com.felixsiu.resume.model.dto.EmailDto;
import com.felixsiu.resume.model.entity.User;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.model.vo.EmailVo;
import com.felixsiu.resume.service.account.AccountBaseService;
import com.felixsiu.resume.utils.EmailTokenUtil;
import com.felixsiu.resume.utils.Sha256Hex;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@Slf4j
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
		private EmailTokenService emailTokenService;
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
		public void actionCallback(String userId, HttpServletResponse response) {
				cookieService.clearTokenCookies(response);
				
				int result = refreshTokenService.revokeAllByUserId(userId);
				if (result == 0) {
						log.warn("revoke all token failed for userId={}", userId);
				}
		}
		
		/**
		 * call JavaMailSender send email ( for email verification )
		 *
		 * @param toEmail receiver
		 * @param userId  user id
		 */
		public void sendVerifyTokenEmail(String toEmail, String userId, EmailTokenMap emailTokenMap) {
				String purpose = emailTokenMap.getName();
				int unusedTokenCount = emailTokenService.revokeUnusedByUserId(userId, purpose);
				log.info("clean up unused token count: {}", unusedTokenCount);
				
				SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
				
				try {
						String rawEmailToken = EmailTokenUtil.generate();
						String tokenHash = Sha256Hex.toHexString(rawEmailToken);
						
						boolean insertSuccess = emailTokenService.insert(tokenHash, toEmail, userId, purpose);
						
						if (!insertSuccess) {
								throw new BusinessException(ErrorMap.EMAIL_CODE_FAILED);
						}
						
						String frontEndPath, textPrefix, subject;
						switch (emailTokenMap) {
								case VERIFY:
										frontEndPath = "/verify-email";
										textPrefix = "Email Verification Link: ";
										subject = "Email Verification 電子郵箱驗證";
										break;
								case RESET:
										frontEndPath = "/reset-password";
										textPrefix = "Reset Password Link: ";
										subject = "Reset Password 重設密碼";
										break;
								default:
										frontEndPath = "";
										textPrefix = "";
										subject = "";
						}
						;
						
						String text = textPrefix + mailProperties.getFrontendBaseUrl() + frontEndPath + "?token=" + rawEmailToken + " The link expires in " + EmailTokenService.tokenExpiresInMinutes + " minutes.";
						simpleMailMessage.setSubject(subject);
						simpleMailMessage.setFrom(mailProperties.getFrom());
						simpleMailMessage.setTo(toEmail);
						simpleMailMessage.setText(text);
						
						javaMailSender.send(simpleMailMessage);
						
				} catch (NoSuchAlgorithmException e) {
						throw new BusinessException(ErrorMap.EMAIL_CODE_FAILED);
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
						throw new BusinessException(ErrorMap.USER_NOT_FOUND);
				}
				
				String username = user.getUsername();
				LocalDateTime now = LocalDateTime.now();
				
				boolean deleteSuccess = accountBaseService.setAccountDeleteTime(userId, username, now);
				
				if (!deleteSuccess) {
						throw new BusinessException(ErrorMap.DELETE_FAILED);
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
						sendVerifyTokenEmail(newEmail, userId, EmailTokenMap.VERIFY);
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
				
				sendVerifyTokenEmail(newEmail, userId, EmailTokenMap.VERIFY);
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
