package com.felixsiu.resume.service;

import com.felixsiu.resume.config.MailProperties;
import com.felixsiu.resume.model.EmailTokenMap;
import com.felixsiu.resume.model.dto.ChangePasswordDto;
import com.felixsiu.resume.model.dto.EmailDto;
import com.felixsiu.resume.model.entity.User;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.model.vo.EmailVo;
import com.felixsiu.resume.service.account.AccountBaseService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
		@Mock
		private AccountBaseService accountBaseService;
		@Mock
		private BCryptPasswordEncoder passwordEncoder;
		@Mock
		private CookieService cookieService;
		@Mock
		private RefreshTokenService refreshTokenService;
		@Mock
		private EmailTokenService emailTokenService;
		@Mock
		private MailProperties mailProperties;
		@Mock
		private JavaMailSender javaMailSender;
		@Mock
		private HttpServletResponse response;
		@InjectMocks
		private AccountService accountService;
		
		private final String purposeVerifyEmail = EmailTokenMap.VERIFY.getName();
		
		private User createUser(String id, String username, String encodedPassword) {
				User user = new User();
				user.setId(id);
				user.setUsername(username);
				user.setPassword(encodedPassword);
				return user;
		}
		
		private void stubMailForSend() {
				when(mailProperties.getFrom()).thenReturn("noreply@example.com");
				when(mailProperties.getFrontendBaseUrl()).thenReturn("https://admin.example.com");
		}
		
		// ============================================================
		// deleteAccount
		// ============================================================
		
		@Test
		void deleteAccount_shouldSuccess_andRevokeTokens() {
				String userId = "uuid-123";
				User user = createUser(userId, "felix", "encoded");
				
				when(accountBaseService.selectByPK(userId)).thenReturn(user);
				when(accountBaseService.setAccountDeleteTime(eq(userId), eq("felix"), any(LocalDateTime.class))).thenReturn(true);
				when(refreshTokenService.revokeAllByUserId(userId)).thenReturn(1);
				
				BaseVo<Void> result = accountService.deleteAccount(userId, response);
				
				assertThat(result.getIsSuccess()).isTrue();
				verify(accountBaseService).setAccountDeleteTime(eq(userId), eq("felix"), any(LocalDateTime.class));
				verify(cookieService).clearTokenCookies(response);
				verify(refreshTokenService).revokeAllByUserId(userId);
		}
		
		@Test
		void deleteAccount_shouldThrow_whenUserNotFound() {
				String userId = "missing";
				when(accountBaseService.selectByPK(userId)).thenReturn(null);
				
				assertThatThrownBy(() -> accountService.deleteAccount(userId, response))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("找不到該用戶");
				
				verify(accountBaseService, never()).setAccountDeleteTime(anyString(), anyString(), any());
				verify(cookieService, never()).clearTokenCookies(any());
				verify(refreshTokenService, never()).revokeAllByUserId(anyString());
		}
		
		@Test
		void deleteAccount_shouldThrow_whenSetDeleteTimeFails() {
				String userId = "uuid-123";
				User user = createUser(userId, "felix", "encoded");
				
				when(accountBaseService.selectByPK(userId)).thenReturn(user);
				when(accountBaseService.setAccountDeleteTime(eq(userId), eq("felix"), any(LocalDateTime.class))).thenReturn(false);
				
				assertThatThrownBy(() -> accountService.deleteAccount(userId, response))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("刪除失敗");
				
				verify(cookieService, never()).clearTokenCookies(any());
				verify(refreshTokenService, never()).revokeAllByUserId(anyString());
		}
		
		@Test
		void deleteAccount_shouldStillSucceed_whenRevokeReturnsZero() {
				String userId = "uuid-123";
				User user = createUser(userId, "felix", "encoded");
				
				when(accountBaseService.selectByPK(userId)).thenReturn(user);
				when(accountBaseService.setAccountDeleteTime(eq(userId), eq("felix"), any(LocalDateTime.class))).thenReturn(true);
				when(refreshTokenService.revokeAllByUserId(userId)).thenReturn(0);
				
				BaseVo<Void> result = accountService.deleteAccount(userId, response);
				
				assertThat(result.getIsSuccess()).isTrue();
				verify(cookieService).clearTokenCookies(response);
				verify(refreshTokenService).revokeAllByUserId(userId);
		}
		
		// ============================================================
		// changePassword
		// ============================================================
		
		@Test
		void changePassword_shouldSuccess_andRevokeTokens() {
				String userId = "uuid-123";
				User user = createUser(userId, "felix", "encoded-old");
				ChangePasswordDto dto = new ChangePasswordDto("oldPass12", "newPass34");
				
				when(accountBaseService.selectByPK(userId)).thenReturn(user);
				when(passwordEncoder.matches("oldPass12", "encoded-old")).thenReturn(true);
				when(passwordEncoder.encode("newPass34")).thenReturn("encoded-new");
				when(accountBaseService.updatePasswordByPK(userId, "encoded-new")).thenReturn(true);
				when(refreshTokenService.revokeAllByUserId(userId)).thenReturn(1);
				
				BaseVo<Void> result = accountService.changePassword(userId, dto, response);
				
				assertThat(result.getIsSuccess()).isTrue();
				verify(accountBaseService).updatePasswordByPK(userId, "encoded-new");
				verify(cookieService).clearTokenCookies(response);
				verify(refreshTokenService).revokeAllByUserId(userId);
		}
		
		@Test
		void changePassword_shouldThrow_whenUserNotFound() {
				String userId = "missing";
				ChangePasswordDto dto = new ChangePasswordDto("oldPass12", "newPass34");
				when(accountBaseService.selectByPK(userId)).thenReturn(null);
				
				assertThatThrownBy(() -> accountService.changePassword(userId, dto, response))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("找不到該用戶");
				
				verify(passwordEncoder, never()).matches(anyString(), anyString());
				verify(accountBaseService, never()).updatePasswordByPK(anyString(), anyString());
				verify(cookieService, never()).clearTokenCookies(any());
		}
		
		@Test
		void changePassword_shouldThrow_whenOldPasswordMismatch() {
				String userId = "uuid-123";
				User user = createUser(userId, "felix", "encoded-old");
				ChangePasswordDto dto = new ChangePasswordDto("wrongPass1", "newPass34");
				
				when(accountBaseService.selectByPK(userId)).thenReturn(user);
				when(passwordEncoder.matches("wrongPass1", "encoded-old")).thenReturn(false);
				
				assertThatThrownBy(() -> accountService.changePassword(userId, dto, response))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("密碼錯誤");
				
				verify(accountBaseService, never()).updatePasswordByPK(anyString(), anyString());
				verify(cookieService, never()).clearTokenCookies(any());
		}
		
		@Test
		void changePassword_shouldThrow_whenNewPasswordSameAsOld() {
				String userId = "uuid-123";
				User user = createUser(userId, "felix", "encoded-old");
				ChangePasswordDto dto = new ChangePasswordDto("samePass1", "samePass1");
				
				when(accountBaseService.selectByPK(userId)).thenReturn(user);
				when(passwordEncoder.matches("samePass1", "encoded-old")).thenReturn(true);
				
				assertThatThrownBy(() -> accountService.changePassword(userId, dto, response))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("新舊密碼一致");
				
				verify(passwordEncoder, never()).encode(anyString());
				verify(accountBaseService, never()).updatePasswordByPK(anyString(), anyString());
				verify(cookieService, never()).clearTokenCookies(any());
		}
		
		@Test
		void changePassword_shouldThrow_whenUpdateFails() {
				String userId = "uuid-123";
				User user = createUser(userId, "felix", "encoded-old");
				ChangePasswordDto dto = new ChangePasswordDto("oldPass12", "newPass34");
				
				when(accountBaseService.selectByPK(userId)).thenReturn(user);
				when(passwordEncoder.matches("oldPass12", "encoded-old")).thenReturn(true);
				when(passwordEncoder.encode("newPass34")).thenReturn("encoded-new");
				when(accountBaseService.updatePasswordByPK(userId, "encoded-new")).thenReturn(false);
				
				assertThatThrownBy(() -> accountService.changePassword(userId, dto, response))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("更新密碼失敗");
				
				verify(cookieService, never()).clearTokenCookies(any());
				verify(refreshTokenService, never()).revokeAllByUserId(anyString());
		}
		
		// ============================================================
		// updateEmail
		// ============================================================
		
		@Test
		void updateEmail_shouldThrow_whenUserNotFound() {
				String userId = "missing";
				EmailDto dto = new EmailDto("new@example.com");
				when(accountBaseService.selectByPK(userId)).thenReturn(null);
				
				assertThatThrownBy(() -> accountService.updateEmail(userId, dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("找不到該用戶");
				
				verify(accountBaseService, never()).countEmail(anyString());
				verify(accountBaseService, never()).updateEmailByPK(anyString(), anyString(), any());
				verify(emailTokenService, never()).insert(anyString(), anyString(), anyString(), anyString());
				verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
		}
		
		@Test
		void updateEmail_shouldBindAndSend_whenFirstBind() {
				String userId = "uuid-123";
				String email = "new@example.com";
				User user = createUser(userId, "felix", "encoded");
				user.setEmail(null);
				user.setEmailVerifiedAt(null);
				EmailDto dto = new EmailDto(email);
				
				when(accountBaseService.selectByPK(userId)).thenReturn(user);
				when(accountBaseService.countEmail(email)).thenReturn(0L);
				when(accountBaseService.updateEmailByPK(userId, email, null)).thenReturn(true);
				when(emailTokenService.revokeUnusedByUserId(userId, purposeVerifyEmail)).thenReturn(0);
				when(emailTokenService.insert(anyString(), eq(email), eq(userId), eq(purposeVerifyEmail))).thenReturn(true);
				stubMailForSend();
				
				BaseVo<EmailVo> result = accountService.updateEmail(userId, dto);
				
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData().getEmail()).isEqualTo(email);
				assertThat(result.getData().getVerified()).isFalse();
				assertThat(result.getData().getNextStepMessage()).isNotBlank();
				verify(accountBaseService).updateEmailByPK(userId, email, null);
				
				InOrder inOrder = inOrder(emailTokenService, javaMailSender);
				inOrder.verify(emailTokenService).revokeUnusedByUserId(userId, purposeVerifyEmail);
				inOrder.verify(emailTokenService).insert(anyString(), eq(email), eq(userId), eq(purposeVerifyEmail));
				inOrder.verify(javaMailSender).send(any(SimpleMailMessage.class));
				
				ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
				verify(javaMailSender).send(mailCaptor.capture());
				assertThat(mailCaptor.getValue().getTo()).containsExactly(email);
				assertThat(mailCaptor.getValue().getFrom()).isEqualTo("noreply@example.com");
		}
		
		@Test
		void updateEmail_shouldResend_whenSameUnverifiedEmail() {
				String userId = "uuid-123";
				String email = "pending@example.com";
				User user = createUser(userId, "felix", "encoded");
				user.setEmail(email);
				user.setEmailVerifiedAt(null);
				EmailDto dto = new EmailDto(email);
				
				when(accountBaseService.selectByPK(userId)).thenReturn(user);
				when(emailTokenService.revokeUnusedByUserId(userId, purposeVerifyEmail)).thenReturn(1);
				when(emailTokenService.insert(anyString(), eq(email), eq(userId), eq(purposeVerifyEmail))).thenReturn(true);
				stubMailForSend();
				
				BaseVo<EmailVo> result = accountService.updateEmail(userId, dto);
				
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData().getEmail()).isEqualTo(email);
				assertThat(result.getData().getVerified()).isFalse();
				verify(accountBaseService, never()).countEmail(anyString());
				verify(accountBaseService, never()).updateEmailByPK(anyString(), anyString(), any());
				
				InOrder inOrder = inOrder(emailTokenService);
				inOrder.verify(emailTokenService).revokeUnusedByUserId(userId, purposeVerifyEmail);
				inOrder.verify(emailTokenService).insert(anyString(), eq(email), eq(userId), eq(purposeVerifyEmail));
				verify(javaMailSender).send(any(SimpleMailMessage.class));
		}
		
		@Test
		void updateEmail_shouldThrow_whenSameEmailAlreadyVerified() {
				String userId = "uuid-123";
				String email = "verified@example.com";
				User user = createUser(userId, "felix", "encoded");
				user.setEmail(email);
				user.setEmailVerifiedAt(LocalDateTime.now());
				EmailDto dto = new EmailDto(email);
				
				when(accountBaseService.selectByPK(userId)).thenReturn(user);
				
				assertThatThrownBy(() -> accountService.updateEmail(userId, dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("已驗證");
				
				verify(accountBaseService, never()).countEmail(anyString());
				verify(accountBaseService, never()).updateEmailByPK(anyString(), anyString(), any());
				verify(emailTokenService, never()).insert(anyString(), anyString(), anyString(), anyString());
				verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
		}
		
		@Test
		void updateEmail_shouldThrow_whenEmailTaken() {
				String userId = "uuid-123";
				String email = "taken@example.com";
				User user = createUser(userId, "felix", "encoded");
				user.setEmail(null);
				EmailDto dto = new EmailDto(email);
				
				when(accountBaseService.selectByPK(userId)).thenReturn(user);
				when(accountBaseService.countEmail(email)).thenReturn(1L);
				
				assertThatThrownBy(() -> accountService.updateEmail(userId, dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("已被佔用");
				
				verify(accountBaseService, never()).updateEmailByPK(anyString(), anyString(), any());
				verify(emailTokenService, never()).insert(anyString(), anyString(), anyString(), anyString());
				verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
		}
		
		@Test
		void updateEmail_shouldThrow_whenUpdateFails() {
				String userId = "uuid-123";
				String email = "new@example.com";
				User user = createUser(userId, "felix", "encoded");
				user.setEmail(null);
				EmailDto dto = new EmailDto(email);
				
				when(accountBaseService.selectByPK(userId)).thenReturn(user);
				when(accountBaseService.countEmail(email)).thenReturn(0L);
				when(accountBaseService.updateEmailByPK(userId, email, null)).thenReturn(false);
				
				assertThatThrownBy(() -> accountService.updateEmail(userId, dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("更新電子郵箱失敗");
				
				verify(emailTokenService, never()).insert(anyString(), anyString(), anyString(), anyString());
				verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
		}
		
		@Test
		void updateEmail_shouldThrow_whenTokenInsertFails() {
				String userId = "uuid-123";
				String email = "new@example.com";
				User user = createUser(userId, "felix", "encoded");
				user.setEmail(null);
				EmailDto dto = new EmailDto(email);
				
				when(accountBaseService.selectByPK(userId)).thenReturn(user);
				when(accountBaseService.countEmail(email)).thenReturn(0L);
				when(accountBaseService.updateEmailByPK(userId, email, null)).thenReturn(true);
				when(emailTokenService.insert(anyString(), eq(email), eq(userId), eq(purposeVerifyEmail))).thenReturn(false);
				
				assertThatThrownBy(() -> accountService.updateEmail(userId, dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("獲取電子郵箱驗證碼失敗");
				
				verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
		}
		
		@Test
		void updateEmail_shouldChangeAndSend_whenDifferentUnverifiedEmail() {
				String userId = "uuid-123";
				String oldEmail = "old@example.com";
				String newEmail = "new@example.com";
				User user = createUser(userId, "felix", "encoded");
				user.setEmail(oldEmail);
				user.setEmailVerifiedAt(null);
				EmailDto dto = new EmailDto(newEmail);
				
				when(accountBaseService.selectByPK(userId)).thenReturn(user);
				when(accountBaseService.countEmail(newEmail)).thenReturn(0L);
				when(accountBaseService.updateEmailByPK(userId, newEmail, null)).thenReturn(true);
				when(emailTokenService.revokeUnusedByUserId(userId, purposeVerifyEmail)).thenReturn(1);
				when(emailTokenService.insert(anyString(), eq(newEmail), eq(userId), eq(purposeVerifyEmail))).thenReturn(true);
				stubMailForSend();
				
				BaseVo<EmailVo> result = accountService.updateEmail(userId, dto);
				
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData().getEmail()).isEqualTo(newEmail);
				assertThat(result.getData().getVerified()).isFalse();
				verify(accountBaseService).updateEmailByPK(userId, newEmail, null);
				verify(emailTokenService).revokeUnusedByUserId(userId, purposeVerifyEmail);
				verify(emailTokenService).insert(anyString(), eq(newEmail), eq(userId), eq(purposeVerifyEmail));
				verify(javaMailSender).send(any(SimpleMailMessage.class));
		}
		
		// ============================================================
		// queryEmailStatus
		// ============================================================
		
		@Test
		void queryEmailStatus_shouldThrow_whenUserNotFound() {
				String userId = "missing";
				when(accountBaseService.selectByPK(userId)).thenReturn(null);
				
				assertThatThrownBy(() -> accountService.queryEmailStatus(userId))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("找不到該用戶");
		}
		
		@Test
		void queryEmailStatus_shouldReturnBindHint_whenEmailNull() {
				String userId = "uuid-123";
				User user = createUser(userId, "felix", "encoded");
				user.setEmail(null);
				user.setEmailVerifiedAt(null);
				
				when(accountBaseService.selectByPK(userId)).thenReturn(user);
				
				BaseVo<EmailVo> result = accountService.queryEmailStatus(userId);
				
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData().getEmail()).isNull();
				assertThat(result.getData().getVerified()).isFalse();
				assertThat(result.getData().getNextStepMessage())
								.contains("please bind your email address");
		}
		
		@Test
		void queryEmailStatus_shouldReturnVerifyHint_whenEmailPending() {
				String userId = "uuid-123";
				String email = "pending@example.com";
				User user = createUser(userId, "felix", "encoded");
				user.setEmail(email);
				user.setEmailVerifiedAt(null);
				
				when(accountBaseService.selectByPK(userId)).thenReturn(user);
				
				BaseVo<EmailVo> result = accountService.queryEmailStatus(userId);
				
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData().getEmail()).isEqualTo(email);
				assertThat(result.getData().getVerified()).isFalse();
				assertThat(result.getData().getNextStepMessage())
								.contains("Please verify your email address");
		}
		
		@Test
		void queryEmailStatus_shouldReturnVerified_whenEmailVerified() {
				String userId = "uuid-123";
				String email = "verified@example.com";
				User user = createUser(userId, "felix", "encoded");
				user.setEmail(email);
				user.setEmailVerifiedAt(LocalDateTime.now());
				
				when(accountBaseService.selectByPK(userId)).thenReturn(user);
				
				BaseVo<EmailVo> result = accountService.queryEmailStatus(userId);
				
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData().getEmail()).isEqualTo(email);
				assertThat(result.getData().getVerified()).isTrue();
				assertThat(result.getData().getNextStepMessage())
								.contains("already verified");
		}
}
