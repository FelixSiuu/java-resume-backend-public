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
import com.felixsiu.resume.utils.RefreshTokenUtil;
import com.felixsiu.resume.utils.Sha256Hex;
import com.felixsiu.resume.utils.XsrfTokenUtil;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class AuthService {
		@Autowired
		UserMapper userMapper;
		@Autowired
		RefreshTokenService refreshTokenService;
		@Autowired
		DefaultKaptcha defaultKaptcha;
		@Autowired
		BCryptPasswordEncoder passwordEncoder;
		@Autowired
		JwtUtil jwtUtil;
		@Autowired
		CookieProperties cookieProperties;
		@Autowired
		CookieService cookieService;
		
		/**
		 * 註冊
		 */
		public BaseVo<Void> register(RegisterDto registerDto) throws RuntimeException {
				String un = registerDto.username();
				String pwd = registerDto.password();
				
				LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
				lambdaQueryWrapper.eq(User::getUsername, un);
				boolean exists = userMapper.exists(lambdaQueryWrapper);
				
				if (exists) {
						throw new RuntimeException("該用戶已存在");
				}
				
				User newUser = new User();
				newUser.setUsername(un);
				newUser.setPassword(passwordEncoder.encode(pwd));
				
				int res = userMapper.insert(newUser);
				
				if (res == 0) {
						throw new RuntimeException("創建帳號失敗");
				}
				
				return BaseVo.success(null);
		}
		
		/**
		 * 登錄
		 */
		@Transactional(rollbackFor = Exception.class)
		public BaseVo<LoginVo> login(LoginDto loginDto, HttpSession httpSession, HttpServletResponse response) {
				String un = loginDto.username();
				String pwd = loginDto.password();
				String capt = loginDto.captcha();
				Boolean rememberMe = loginDto.rememberMe();
				
				if (!capt.equals(httpSession.getAttribute("captcha"))) {
						throw new RuntimeException("captcha 錯誤");
				}
				
				User foundUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, un));
				
				if (foundUser == null || foundUser.getDeletedAt() != null) {
						throw new RuntimeException("username 或 password錯誤");
				}
				
				boolean isPwdMatch = passwordEncoder.matches(pwd, foundUser.getPassword());
				
				if (!isPwdMatch) {
						throw new RuntimeException("username 或 password錯誤");
				}
				
				String rawRefreshToken = RefreshTokenUtil.generate();
				Integer refreshTtlSecondsShort = cookieProperties.getRefreshTtlSecondsShort();
				Integer refreshTtlSecondsLong = cookieProperties.getRefreshTtlSecondsLong();
				Integer refreshTtlSeconds = rememberMe ? refreshTtlSecondsLong : refreshTtlSecondsShort;
				
				try {
						RefreshToken refreshToken = new RefreshToken();
						String hashRefreshToken = Sha256Hex.toHexString(rawRefreshToken);
						refreshToken.setUserId(foundUser.getId());
						refreshToken.setUserName(foundUser.getUsername());
						refreshToken.setTokenHash(hashRefreshToken);
						refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTtlSeconds));
						refreshToken.setRememberMe(rememberMe);
						
						boolean isInsertRefreshToken = refreshTokenService.insert(refreshToken);
						
						if (!isInsertRefreshToken) {
								throw new RuntimeException("refresh token 寫入失敗");
						}
						
						Integer accessTtlSeconds = cookieProperties.getAccessTtlSeconds();
						String accessToken = jwtUtil.generateToken(foundUser.getId(), accessTtlSeconds);
						String xsrfToken = XsrfTokenUtil.generate();
						
						cookieService.writeTokenToCookie(
										response,
										accessToken, Duration.ofSeconds(accessTtlSeconds),
										rawRefreshToken, rememberMe ? Duration.ofSeconds(refreshTtlSeconds) : null,
										xsrfToken, Duration.ofSeconds(accessTtlSeconds)
						);
						
						LoginVo loginVo = new LoginVo();
						loginVo.setId(foundUser.getId());
						loginVo.setUsername(foundUser.getUsername());
						loginVo.setXsrfToken(xsrfToken);
						
						return BaseVo.success(loginVo);
						
				} catch (NoSuchAlgorithmException e) {
						throw new RuntimeException("refresh token 錯誤");
				}
		}
		
		/**
		 * 獲取驗證碼
		 */
		public BaseVo<String> createCaptcha(HttpSession session) throws IOException {
				
				String text = defaultKaptcha.createText();
				System.out.println("captcha text: " + text);
				
				session.setAttribute("captcha", text);
				BufferedImage bufferedImage = defaultKaptcha.createImage(text);
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				ImageIO.write(bufferedImage, "jpg", byteArrayOutputStream);
				byte[] imageByteArray = byteArrayOutputStream.toByteArray();
				
				return BaseVo.success("data:image/jpeg;base64, " + Base64.getEncoder().encodeToString(imageByteArray));
		}
		
		/**
		 * 透過username獲取userId
		 */
		public String getUserId(String username) {
				
				if (!StringUtils.hasText(username)) {
						throw new RuntimeException("請輸入用戶名");
				}
				
				LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
				lambdaQueryWrapper.eq(User::getUsername, username);
				User user = userMapper.selectOne(lambdaQueryWrapper);
				
				if (user == null || user.getDeletedAt() != null) {
						throw new RuntimeException("用戶不存在");
				}
				
				return user.getId();
		}
		
		private String extractRefreshCookie(HttpServletRequest request) {
				String rawRefreshToken = null;
				if (request.getCookies() != null) {
						for (Cookie c : request.getCookies()) {
								if (cookieProperties.getNameRefresh().equals(c.getName())) {
										rawRefreshToken = c.getValue();
										break;
								}
						}
				}
				return rawRefreshToken;
		}
		
		/**
		 * logout user, revoke refresh token and remove cookie
		 *
		 * @param request  HttpServletRequest
		 * @param response HttpServletResponse
		 * @return void
		 */
		public BaseVo<Void> logout(HttpServletRequest request, HttpServletResponse response) {
				String raw = extractRefreshCookie(request);
				if (StringUtils.hasText(raw)) {
						try {
								String hash = Sha256Hex.toHexString(raw);
								RefreshToken t = refreshTokenService.selectOneByHash(hash);
								if (t != null && !t.getRevoked()) {
										refreshTokenService.revokeIfActive(t.getId(), null);  // tag revoked
								}
						} catch (NoSuchAlgorithmException e) {
								throw new RuntimeException("refresh token 錯誤");
						}
				}
				
				cookieService.clearTokenCookies(response);
				return BaseVo.success(null);
		}
		
		@Transactional(rollbackFor = Exception.class)
		public BaseVo<LoginVo> refreshToken(HttpServletRequest request, HttpServletResponse response) {
				
				String raw = extractRefreshCookie(request);
				
				if (!StringUtils.hasText(raw)) {
						// no refresh token
						response.setStatus(401);
						return BaseVo.fail(null, "已過期，請重新登錄");
				}
				
				try {
						String hashRefreshToken = Sha256Hex.toHexString(raw);
						RefreshToken existingRefreshToken = refreshTokenService.selectOneByHash(hashRefreshToken);
						
						if (existingRefreshToken == null) {
								response.setStatus(401);
								return BaseVo.fail(null, "已過期，請重新登錄");
						}
						
						Boolean isRevoked = existingRefreshToken.getRevoked();
						String userId = existingRefreshToken.getUserId();
						
						User foundUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getId, userId));
						
						if(foundUser == null || foundUser.getDeletedAt() != null) {
								throw new RuntimeException("找不到該用戶");
						}
						
						LocalDateTime expiresTime = existingRefreshToken.getExpiresAt();
						String userName = existingRefreshToken.getUserName();
						Boolean rememberMe = existingRefreshToken.getRememberMe();
						LocalDateTime now = LocalDateTime.now();
						
						if (isRevoked) {
								int result = refreshTokenService.revokeAllByUserId(userId);
								if (result == 0) System.err.println("revoke all token failed");
								response.setStatus(403);
								return BaseVo.fail(null, "未經授權的活動");
						}
						
						if (expiresTime.isBefore(now)) {
								response.setStatus(401);
								return BaseVo.fail(null, "已過期，請重新登錄");
						}
						
						String newRawRefreshToken = RefreshTokenUtil.generate();
						String newHashRefreshToken = Sha256Hex.toHexString(newRawRefreshToken);
						RefreshToken newRefreshToken = new RefreshToken();
						newRefreshToken.setTokenHash(newHashRefreshToken);
						newRefreshToken.setUserId(userId);
						newRefreshToken.setUserName(userName);
						newRefreshToken.setExpiresAt(expiresTime);
						newRefreshToken.setRememberMe(existingRefreshToken.getRememberMe());
						
						// insert new refresh token
						boolean isInsertSuccess = refreshTokenService.insert(newRefreshToken);
						
						if (!isInsertSuccess) {
								throw new RuntimeException("new refresh token 寫入失敗");
						}
						
						// tag existing refresh token revoked
						boolean isRevokedSuccess = refreshTokenService.revokeIfActive(existingRefreshToken.getId(), newRefreshToken.getId());
						
						if (!isRevokedSuccess) {
								int result = refreshTokenService.revokeAllByUserId(userId);
								if (result == 0) System.err.println("revoke all token failed");
								response.setStatus(403);
								return BaseVo.fail(null, "未經授權的活動");
						}
						
						// write new token(access refresh xsrf) to response
						Integer accessTtlSeconds = cookieProperties.getAccessTtlSeconds();
						String newAccessToken = jwtUtil.generateToken(userId, accessTtlSeconds);
						String newXsrfToken = XsrfTokenUtil.generate();
						long remainingSeconds = Duration.between(now, expiresTime).getSeconds();
						
						if (remainingSeconds <= 0) {
								response.setStatus(401);
								return BaseVo.fail(null, "已過期，請重新登錄");
						}
						
						cookieService.writeTokenToCookie(
										response,
										newAccessToken, Duration.ofSeconds(accessTtlSeconds),
										newRawRefreshToken, rememberMe ? Duration.ofSeconds(remainingSeconds) : null,
										newXsrfToken, Duration.ofSeconds(accessTtlSeconds)
						);
						
						LoginVo loginVo = new LoginVo();
						loginVo.setId(userId);
						loginVo.setUsername(userName);
						loginVo.setXsrfToken(newXsrfToken);
						
						return BaseVo.success(loginVo);
						
				} catch (NoSuchAlgorithmException e) {
						throw new RuntimeException("refresh token 錯誤");
				}
				
		}
}
