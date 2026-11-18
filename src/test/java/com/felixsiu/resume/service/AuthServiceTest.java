package com.felixsiu.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.felixsiu.resume.config.CookieProperties;
import com.felixsiu.resume.model.EmailTokenMap;
import com.felixsiu.resume.model.dto.*;
import com.felixsiu.resume.model.entity.EmailToken;
import com.felixsiu.resume.model.entity.RefreshToken;
import com.felixsiu.resume.model.entity.User;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.model.vo.CoolDownVo;
import com.felixsiu.resume.model.vo.LoginVo;
import com.felixsiu.resume.respository.UserMapper;
import com.felixsiu.resume.service.account.AccountBaseService;
import com.felixsiu.resume.utils.CacheKeys;
import com.felixsiu.resume.utils.JwtUtil;
import com.felixsiu.resume.utils.MyRedisTemplate;
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
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

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
		@Mock
		private EmailTokenService emailTokenService;
		@Mock
		private AccountBaseService accountBaseService;
		@Mock
		private AccountService accountService;
		@Mock
		private MyRedisTemplate myRedisTemplate;
		@InjectMocks
		private AuthService authService;
		
		private final EmailTokenMap purposeVerifyEmail = EmailTokenMap.VERIFY;
		private final String purposeVerify = EmailTokenMap.VERIFY.getName();
		private final EmailTokenMap purposeReset = EmailTokenMap.RESET;
		private final String purposeResetName = EmailTokenMap.RESET.getName();
		
		// ============================================================
		// 測試數據工廠
		// ============================================================
		
		private RegisterDto createRegisterDto(String username, String password, String email) {
				return new RegisterDto(username, password, email);
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
		void register_shouldSuccess_whenUsernameAndEmailAvailable() {
				RegisterDto dto = createRegisterDto("felix", "password123", "felix@example.com");
				String encodedPassword = "encoded_password_123";
				String userId = "uuid-new";
				
				when(accountBaseService.countEmail("felix@example.com")).thenReturn(0L);
				when(userMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
				when(passwordEncoder.encode("password123")).thenReturn(encodedPassword);
				when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
						User user = invocation.getArgument(0);
						user.setId(userId);
						return 1;
				});
				
				BaseVo<Void> result = authService.register(dto);
				
				assertThat(result.getIsSuccess()).isTrue();
				verify(passwordEncoder).encode("password123");
				
				ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
				verify(userMapper).insert(captor.capture());
				User captured = captor.getValue();
				assertThat(captured.getUsername()).isEqualTo("felix");
				assertThat(captured.getPassword()).isEqualTo(encodedPassword);
				assertThat(captured.getEmail()).isEqualTo("felix@example.com");
				assertThat(captured.getEmailVerifiedAt()).isNull();
				verify(accountService).sendVerifyTokenEmail("felix@example.com", userId, purposeVerifyEmail);
		}
		
		@Test
		void register_shouldThrow_whenEmailTaken() {
				RegisterDto dto = createRegisterDto("felix", "password123", "taken@example.com");
				
				when(accountBaseService.countEmail("taken@example.com")).thenReturn(1L);
				
				assertThatThrownBy(() -> authService.register(dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("該電子郵箱已被佔用");
				
				verify(userMapper, never()).exists(any(LambdaQueryWrapper.class));
				verify(userMapper, never()).insert(any(User.class));
				verify(passwordEncoder, never()).encode(anyString());
				verify(accountService, never()).sendVerifyTokenEmail(anyString(), anyString(), any(EmailTokenMap.class));
		}
		
		@Test
		void register_shouldThrowException_whenUsernameAlreadyExists() {
				RegisterDto dto = createRegisterDto("felix", "password123", "felix@example.com");
				
				when(accountBaseService.countEmail("felix@example.com")).thenReturn(0L);
				when(userMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(true);
				
				assertThatThrownBy(() -> authService.register(dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("該用戶已存在");
				
				verify(userMapper, never()).insert(any(User.class));
				verify(passwordEncoder, never()).encode(anyString());
				verify(accountService, never()).sendVerifyTokenEmail(anyString(), anyString(), any(EmailTokenMap.class));
		}
		
		@Test
		void register_shouldThrowException_whenInsertFails() {
				RegisterDto dto = createRegisterDto("felix", "password123", "felix@example.com");
				
				when(accountBaseService.countEmail("felix@example.com")).thenReturn(0L);
				when(userMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
				when(passwordEncoder.encode(anyString())).thenReturn("encoded");
				when(userMapper.insert(any(User.class))).thenReturn(0);
				
				assertThatThrownBy(() -> authService.register(dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("創建帳號失敗");
				
				verify(accountService, never()).sendVerifyTokenEmail(anyString(), anyString(), any(EmailTokenMap.class));
		}
		
		@Test
		void register_shouldThrow_whenInsertSucceedsButUserIdMissing() {
				RegisterDto dto = createRegisterDto("felix", "password123", "felix@example.com");
				
				when(accountBaseService.countEmail("felix@example.com")).thenReturn(0L);
				when(userMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
				when(passwordEncoder.encode(anyString())).thenReturn("encoded");
				when(userMapper.insert(any(User.class))).thenReturn(1); // id 未回填
				
				assertThatThrownBy(() -> authService.register(dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("創建帳號失敗");
				
				verify(accountService, never()).sendVerifyTokenEmail(anyString(), anyString(), any(EmailTokenMap.class));
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
		
		// ============================================================
		// verifyEmail
		// ============================================================
		
		@Test
		void verifyEmail_shouldSuccess_whenTokenMatchesCurrentEmail() throws NoSuchAlgorithmException {
				String userId = "uuid-123";
				String email = "felix@example.com";
				String rawToken = "raw-verify-token";
				String tokenHash = Sha256Hex.toHexString(rawToken);
				
				EmailToken token = new EmailToken();
				token.setTokenHash(tokenHash);
				token.setUserId(userId);
				token.setEmail(email);
				
				User user = createUser(userId, "felix", "encoded");
				user.setEmail(email);
				
				when(emailTokenService.findByValidHash(tokenHash, purposeVerify)).thenReturn(token);
				when(accountBaseService.selectByPK(userId)).thenReturn(user);
				when(emailTokenService.updateHashUsedAt(tokenHash)).thenReturn(true);
				when(accountBaseService.updateEmailByPK(eq(userId), eq(email), any(LocalDateTime.class))).thenReturn(true);
				
				BaseVo<Void> result = authService.verifyEmail(new ValidateEmailTokenDto(rawToken));
				
				assertThat(result.getIsSuccess()).isTrue();
				verify(emailTokenService).updateHashUsedAt(tokenHash);
				verify(accountBaseService).updateEmailByPK(eq(userId), eq(email), any(LocalDateTime.class));
		}
		
		@Test
		void verifyEmail_shouldThrow_whenTokenBlank() {
				assertThatThrownBy(() -> authService.verifyEmail(new ValidateEmailTokenDto("  ")))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("token不能為空");
				
				verify(emailTokenService, never()).findByValidHash(anyString(), anyString());
		}
		
		@Test
		void verifyEmail_shouldThrow_whenTokenInvalidOrExpired() throws NoSuchAlgorithmException {
				String rawToken = "bad-or-expired";
				String tokenHash = Sha256Hex.toHexString(rawToken);
				when(emailTokenService.findByValidHash(tokenHash, purposeVerify)).thenReturn(null);
				
				assertThatThrownBy(() -> authService.verifyEmail(new ValidateEmailTokenDto(rawToken)))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("token 無效或已失效");
				
				verify(accountBaseService, never()).selectByPK(anyString());
				verify(emailTokenService, never()).updateHashUsedAt(anyString());
		}
		
		@Test
		void verifyEmail_shouldThrow_whenUserNotFound() throws NoSuchAlgorithmException {
				String userId = "uuid-123";
				String email = "felix@example.com";
				String rawToken = "raw-verify-token";
				String tokenHash = Sha256Hex.toHexString(rawToken);
				
				EmailToken token = new EmailToken();
				token.setTokenHash(tokenHash);
				token.setUserId(userId);
				token.setEmail(email);
				
				when(emailTokenService.findByValidHash(tokenHash, purposeVerify)).thenReturn(token);
				when(accountBaseService.selectByPK(userId)).thenReturn(null);
				
				assertThatThrownBy(() -> authService.verifyEmail(new ValidateEmailTokenDto(rawToken)))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("token 無效或已失效");
				
				verify(emailTokenService, never()).updateHashUsedAt(anyString());
				verify(accountBaseService, never()).updateEmailByPK(anyString(), anyString(), any());
		}
		
		@Test
		void verifyEmail_shouldThrow_whenTokenEmailMismatchCurrentEmail() throws NoSuchAlgorithmException {
				String userId = "uuid-123";
				String rawToken = "raw-old-token";
				String tokenHash = Sha256Hex.toHexString(rawToken);
				
				EmailToken token = new EmailToken();
				token.setTokenHash(tokenHash);
				token.setUserId(userId);
				token.setEmail("old@example.com");
				
				User user = createUser(userId, "felix", "encoded");
				user.setEmail("new@example.com");
				
				when(emailTokenService.findByValidHash(tokenHash, purposeVerify)).thenReturn(token);
				when(accountBaseService.selectByPK(userId)).thenReturn(user);
				
				assertThatThrownBy(() -> authService.verifyEmail(new ValidateEmailTokenDto(rawToken)))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("token 無效或已失效");
				
				verify(emailTokenService, never()).updateHashUsedAt(anyString());
				verify(accountBaseService, never()).updateEmailByPK(anyString(), anyString(), any());
		}
		
		@Test
		void verifyEmail_shouldThrow_whenUserEmailNull() throws NoSuchAlgorithmException {
				String userId = "uuid-123";
				String rawToken = "raw-verify-token";
				String tokenHash = Sha256Hex.toHexString(rawToken);
				
				EmailToken token = new EmailToken();
				token.setTokenHash(tokenHash);
				token.setUserId(userId);
				token.setEmail("felix@example.com");
				
				User user = createUser(userId, "felix", "encoded");
				user.setEmail(null);
				
				when(emailTokenService.findByValidHash(tokenHash, purposeVerify)).thenReturn(token);
				when(accountBaseService.selectByPK(userId)).thenReturn(user);
				
				assertThatThrownBy(() -> authService.verifyEmail(new ValidateEmailTokenDto(rawToken)))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("token 無效或已失效");
				
				verify(emailTokenService, never()).updateHashUsedAt(anyString());
		}
		
		@Test
		void verifyEmail_shouldThrow_whenMarkUsedFails() throws NoSuchAlgorithmException {
				String userId = "uuid-123";
				String email = "felix@example.com";
				String rawToken = "raw-verify-token";
				String tokenHash = Sha256Hex.toHexString(rawToken);
				
				EmailToken token = new EmailToken();
				token.setTokenHash(tokenHash);
				token.setUserId(userId);
				token.setEmail(email);
				
				User user = createUser(userId, "felix", "encoded");
				user.setEmail(email);
				
				when(emailTokenService.findByValidHash(tokenHash, purposeVerify)).thenReturn(token);
				when(accountBaseService.selectByPK(userId)).thenReturn(user);
				when(emailTokenService.updateHashUsedAt(tokenHash)).thenReturn(false);
				
				assertThatThrownBy(() -> authService.verifyEmail(new ValidateEmailTokenDto(rawToken)))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("驗證電子郵箱出現了錯誤");
				
				verify(accountBaseService, never()).updateEmailByPK(anyString(), anyString(), any());
		}
		
		@Test
		void verifyEmail_shouldThrow_whenUpdateEmailStatusFails() throws NoSuchAlgorithmException {
				String userId = "uuid-123";
				String email = "felix@example.com";
				String rawToken = "raw-verify-token";
				String tokenHash = Sha256Hex.toHexString(rawToken);
				
				EmailToken token = new EmailToken();
				token.setTokenHash(tokenHash);
				token.setUserId(userId);
				token.setEmail(email);
				
				User user = createUser(userId, "felix", "encoded");
				user.setEmail(email);
				
				when(emailTokenService.findByValidHash(tokenHash, purposeVerify)).thenReturn(token);
				when(accountBaseService.selectByPK(userId)).thenReturn(user);
				when(emailTokenService.updateHashUsedAt(tokenHash)).thenReturn(true);
				when(accountBaseService.updateEmailByPK(eq(userId), eq(email), any(LocalDateTime.class))).thenReturn(false);
				
				assertThatThrownBy(() -> authService.verifyEmail(new ValidateEmailTokenDto(rawToken)))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("更新電子郵箱狀態失敗");
		}
		
		// ============================================================
		// forgotPassword
		// ============================================================
		
		@Test
		void forgotPassword_shouldThrow_whenCaptchaMismatch() {
				when(httpSession.getAttribute("captcha")).thenReturn("ABC123");
				
				assertThatThrownBy(() -> authService.forgotPassword(
								httpSession,
								new ForgotPasswordDto("felix@example.com", "WRONG")
				))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("captcha 錯誤");
				
				verify(myRedisTemplate, never()).getExpire(anyString(), any());
				verify(accountBaseService, never()).selectByVerifiedEmail(anyString());
				verify(accountService, never()).sendVerifyTokenEmail(anyString(), anyString(), any());
				verify(myRedisTemplate, never()).set(anyString(), any(), anyLong(), any());
		}
		
		@Test
		void forgotPassword_shouldThrow_whenCaptchaMissingInSession() {
				when(httpSession.getAttribute("captcha")).thenReturn(null);
				
				assertThatThrownBy(() -> authService.forgotPassword(
								httpSession,
								new ForgotPasswordDto("felix@example.com", "ABC123")
				))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("captcha 錯誤");
				
				verify(myRedisTemplate, never()).getExpire(anyString(), any());
				verify(accountService, never()).sendVerifyTokenEmail(anyString(), anyString(), any());
		}
		
		@Test
		void forgotPassword_shouldFail_whenCooldownActive() {
				String email = "felix@example.com";
				String cacheKey = CacheKeys.forgotPassword(email.toLowerCase());
				
				when(httpSession.getAttribute("captcha")).thenReturn("ABC123");
				when(myRedisTemplate.getExpire(cacheKey, TimeUnit.SECONDS)).thenReturn(42L);
				
				BaseVo<CoolDownVo> result = authService.forgotPassword(
								httpSession,
								new ForgotPasswordDto(email, "ABC123")
				);
				
				assertThat(result.getIsSuccess()).isFalse();
				assertThat(result.getMessage()).contains("Too many requests");
				assertThat(result.getData()).isNotNull();
				assertThat(result.getData().getCd()).isEqualTo(42L);
				
				verify(accountBaseService, never()).selectByVerifiedEmail(anyString());
				verify(accountService, never()).sendVerifyTokenEmail(anyString(), anyString(), any());
				verify(myRedisTemplate, never()).set(anyString(), any(), anyLong(), any());
		}
		
		@Test
		void forgotPassword_shouldSendResetEmail_whenVerifiedUserExists() {
				String email = "felix@example.com";
				String userId = "uuid-123";
				String cacheKey = CacheKeys.forgotPassword(email.toLowerCase());
				
				User user = createUser(userId, "felix", "encoded");
				user.setEmail(email);
				user.setEmailVerifiedAt(LocalDateTime.now().minusDays(1));
				
				when(httpSession.getAttribute("captcha")).thenReturn("ABC123");
				when(myRedisTemplate.getExpire(cacheKey, TimeUnit.SECONDS)).thenReturn(-2L);
				when(accountBaseService.selectByVerifiedEmail(email)).thenReturn(user);
				
				BaseVo<CoolDownVo> result = authService.forgotPassword(
								httpSession,
								new ForgotPasswordDto(email, "ABC123")
				);
				
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getMessage()).contains("password reset link");
				assertThat(result.getData().getCd()).isEqualTo(60L);
				
				verify(accountService).sendVerifyTokenEmail(email, userId, purposeReset);
				verify(myRedisTemplate).set(cacheKey, "1", 60L, TimeUnit.SECONDS);
		}
		
		@Test
		void forgotPassword_shouldSucceedWithoutSending_whenUserNotFound() {
				String email = "unknown@example.com";
				String cacheKey = CacheKeys.forgotPassword(email.toLowerCase());
				
				when(httpSession.getAttribute("captcha")).thenReturn("ABC123");
				when(myRedisTemplate.getExpire(cacheKey, TimeUnit.SECONDS)).thenReturn(null);
				when(accountBaseService.selectByVerifiedEmail(email)).thenReturn(null);
				
				BaseVo<CoolDownVo> result = authService.forgotPassword(
								httpSession,
								new ForgotPasswordDto(email, "ABC123")
				);
				
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData().getCd()).isEqualTo(60L);
				
				verify(accountService, never()).sendVerifyTokenEmail(anyString(), anyString(), any());
				verify(myRedisTemplate).set(cacheKey, "1", 60L, TimeUnit.SECONDS);
		}
		
		@Test
		void forgotPassword_shouldUseLowercaseEmailForCacheKey() {
				String email = "Felix@Example.COM";
				String cacheKey = CacheKeys.forgotPassword(email.toLowerCase());
				
				when(httpSession.getAttribute("captcha")).thenReturn("ABC123");
				when(myRedisTemplate.getExpire(cacheKey, TimeUnit.SECONDS)).thenReturn(-2L);
				when(accountBaseService.selectByVerifiedEmail(email)).thenReturn(null);
				
				BaseVo<CoolDownVo> result = authService.forgotPassword(
								httpSession,
								new ForgotPasswordDto(email, "ABC123")
				);
				
				assertThat(result.getIsSuccess()).isTrue();
				verify(myRedisTemplate).getExpire(eq(cacheKey), eq(TimeUnit.SECONDS));
				verify(myRedisTemplate).set(eq(cacheKey), eq("1"), eq(60L), eq(TimeUnit.SECONDS));
				verify(accountBaseService).selectByVerifiedEmail(email);
		}
		
		// ============================================================
		// resetPassword
		// ============================================================
		
		@Test
		void resetPassword_shouldSuccess_updatePasswordMarkUsedAndRevoke() throws NoSuchAlgorithmException {
				String userId = "uuid-123";
				String email = "felix@example.com";
				String rawToken = "raw-reset-token";
				String tokenHash = Sha256Hex.toHexString(rawToken);
				String rawPassword = "newPass123";
				String encodedOld = "encoded-old";
				String encodedNew = "encoded-new";
				
				EmailToken token = new EmailToken();
				token.setTokenHash(tokenHash);
				token.setUserId(userId);
				token.setEmail(email);
				token.setPurpose(purposeResetName);
				
				User user = createUser(userId, "felix", encodedOld);
				user.setEmail(email);
				
				when(emailTokenService.findByValidHash(tokenHash, purposeResetName)).thenReturn(token);
				when(accountBaseService.selectByPK(userId)).thenReturn(user);
				when(passwordEncoder.encode(rawPassword)).thenReturn(encodedNew);
				when(passwordEncoder.matches(rawPassword, encodedOld)).thenReturn(false);
				when(accountBaseService.updatePasswordByPK(userId, encodedNew)).thenReturn(true);
				when(emailTokenService.updateHashUsedAt(tokenHash)).thenReturn(true);
				
				BaseVo<Void> result = authService.resetPassword(
								new ResetPasswordDto(rawToken, rawPassword),
								httpServletResponse
				);
				
				assertThat(result.getIsSuccess()).isTrue();
				
				InOrder inOrder = inOrder(accountBaseService, emailTokenService, accountService);
				inOrder.verify(accountBaseService).updatePasswordByPK(userId, encodedNew);
				inOrder.verify(emailTokenService).updateHashUsedAt(tokenHash);
				inOrder.verify(accountService).actionCallback(userId, httpServletResponse);
		}
		
		@Test
		void resetPassword_shouldThrow_whenTokenBlank() {
				assertThatThrownBy(() -> authService.resetPassword(
								new ResetPasswordDto("  ", "newPass123"),
								httpServletResponse
				))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("token不能為空");
				
				verify(emailTokenService, never()).findByValidHash(anyString(), anyString());
				verify(accountService, never()).actionCallback(anyString(), any());
		}
		
		@Test
		void resetPassword_shouldThrow_whenTokenInvalidOrExpired() throws NoSuchAlgorithmException {
				String rawToken = "bad-or-expired";
				String tokenHash = Sha256Hex.toHexString(rawToken);
				
				when(emailTokenService.findByValidHash(tokenHash, purposeResetName)).thenReturn(null);
				
				assertThatThrownBy(() -> authService.resetPassword(
								new ResetPasswordDto(rawToken, "newPass123"),
								httpServletResponse
				))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("token 無效或已失效");
				
				verify(accountBaseService, never()).selectByPK(anyString());
				verify(accountBaseService, never()).updatePasswordByPK(anyString(), anyString());
				verify(emailTokenService, never()).updateHashUsedAt(anyString());
				verify(accountService, never()).actionCallback(anyString(), any());
		}
		
		@Test
		void resetPassword_shouldThrow_whenUserNotFound() throws NoSuchAlgorithmException {
				String userId = "uuid-missing";
				String rawToken = "raw-reset-token";
				String tokenHash = Sha256Hex.toHexString(rawToken);
				
				EmailToken token = new EmailToken();
				token.setTokenHash(tokenHash);
				token.setUserId(userId);
				token.setEmail("felix@example.com");
				
				when(emailTokenService.findByValidHash(tokenHash, purposeResetName)).thenReturn(token);
				when(accountBaseService.selectByPK(userId)).thenReturn(null);
				
				assertThatThrownBy(() -> authService.resetPassword(
								new ResetPasswordDto(rawToken, "newPass123"),
								httpServletResponse
				))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("token 無效或已失效");
				
				verify(accountBaseService, never()).updatePasswordByPK(anyString(), anyString());
				verify(emailTokenService, never()).updateHashUsedAt(anyString());
				verify(accountService, never()).actionCallback(anyString(), any());
		}
		
		@Test
		void resetPassword_shouldThrow_whenNewPasswordSameAsOld() throws NoSuchAlgorithmException {
				String userId = "uuid-123";
				String rawToken = "raw-reset-token";
				String tokenHash = Sha256Hex.toHexString(rawToken);
				String rawPassword = "samePass123";
				String encodedOld = "encoded-old";
				
				EmailToken token = new EmailToken();
				token.setTokenHash(tokenHash);
				token.setUserId(userId);
				
				User user = createUser(userId, "felix", encodedOld);
				
				when(emailTokenService.findByValidHash(tokenHash, purposeResetName)).thenReturn(token);
				when(accountBaseService.selectByPK(userId)).thenReturn(user);
				when(passwordEncoder.matches(rawPassword, encodedOld)).thenReturn(true);
				
				assertThatThrownBy(() -> authService.resetPassword(
								new ResetPasswordDto(rawToken, rawPassword),
								httpServletResponse
				))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("新舊密碼一致");
				
				verify(passwordEncoder, never()).encode(anyString());
				verify(accountBaseService, never()).updatePasswordByPK(anyString(), anyString());
				verify(emailTokenService, never()).updateHashUsedAt(anyString());
				verify(accountService, never()).actionCallback(anyString(), any());
		}
		
		@Test
		void resetPassword_shouldThrow_whenUpdatePasswordFails() throws NoSuchAlgorithmException {
				String userId = "uuid-123";
				String rawToken = "raw-reset-token";
				String tokenHash = Sha256Hex.toHexString(rawToken);
				String rawPassword = "newPass123";
				String encodedOld = "encoded-old";
				String encodedNew = "encoded-new";
				
				EmailToken token = new EmailToken();
				token.setTokenHash(tokenHash);
				token.setUserId(userId);
				
				User user = createUser(userId, "felix", encodedOld);
				
				when(emailTokenService.findByValidHash(tokenHash, purposeResetName)).thenReturn(token);
				when(accountBaseService.selectByPK(userId)).thenReturn(user);
				when(passwordEncoder.encode(rawPassword)).thenReturn(encodedNew);
				when(passwordEncoder.matches(rawPassword, encodedOld)).thenReturn(false);
				when(accountBaseService.updatePasswordByPK(userId, encodedNew)).thenReturn(false);
				
				assertThatThrownBy(() -> authService.resetPassword(
								new ResetPasswordDto(rawToken, rawPassword),
								httpServletResponse
				))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("更新密碼出現了錯誤");
				
				verify(emailTokenService, never()).updateHashUsedAt(anyString());
				verify(accountService, never()).actionCallback(anyString(), any());
		}
		
		@Test
		void resetPassword_shouldThrow_whenMarkUsedFails() throws NoSuchAlgorithmException {
				String userId = "uuid-123";
				String rawToken = "raw-reset-token";
				String tokenHash = Sha256Hex.toHexString(rawToken);
				String rawPassword = "newPass123";
				String encodedOld = "encoded-old";
				String encodedNew = "encoded-new";
				
				EmailToken token = new EmailToken();
				token.setTokenHash(tokenHash);
				token.setUserId(userId);
				
				User user = createUser(userId, "felix", encodedOld);
				
				when(emailTokenService.findByValidHash(tokenHash, purposeResetName)).thenReturn(token);
				when(accountBaseService.selectByPK(userId)).thenReturn(user);
				when(passwordEncoder.encode(rawPassword)).thenReturn(encodedNew);
				when(passwordEncoder.matches(rawPassword, encodedOld)).thenReturn(false);
				when(accountBaseService.updatePasswordByPK(userId, encodedNew)).thenReturn(true);
				when(emailTokenService.updateHashUsedAt(tokenHash)).thenReturn(false);
				
				assertThatThrownBy(() -> authService.resetPassword(
								new ResetPasswordDto(rawToken, rawPassword),
								httpServletResponse
				))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("更新密碼出現了錯誤");
				
				verify(accountService, never()).actionCallback(anyString(), any());
		}
		
		@Test
		void resetPassword_shouldThrow_whenUserSoftDeleted() throws NoSuchAlgorithmException {
				String userId = "uuid-123";
				String rawToken = "raw-reset-token";
				String tokenHash = Sha256Hex.toHexString(rawToken);
				
				EmailToken token = new EmailToken();
				token.setTokenHash(tokenHash);
				token.setUserId(userId);
				
				User deletedUser = createUser(userId, "felix", "encoded-old", LocalDateTime.now().minusDays(1));
				
				when(emailTokenService.findByValidHash(tokenHash, purposeResetName)).thenReturn(token);
				when(accountBaseService.selectByPK(userId)).thenReturn(deletedUser);
				
				assertThatThrownBy(() -> authService.resetPassword(
								new ResetPasswordDto(rawToken, "newPass123"),
								httpServletResponse
				))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("token 無效或已失效");
				
				verify(passwordEncoder, never()).encode(anyString());
				verify(accountBaseService, never()).updatePasswordByPK(anyString(), anyString());
				verify(accountService, never()).actionCallback(anyString(), any());
		}
		
		// ============================================================
		// validateResetToken
		// ============================================================
		
		@Test
		void validateResetToken_shouldSuccess_withoutMarkingUsed() throws NoSuchAlgorithmException {
				String userId = "uuid-123";
				String rawToken = "raw-reset-token";
				String tokenHash = Sha256Hex.toHexString(rawToken);
				
				EmailToken token = new EmailToken();
				token.setTokenHash(tokenHash);
				token.setUserId(userId);
				token.setPurpose(purposeResetName);
				
				User user = createUser(userId, "felix", "encoded");
				
				when(emailTokenService.findByValidHash(tokenHash, purposeResetName)).thenReturn(token);
				when(accountBaseService.selectByPK(userId)).thenReturn(user);
				
				BaseVo<Void> result = authService.validateResetToken(new ValidateEmailTokenDto(rawToken));
				
				assertThat(result.getIsSuccess()).isTrue();
				verify(emailTokenService, never()).updateHashUsedAt(anyString());
				verify(accountBaseService, never()).updatePasswordByPK(anyString(), anyString());
				verify(accountService, never()).actionCallback(anyString(), any());
		}
		
		@Test
		void validateResetToken_shouldThrow_whenTokenBlank() {
				assertThatThrownBy(() -> authService.validateResetToken(new ValidateEmailTokenDto("  ")))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("token不能為空");
				
				verify(emailTokenService, never()).findByValidHash(anyString(), anyString());
				verify(accountBaseService, never()).selectByPK(anyString());
		}
		
		@Test
		void validateResetToken_shouldThrow_whenTokenInvalidOrExpired() throws NoSuchAlgorithmException {
				String rawToken = "bad-or-expired";
				String tokenHash = Sha256Hex.toHexString(rawToken);
				
				when(emailTokenService.findByValidHash(tokenHash, purposeResetName)).thenReturn(null);
				
				assertThatThrownBy(() -> authService.validateResetToken(new ValidateEmailTokenDto(rawToken)))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("token 無效或已失效");
				
				verify(accountBaseService, never()).selectByPK(anyString());
				verify(emailTokenService, never()).updateHashUsedAt(anyString());
		}
		
		@Test
		void validateResetToken_shouldThrow_whenUserNotFound() throws NoSuchAlgorithmException {
				String userId = "uuid-missing";
				String rawToken = "raw-reset-token";
				String tokenHash = Sha256Hex.toHexString(rawToken);
				
				EmailToken token = new EmailToken();
				token.setTokenHash(tokenHash);
				token.setUserId(userId);
				
				when(emailTokenService.findByValidHash(tokenHash, purposeResetName)).thenReturn(token);
				when(accountBaseService.selectByPK(userId)).thenReturn(null);
				
				assertThatThrownBy(() -> authService.validateResetToken(new ValidateEmailTokenDto(rawToken)))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("token 無效或已失效");
				
				verify(emailTokenService, never()).updateHashUsedAt(anyString());
		}
		
		@Test
		void validateResetToken_shouldThrow_whenUserSoftDeleted() throws NoSuchAlgorithmException {
				String userId = "uuid-123";
				String rawToken = "raw-reset-token";
				String tokenHash = Sha256Hex.toHexString(rawToken);
				
				EmailToken token = new EmailToken();
				token.setTokenHash(tokenHash);
				token.setUserId(userId);
				
				User deletedUser = createUser(userId, "felix", "encoded", LocalDateTime.now().minusDays(1));
				
				when(emailTokenService.findByValidHash(tokenHash, purposeResetName)).thenReturn(token);
				when(accountBaseService.selectByPK(userId)).thenReturn(deletedUser);
				
				assertThatThrownBy(() -> authService.validateResetToken(new ValidateEmailTokenDto(rawToken)))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("token 無效或已失效");
				
				verify(emailTokenService, never()).updateHashUsedAt(anyString());
		}
}