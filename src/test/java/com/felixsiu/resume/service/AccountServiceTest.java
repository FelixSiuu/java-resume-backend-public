package com.felixsiu.resume.service;

import com.felixsiu.resume.model.dto.ChangePasswordDto;
import com.felixsiu.resume.model.entity.User;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.service.account.AccountBaseService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
		private HttpServletResponse response;
		@InjectMocks
		private AccountService accountService;
		
		private User createUser(String id, String username, String encodedPassword) {
				User user = new User();
				user.setId(id);
				user.setUsername(username);
				user.setPassword(encodedPassword);
				return user;
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
}
