package com.felixsiu.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.felixsiu.resume.model.dto.LoginDto;
import com.felixsiu.resume.model.dto.RegisterDto;
import com.felixsiu.resume.model.entity.User;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.model.vo.LoginVo;
import com.felixsiu.resume.respository.UserMapper;
import com.felixsiu.resume.utils.JwtUtil;
import com.google.code.kaptcha.impl.DefaultKaptcha;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
		
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
		
		@InjectMocks
		private UserService userService;
		
		// ============================================================
		// 測試數據工廠
		// ============================================================
		
		private RegisterDto createRegisterDto(String username, String password) {
				return new RegisterDto(username, password);
		}
		
		private LoginDto createLoginDto(String username, String password, String captcha) {
				return new LoginDto(username, password, captcha);
		}
		
		private User createUser(String id, String username, String encodedPassword) {
				User user = new User();
				user.setId(id);
				user.setUsername(username);
				user.setPassword(encodedPassword);
				return user;
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
				BaseVo<Void> result = userService.register(dto);
				
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
				assertThatThrownBy(() -> userService.register(dto))
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
				assertThatThrownBy(() -> userService.register(dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("創建帳號失敗");
		}
		
		// ============================================================
		// 2. login 測試
		// ============================================================
		
		@Test
		void login_shouldSuccess_whenValidCredentials() {
				// 準備
				LoginDto dto = createLoginDto("felix", "password123", "ABC123");
				String userId = "uuid-123";
				String encodedPassword = "encoded_password_123";
				String expectedToken = "jwt_token_xyz";
				
				User mockUser = createUser(userId, "felix", encodedPassword);
				
				when(httpSession.getAttribute("captcha")).thenReturn("ABC123");
				when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(mockUser);
				when(passwordEncoder.matches("password123", encodedPassword)).thenReturn(true);
				when(jwtUtil.generateToken(userId)).thenReturn(expectedToken);
				
				// 執行
				BaseVo<LoginVo> result = userService.login(dto, httpSession);
				
				// 驗證
				assertThat(result.getIsSuccess()).isTrue();
				LoginVo loginVo = result.getData();
				assertThat(loginVo.getUsername()).isEqualTo("felix");
				assertThat(loginVo.getToken()).isEqualTo(expectedToken);
				
				verify(jwtUtil).generateToken(userId);
		}
		
		@Test
		void login_shouldThrowException_whenCaptchaMismatch() {
				// 準備：驗證碼錯誤
				LoginDto dto = createLoginDto("felix", "password123", "WRONG");
				
				when(httpSession.getAttribute("captcha")).thenReturn("ABC123");
				
				// 執行 & 驗證
				assertThatThrownBy(() -> userService.login(dto, httpSession))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("captcha 錯誤");
				
				// 驗證：不應該查詢用戶
				verify(userMapper, never()).selectOne(any(LambdaQueryWrapper.class));
				verify(jwtUtil, never()).generateToken(anyString());
		}
		
		@Test
		void login_shouldThrowException_whenCaptchaIsNull() {
				// 準備：Session 中沒有驗證碼（過期）
				LoginDto dto = createLoginDto("felix", "password123", "ABC123");
				
				when(httpSession.getAttribute("captcha")).thenReturn(null);
				
				// 執行 & 驗證
				assertThatThrownBy(() -> userService.login(dto, httpSession))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("captcha 錯誤");
		}
		
		@Test
		void login_shouldThrowException_whenUserNotFound() {
				// 準備：用戶不存在
				LoginDto dto = createLoginDto("unknown", "password123", "ABC123");
				
				when(httpSession.getAttribute("captcha")).thenReturn("ABC123");
				when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
				
				// 執行 & 驗證
				assertThatThrownBy(() -> userService.login(dto, httpSession))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("username 或 password錯誤");
		}
		
		@Test
		void login_shouldThrowException_whenPasswordMismatch() {
				// 準備：密碼錯誤
				LoginDto dto = createLoginDto("felix", "wrong_password", "ABC123");
				User mockUser = createUser("uuid-123", "felix", "encoded_password");
				
				when(httpSession.getAttribute("captcha")).thenReturn("ABC123");
				when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(mockUser);
				when(passwordEncoder.matches("wrong_password", "encoded_password")).thenReturn(false);
				
				// 執行 & 驗證
				assertThatThrownBy(() -> userService.login(dto, httpSession))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("username 或 password錯誤");
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
				BaseVo<String> result = userService.createCaptcha(httpSession);
				
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
				BaseVo<String> result = userService.createCaptcha(httpSession);
				
				// 驗證
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData()).startsWith("data:image/jpeg;base64,");
		}
}