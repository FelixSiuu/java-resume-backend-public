package com.felixsiu.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.felixsiu.resume.config.CookieProperties;
import com.felixsiu.resume.model.dto.LoginDto;
import com.felixsiu.resume.model.dto.RegisterDto;
import com.felixsiu.resume.model.entity.RefreshToken;
import com.felixsiu.resume.model.entity.User;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.model.vo.LoginVo;
import com.felixsiu.resume.respository.UserMapper;
import com.felixsiu.resume.utils.JwtUtil;
import com.felixsiu.resume.utils.Sha256Hex;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
		@Mock
		private UserMapper userMapper;
		@Mock
		private DefaultKaptcha defaultKaptcha;
		@Mock
		private BCryptPasswordEncoder passwordEncoder;
		@Mock
		private JwtUtil jwtUtil;
		@Mock
		private HttpSession httpSession;
		@Mock
		private HttpServletRequest httpServletRequest;
		@Mock
		private HttpServletResponse httpServletResponse;
		@Mock
		private CookieProperties cookieProperties;
		@Mock
		private RefreshTokenService refreshTokenService;
		@Mock
		private CookieService cookieService;
		@InjectMocks
		private AuthService authService;
		
		// ============================================================
		// 測試數據工廠
		// ============================================================
		
		private RegisterDto createRegisterDto(String username, String password) {
				return new RegisterDto(username, password);
		}
		
		private LoginDto createLoginDto(String username, String password, String captcha, Boolean rememberMe) {
				return new LoginDto(username, password, captcha, rememberMe);
		}
		
		private User createUser(String id, String username, String encodedPassword) {
				return createUser(id, username, encodedPassword, null);
		}
		
		private User createUser(String id, String username, String encodedPassword, LocalDateTime deletedAt) {
				User user = new User();
				user.setId(id);
				user.setUsername(username);
				user.setPassword(encodedPassword);
				user.setDeletedAt(deletedAt);
				return user;
		}
		
		private void stubRefreshCookie(String rawRefreshToken) {
				when(cookieProperties.getNameRefresh()).thenReturn("REFRESH_TOKEN");
				when(httpServletRequest.getCookies())
								.thenReturn(new Cookie[]{new Cookie("REFRESH_TOKEN", rawRefreshToken)});
		}
		
		private void stubActiveUser(String userId, String username) {
				when(userMapper.selectOne(any(LambdaQueryWrapper.class)))
								.thenReturn(createUser(userId, username, "encoded"));
		}
		
		private RefreshToken createRefreshToken(
						Integer id,
						String userId,
						String userName,
						String tokenHash,
						boolean revoked,
						boolean rememberMe,
						LocalDateTime expiresAt
		) {
				RefreshToken token = new RefreshToken();
				token.setId(id);
				token.setUserId(userId);
				token.setUserName(userName);
				token.setTokenHash(tokenHash);
				token.setRevoked(revoked);
				token.setRememberMe(rememberMe);
				token.setExpiresAt(expiresAt);
				return token;
		}
		
		// ============================================================
		// 1. register 測試
		// ============================================================
		
		@Test
		void register_shouldSuccess_whenUsernameNotExists() {
				// 準備
				RegisterDto dto = createRegisterDto("felix", "password123");
				String encodedPassword = "encoded_password_123";
				
				when(userMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
				when(passwordEncoder.encode("password123")).thenReturn(encodedPassword);
				when(userMapper.insert(any(User.class))).thenReturn(1);
				
				// 執行
				BaseVo<Void> result = authService.register(dto);
				
				// 驗證
				assertThat(result.getIsSuccess()).isTrue();
				
				// 驗證密碼被編碼
				verify(passwordEncoder).encode("password123");
				
				// 驗證 User 被正確儲存
				ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
				verify(userMapper).insert(captor.capture());
				User captured = captor.getValue();
				assertThat(captured.getUsername()).isEqualTo("felix");
				assertThat(captured.getPassword()).isEqualTo(encodedPassword);
		}
		
		@Test
		void register_shouldThrowException_whenUsernameAlreadyExists() {
				// 準備：用戶已存在
				RegisterDto dto = createRegisterDto("felix", "password123");
				
				when(userMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(true);
				
				// 執行 & 驗證
				assertThatThrownBy(() -> authService.register(dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("該用戶已存在");
				
				// 驗證：不應該進行插入
				verify(userMapper, never()).insert(any(User.class));
				verify(passwordEncoder, never()).encode(anyString());
		}
		
		@Test
		void register_shouldThrowException_whenInsertFails() {
				// 準備：插入失敗
				RegisterDto dto = createRegisterDto("felix", "password123");
				
				when(userMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
				when(passwordEncoder.encode(anyString())).thenReturn("encoded");
				when(userMapper.insert(any(User.class))).thenReturn(0);
				
				// 執行 & 驗證
				assertThatThrownBy(() -> authService.register(dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("創建帳號失敗");
		}
		
		// ============================================================
		// 2. login 測試
		// ============================================================
		
		@Test
		void login_shouldSuccess_whenValidCredentials() {
				LoginDto dto = createLoginDto("felix", "password123", "ABC123", true);
				String userId = "uuid-123";
				String encodedPassword = "encoded_password_123";
				String expectedToken = "jwt_token_xyz";
				Integer accessTtl = 3600;
				Integer refreshTtlShort = 28800;
				Integer refreshTtlLong = 604800;
				
				User mockUser = createUser(userId, "felix", encodedPassword);
				
				when(httpSession.getAttribute("captcha")).thenReturn("ABC123");
				when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(mockUser);
				when(passwordEncoder.matches("password123", encodedPassword)).thenReturn(true);
				when(jwtUtil.generateToken(userId, accessTtl)).thenReturn(expectedToken);
				when(cookieProperties.getAccessTtlSeconds()).thenReturn(accessTtl);
				when(cookieProperties.getRefreshTtlSecondsShort()).thenReturn(refreshTtlShort);
				when(cookieProperties.getRefreshTtlSecondsLong()).thenReturn(refreshTtlLong);
				when(refreshTokenService.insert(any())).thenReturn(true);
				
				BaseVo<LoginVo> result = authService.login(dto, httpSession, httpServletResponse);
				
				assertThat(result.getIsSuccess()).isTrue();
				LoginVo loginVo = result.getData();
				assertThat(loginVo.getUsername()).isEqualTo("felix");
				assertThat(loginVo.getXsrfToken()).isNotBlank();
				
				verify(jwtUtil).generateToken(userId, accessTtl);
				verify(refreshTokenService, times(1)).insert(any());
				// Cookie 已交給 CookieService，不再直接寫 response header
				verify(cookieService).writeTokenToCookie(
								eq(httpServletResponse),
								eq(expectedToken),
								eq(Duration.ofSeconds(accessTtl)),
								anyString(),
								eq(Duration.ofSeconds(refreshTtlLong)),
								anyString(),
								eq(Duration.ofSeconds(accessTtl))
				);
				verify(httpServletResponse, never()).addHeader(anyString(), anyString());
		}
		
		@Test
		void login_shouldThrowException_whenCaptchaMismatch() {
				// 準備：驗證碼錯誤
				LoginDto dto = createLoginDto("felix", "password123", "WRONG", false);
				
				when(httpSession.getAttribute("captcha")).thenReturn("ABC123");
				
				// 執行 & 驗證
				assertThatThrownBy(() -> authService.login(dto, httpSession, httpServletResponse))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("captcha 錯誤");
				
				// 驗證：不應該查詢用戶
				verify(userMapper, never()).selectOne(any(LambdaQueryWrapper.class));
				verify(jwtUtil, never()).generateToken(anyString(), anyInt());
		}
		
		@Test
		void login_shouldThrowException_whenCaptchaIsNull() {
				// 準備：Session 中沒有驗證碼（過期）
				LoginDto dto = createLoginDto("felix", "password123", "ABC123", false);
				
				when(httpSession.getAttribute("captcha")).thenReturn(null);
				
				// 執行 & 驗證
				assertThatThrownBy(() -> authService.login(dto, httpSession, httpServletResponse))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("captcha 錯誤");
		}
		
		@Test
		void login_shouldThrowException_whenUserNotFound() {
				// 準備：用戶不存在
				LoginDto dto = createLoginDto("unknown", "password123", "ABC123", false);
				
				when(httpSession.getAttribute("captcha")).thenReturn("ABC123");
				when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
				
				// 執行 & 驗證
				assertThatThrownBy(() -> authService.login(dto, httpSession, httpServletResponse))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("username 或 password錯誤");
		}
		
		@Test
		void login_shouldThrowException_whenPasswordMismatch() {
				// 準備：密碼錯誤
				LoginDto dto = createLoginDto("felix", "wrong_password", "ABC123", false);
				User mockUser = createUser("uuid-123", "felix", "encoded_password");
				
				when(httpSession.getAttribute("captcha")).thenReturn("ABC123");
				when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(mockUser);
				when(passwordEncoder.matches("wrong_password", "encoded_password")).thenReturn(false);
				
				// 執行 & 驗證
				assertThatThrownBy(() -> authService.login(dto, httpSession, httpServletResponse))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("username 或 password錯誤");
		}
		
		@Test
		void login_shouldThrowException_whenUserSoftDeleted() {
				LoginDto dto = createLoginDto("felix", "password123", "ABC123", false);
				User deletedUser = createUser("uuid-123", "felix", "encoded_password", LocalDateTime.now().minusDays(1));
				
				when(httpSession.getAttribute("captcha")).thenReturn("ABC123");
				when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(deletedUser);
				
				assertThatThrownBy(() -> authService.login(dto, httpSession, httpServletResponse))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("username 或 password錯誤");
				
				verify(passwordEncoder, never()).matches(anyString(), anyString());
				verify(refreshTokenService, never()).insert(any());
				verify(cookieService, never()).writeTokenToCookie(any(), any(), any(), any(), any(), any(), any());
		}
		
		// ============================================================
		// 3. createCaptcha 測試
		// ============================================================
		
		@Test
		@Disabled("ImageIO.write() 需要真實圖片，在單元測試中難以模擬")
		void createCaptcha_shouldSuccess() throws Exception {
				// 準備：模擬 Kaptcha 行為
				String captchaText = "ABC123";
				BufferedImage mockImage = mock(BufferedImage.class);
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				
				when(defaultKaptcha.createText()).thenReturn(captchaText);
				when(defaultKaptcha.createImage(captchaText)).thenReturn(mockImage);
				
				// 執行
				BaseVo<String> result = authService.createCaptcha(httpSession);
				
				// 驗證
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData()).startsWith("data:image/jpeg;base64,");
				assertThat(result.getData()).isNotEmpty();
				
				// 驗證 Session 儲存
				verify(httpSession).setAttribute("captcha", captchaText);
				verify(defaultKaptcha).createText();
				verify(defaultKaptcha).createImage(captchaText);
		}
		
		@Test
		@Disabled("ImageIO.write() 需要真實圖片，在單元測試中難以模擬")
		void createCaptcha_shouldHandleIOException_gracefully() throws Exception {
				// 準備：模擬 Kaptcha 行為
				String captchaText = "ABC123";
				BufferedImage mockImage = mock(BufferedImage.class);
				
				when(defaultKaptcha.createText()).thenReturn(captchaText);
				when(defaultKaptcha.createImage(captchaText)).thenReturn(mockImage);
				// 這裡不好模擬 ImageIO.write 拋出異常，但可以透過實際測試驗證
				
				// 執行
				BaseVo<String> result = authService.createCaptcha(httpSession);
				
				// 驗證
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData()).startsWith("data:image/jpeg;base64,");
		}
		
		@Test
		void logout_shouldSuccess() {
				when(httpServletRequest.getCookies()).thenReturn(null);
				
				BaseVo<Void> result = authService.logout(httpServletRequest, httpServletResponse);
				
				assertThat(result.getIsSuccess()).isTrue();
				verify(cookieService).clearTokenCookies(httpServletResponse);
				verify(refreshTokenService, never()).revokeIfActive(any(), any());
				verify(httpServletResponse, never()).addHeader(anyString(), anyString());
		}
		
		// ============================================================
		// 4. refreshToken 測試
		// ============================================================
		
		@Test
		void refresh_shouldFail401_whenNoRefreshCookie() {
				when(httpServletRequest.getCookies()).thenReturn(null);
				
				BaseVo<LoginVo> result = authService.refreshToken(httpServletRequest, httpServletResponse);
				
				assertThat(result.getIsSuccess()).isFalse();
				assertThat(result.getMessage()).contains("已過期");
				verify(httpServletResponse).setStatus(401);
				verify(refreshTokenService, never()).selectOneByHash(anyString());
				verify(cookieService, never()).writeTokenToCookie(any(), any(), any(), any(), any(), any(), any());
		}
		
		@Test
		void refresh_shouldFail401_whenHashNotFound() throws NoSuchAlgorithmException {
				String raw = "raw-refresh-missing";
				stubRefreshCookie(raw);
				when(refreshTokenService.selectOneByHash(Sha256Hex.toHexString(raw))).thenReturn(null);
				
				BaseVo<LoginVo> result = authService.refreshToken(httpServletRequest, httpServletResponse);
				
				assertThat(result.getIsSuccess()).isFalse();
				assertThat(result.getMessage()).contains("已過期");
				verify(httpServletResponse).setStatus(401);
				verify(refreshTokenService, never()).insert(any());
				verify(cookieService, never()).writeTokenToCookie(any(), any(), any(), any(), any(), any(), any());
		}
		
		@Test
		void refresh_shouldFail403_whenTokenAlreadyRevoked() throws NoSuchAlgorithmException {
				String raw = "raw-refresh-revoked";
				String userId = "uuid-123";
				stubRefreshCookie(raw);
				
				RefreshToken existing = createRefreshToken(
								1, userId, "felix", Sha256Hex.toHexString(raw),
								true, true, LocalDateTime.now().plusDays(1)
				);
				when(refreshTokenService.selectOneByHash(Sha256Hex.toHexString(raw))).thenReturn(existing);
				stubActiveUser(userId, "felix");
				when(refreshTokenService.revokeAllByUserId(userId)).thenReturn(1);
				
				BaseVo<LoginVo> result = authService.refreshToken(httpServletRequest, httpServletResponse);
				
				assertThat(result.getIsSuccess()).isFalse();
				assertThat(result.getMessage()).contains("未經授權");
				verify(httpServletResponse).setStatus(403);
				verify(refreshTokenService).revokeAllByUserId(userId);
				verify(refreshTokenService, never()).insert(any());
				verify(cookieService, never()).writeTokenToCookie(any(), any(), any(), any(), any(), any(), any());
		}
		
		@Test
		void refresh_shouldFail401_whenExpiresAtPassed() throws NoSuchAlgorithmException {
				String raw = "raw-refresh-expired";
				String userId = "uuid-123";
				stubRefreshCookie(raw);
				
				RefreshToken existing = createRefreshToken(
								1, userId, "felix", Sha256Hex.toHexString(raw),
								false, true, LocalDateTime.now().minusHours(1)
				);
				when(refreshTokenService.selectOneByHash(Sha256Hex.toHexString(raw))).thenReturn(existing);
				stubActiveUser(userId, "felix");
				
				BaseVo<LoginVo> result = authService.refreshToken(httpServletRequest, httpServletResponse);
				
				assertThat(result.getIsSuccess()).isFalse();
				assertThat(result.getMessage()).contains("已過期");
				verify(httpServletResponse).setStatus(401);
				verify(refreshTokenService, never()).insert(any());
				verify(cookieService, never()).writeTokenToCookie(any(), any(), any(), any(), any(), any(), any());
		}
		
		@Test
		void refresh_shouldThrow_whenUserNotFound() throws NoSuchAlgorithmException {
				String raw = "raw-refresh-no-user";
				stubRefreshCookie(raw);
				
				RefreshToken existing = createRefreshToken(
								1, "uuid-123", "felix", Sha256Hex.toHexString(raw),
								false, true, LocalDateTime.now().plusDays(1)
				);
				when(refreshTokenService.selectOneByHash(Sha256Hex.toHexString(raw))).thenReturn(existing);
				when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
				
				assertThatThrownBy(() -> authService.refreshToken(httpServletRequest, httpServletResponse))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("找不到該用戶");
				
				verify(refreshTokenService, never()).insert(any());
				verify(cookieService, never()).writeTokenToCookie(any(), any(), any(), any(), any(), any(), any());
		}
		
		@Test
		void refresh_shouldThrow_whenUserSoftDeleted() throws NoSuchAlgorithmException {
				String raw = "raw-refresh-deleted-user";
				String userId = "uuid-123";
				stubRefreshCookie(raw);
				
				RefreshToken existing = createRefreshToken(
								1, userId, "felix", Sha256Hex.toHexString(raw),
								false, true, LocalDateTime.now().plusDays(1)
				);
				when(refreshTokenService.selectOneByHash(Sha256Hex.toHexString(raw))).thenReturn(existing);
				when(userMapper.selectOne(any(LambdaQueryWrapper.class)))
								.thenReturn(createUser(userId, "felix", "encoded", LocalDateTime.now().minusDays(1)));
				
				assertThatThrownBy(() -> authService.refreshToken(httpServletRequest, httpServletResponse))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("找不到該用戶");
				
				verify(refreshTokenService, never()).insert(any());
				verify(cookieService, never()).writeTokenToCookie(any(), any(), any(), any(), any(), any(), any());
		}
		
		@Test
		void refresh_shouldSuccess_andRotate_whenRememberMeTrue() throws NoSuchAlgorithmException {
				String raw = "raw-refresh-valid";
				String userId = "uuid-123";
				String userName = "felix";
				Integer accessTtl = 1800;
				Integer oldId = 1;
				Integer newId = 99;
				LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(3600);
				
				stubRefreshCookie(raw);
				RefreshToken existing = createRefreshToken(
								oldId, userId, userName, Sha256Hex.toHexString(raw),
								false, true, expiresAt
				);
				when(refreshTokenService.selectOneByHash(Sha256Hex.toHexString(raw))).thenReturn(existing);
				stubActiveUser(userId, userName);
				when(refreshTokenService.insert(any())).thenAnswer(invocation -> {
						RefreshToken token = invocation.getArgument(0);
						token.setId(newId);
						return true;
				});
				when(refreshTokenService.revokeIfActive(oldId, newId)).thenReturn(true);
				when(cookieProperties.getAccessTtlSeconds()).thenReturn(accessTtl);
				when(jwtUtil.generateToken(userId, accessTtl)).thenReturn("new-access-token");
				
				BaseVo<LoginVo> result = authService.refreshToken(httpServletRequest, httpServletResponse);
				
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData().getId()).isEqualTo(userId);
				assertThat(result.getData().getUsername()).isEqualTo(userName);
				assertThat(result.getData().getXsrfToken()).isNotBlank();
				
				ArgumentCaptor<RefreshToken> insertCaptor = ArgumentCaptor.forClass(RefreshToken.class);
				verify(refreshTokenService).insert(insertCaptor.capture());
				RefreshToken inserted = insertCaptor.getValue();
				assertThat(inserted.getExpiresAt()).isEqualTo(expiresAt);
				assertThat(inserted.getUserId()).isEqualTo(userId);
				assertThat(inserted.getRememberMe()).isTrue();
				assertThat(inserted.getTokenHash()).isNotEqualTo(existing.getTokenHash());
				
				verify(refreshTokenService).revokeIfActive(oldId, newId);
				verify(cookieService).writeTokenToCookie(
								eq(httpServletResponse),
								eq("new-access-token"),
								eq(Duration.ofSeconds(accessTtl)),
								anyString(),
								argThat(ttl -> ttl != null && ttl.getSeconds() >= 3500 && ttl.getSeconds() <= 3600),
								anyString(),
								eq(Duration.ofSeconds(accessTtl))
				);
				verify(httpServletResponse, never()).setStatus(anyInt());
		}
		
		@Test
		void refresh_shouldSuccess_withSessionRefreshCookie_whenRememberMeFalse() throws NoSuchAlgorithmException {
				String raw = "raw-refresh-session";
				String userId = "uuid-123";
				Integer accessTtl = 1800;
				Integer oldId = 1;
				Integer newId = 99;
				LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(3600);
				
				stubRefreshCookie(raw);
				RefreshToken existing = createRefreshToken(
								oldId, userId, "felix", Sha256Hex.toHexString(raw),
								false, false, expiresAt
				);
				when(refreshTokenService.selectOneByHash(Sha256Hex.toHexString(raw))).thenReturn(existing);
				stubActiveUser(userId, "felix");
				when(refreshTokenService.insert(any())).thenAnswer(invocation -> {
						RefreshToken token = invocation.getArgument(0);
						token.setId(newId);
						return true;
				});
				when(refreshTokenService.revokeIfActive(oldId, newId)).thenReturn(true);
				when(cookieProperties.getAccessTtlSeconds()).thenReturn(accessTtl);
				when(jwtUtil.generateToken(userId, accessTtl)).thenReturn("new-access-token");
				
				BaseVo<LoginVo> result = authService.refreshToken(httpServletRequest, httpServletResponse);
				
				assertThat(result.getIsSuccess()).isTrue();
				verify(cookieService).writeTokenToCookie(
								eq(httpServletResponse),
								eq("new-access-token"),
								eq(Duration.ofSeconds(accessTtl)),
								anyString(),
								isNull(),
								anyString(),
								eq(Duration.ofSeconds(accessTtl))
				);
		}
		
		@Test
		void refresh_shouldFail403_whenRevokeIfActiveLosesRace() throws NoSuchAlgorithmException {
				String raw = "raw-refresh-race";
				String userId = "uuid-123";
				Integer oldId = 1;
				Integer newId = 99;
				
				stubRefreshCookie(raw);
				RefreshToken existing = createRefreshToken(
								oldId, userId, "felix", Sha256Hex.toHexString(raw),
								false, true, LocalDateTime.now().plusDays(1)
				);
				when(refreshTokenService.selectOneByHash(Sha256Hex.toHexString(raw))).thenReturn(existing);
				stubActiveUser(userId, "felix");
				when(refreshTokenService.insert(any())).thenAnswer(invocation -> {
						RefreshToken token = invocation.getArgument(0);
						token.setId(newId);
						return true;
				});
				when(refreshTokenService.revokeIfActive(oldId, newId)).thenReturn(false);
				when(refreshTokenService.revokeAllByUserId(userId)).thenReturn(2);
				
				BaseVo<LoginVo> result = authService.refreshToken(httpServletRequest, httpServletResponse);
				
				assertThat(result.getIsSuccess()).isFalse();
				assertThat(result.getMessage()).contains("未經授權");
				verify(httpServletResponse).setStatus(403);
				verify(refreshTokenService).revokeAllByUserId(userId);
				verify(cookieService, never()).writeTokenToCookie(any(), any(), any(), any(), any(), any(), any());
		}
		
		@Test
		void refresh_shouldThrow_whenInsertFails() throws NoSuchAlgorithmException {
				String raw = "raw-refresh-insert-fail";
				String userId = "uuid-123";
				stubRefreshCookie(raw);
				
				RefreshToken existing = createRefreshToken(
								1, userId, "felix", Sha256Hex.toHexString(raw),
								false, true, LocalDateTime.now().plusDays(1)
				);
				when(refreshTokenService.selectOneByHash(Sha256Hex.toHexString(raw))).thenReturn(existing);
				stubActiveUser(userId, "felix");
				when(refreshTokenService.insert(any())).thenReturn(false);
				
				assertThatThrownBy(() -> authService.refreshToken(httpServletRequest, httpServletResponse))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("new refresh token 寫入失敗");
				
				verify(refreshTokenService, never()).revokeIfActive(any(), any());
				verify(cookieService, never()).writeTokenToCookie(any(), any(), any(), any(), any(), any(), any());
		}
		
		// ============================================================
		// 5. getUserId 測試
		// ============================================================
		
		@Test
		void getUserId_shouldReturnId_whenUserActive() {
				User user = createUser("uuid-123", "felix", "encoded");
				when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
				
				String result = authService.getUserId("felix");
				
				assertThat(result).isEqualTo("uuid-123");
		}
		
		@Test
		void getUserId_shouldThrow_whenUsernameBlank() {
				assertThatThrownBy(() -> authService.getUserId("  "))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("請輸入用戶名");
				
				verify(userMapper, never()).selectOne(any(LambdaQueryWrapper.class));
		}
		
		@Test
		void getUserId_shouldThrow_whenUserNotFound() {
				when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
				
				assertThatThrownBy(() -> authService.getUserId("unknown"))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("用戶不存在");
		}
		
		@Test
		void getUserId_shouldThrow_whenUserSoftDeleted() {
				User deletedUser = createUser("uuid-123", "felix", "encoded", LocalDateTime.now().minusDays(1));
				when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(deletedUser);
				
				assertThatThrownBy(() -> authService.getUserId("felix"))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("用戶不存在");
		}
}