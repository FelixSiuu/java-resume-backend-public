package com.felixsiu.resume.service;

import com.felixsiu.resume.config.MailProperties;
import com.felixsiu.resume.exception.BusinessException;
import com.felixsiu.resume.model.EmailTokenMap;
import com.felixsiu.resume.model.ErrorMap;
import com.felixsiu.resume.model.entity.EmailToken;
import com.felixsiu.resume.service.emailtoken.EmailTokenBaseService;
import com.felixsiu.resume.utils.EmailTokenUtil;
import com.felixsiu.resume.utils.Sha256Hex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.NoSuchAlgorithmException;

@Slf4j
@Service
public class EmailTokenService {
		@Autowired
		private EmailTokenBaseService emailTokenBaseService;
		@Autowired
		private MailProperties mailProperties;
		@Autowired
		private JavaMailSender javaMailSender;
		
		/**
		 * find valid token hash (reset, verify, delete)
		 *
		 * @param rawToken      raw token
		 * @param emailTokenMap enum EmailTokenMap
		 * @return EmailToken
		 */
		public EmailToken findValidEmailToken(String rawToken, EmailTokenMap emailTokenMap) {
				if (!StringUtils.hasText(rawToken)) {
						throw new BusinessException(ErrorMap.TOKEN_EMPTY);
				}
				
				try {
						String tokenHash = Sha256Hex.toHexString(rawToken);
						
						EmailToken validTokenHash = emailTokenBaseService.findByValidHash(
										tokenHash,
										emailTokenMap.getName()
						);
						
						if (validTokenHash == null) {
								throw new BusinessException(ErrorMap.INVALID_TOKEN);
						}
						
						return validTokenHash;
						
				} catch (NoSuchAlgorithmException e) {
						throw new BusinessException(ErrorMap.INVALID_TOKEN);
				}
		}
		
		/**
		 * call JavaMailSender send email ( email verify & reset password & delete account )
		 *
		 * @param toEmail receiver
		 * @param userId  user id
		 */
		public void sendVerifyTokenEmail(String toEmail, String userId, EmailTokenMap emailTokenMap) {
				String purpose = emailTokenMap.getName();
				int unusedTokenCount = emailTokenBaseService.revokeUnusedByUserId(userId, purpose);
				log.info("clean up unused token count: {}", unusedTokenCount);
				
				SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
				
				try {
						String rawEmailToken;
						switch (emailTokenMap) {
								case DELETE:
										rawEmailToken = EmailTokenUtil.generateShortCode(6);
										break;
								default:
										rawEmailToken = EmailTokenUtil.generate();
						}
						String tokenHash = Sha256Hex.toHexString(rawEmailToken);
						
						boolean insertSuccess = emailTokenBaseService.insert(tokenHash, toEmail, userId, purpose);
						
						if (!insertSuccess) {
								throw new BusinessException(ErrorMap.EMAIL_CODE_FAILED);
						}
						
						String text, subject;
						switch (emailTokenMap) {
								case VERIFY:
										subject = "Email Verification 電子郵箱驗證";
										text = "Email Verification Link: " + mailProperties.getFrontendBaseUrl() + "/verify-email" + "?token=" + rawEmailToken + " The link expires in " + EmailTokenBaseService.tokenExpiresInMinutes + " minutes.";
										break;
								case RESET:
										subject = "Reset Password 重設密碼";
										text = "Reset Password Link: " + mailProperties.getFrontendBaseUrl() + "/reset-password" + "?token=" + rawEmailToken + " The link expires in " + EmailTokenBaseService.tokenExpiresInMinutes + " minutes.";
										break;
								case DELETE:
										subject = "Confirm Account Deletion 確認刪除帳號";
										text = "The verification code for this operation is【" + rawEmailToken + "】, This code expires in " + EmailTokenBaseService.tokenExpiresInMinutes + " minutes.";
										break;
								default:
										subject = "";
										text = "";
						}
						;
						
						simpleMailMessage.setFrom(mailProperties.getFrom());
						simpleMailMessage.setSubject(subject);
						simpleMailMessage.setTo(toEmail);
						simpleMailMessage.setText(text);
						
						javaMailSender.send(simpleMailMessage);
						
				} catch (NoSuchAlgorithmException e) {
						throw new BusinessException(ErrorMap.EMAIL_CODE_FAILED);
				}
		}
}
