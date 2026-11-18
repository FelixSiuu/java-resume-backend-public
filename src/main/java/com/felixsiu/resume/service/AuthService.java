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
import com.felixsiu.resume.utils.*;
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
import java.util.concurrent.TimeUnit;

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
		@Autowired
		EmailTokenService emailTokenService;
		@Autowired
		AccountBaseService accountBaseService;
		@Autowired
		AccountService accountService;
		@Autowired
		MyRedisTemplate myRedisTemplate;
		
		private final String captchaSessionName = "captcha";
		
		/**
		 * 註冊
		 */
		public BaseVo<Void> register(RegisterDto registerDto) throws RuntimeException {
				String un = registerDto.username();
				String pwd = registerDto.password();
				String email = registerDto.email();
				
				Long thisEmailCount = accountBaseService.countEmail(email);
				
				if (thisEmailCount > 0) {
						throw new RuntimeException("該電子郵箱已被佔用");
				}
				
				LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
				lambdaQueryWrapper.eq(User::getUsername, un);
				boolean exists = userMapper.exists(lambdaQueryWrapper);
				
				if (exists) {
						throw new RuntimeException("該用戶已存在");
				}
				
				User newUser = new User();
				newUser.setUsername(un);
				newUser.setPassword(passwordEncoder.encode(pwd));
				newUser.setEmail(email);
				
				int res = userMapper.insert(newUser);
				
				String userId = newUser.getId();
				
				if (res == 0 || userId == null) {
						throw new RuntimeException("創建帳號失敗");
				}
				
				accountService.sendVerifyTokenEmail(email, userId, EmailTokenMap.VERIFY);
				
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
				
				if (!capt.equals(httpSession.getAttribute(captchaSessionName))) {
						throw new RuntimeException("captcha 錯誤");
				}
				
				httpSession.removeAttribute(captchaSessionName);
				
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
				
				session.setAttribute(captchaSessionName, text);
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
		
		/**
		 * refresh token
		 *
		 * @param request  HttpServletRequest
		 * @param response HttpServletResponse
		 * @return LoginVo
		 */
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
						
						if (foundUser == null || foundUser.getDeletedAt() != null) {
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
		
		/**
		 * find valid token hash (reset or verify)
		 *
		 * @param rawToken      raw token
		 * @param emailTokenMap enum EmailTokenMap
		 * @return EmailToken
		 */
		private EmailToken findValidEmailToken(String rawToken, EmailTokenMap emailTokenMap) {
				if (!StringUtils.hasText(rawToken)) {
						throw new RuntimeException("token不能為空");
				}
				
				try {
						String tokenHash = Sha256Hex.toHexString(rawToken);
						
						EmailToken validTokenHash = emailTokenService.findByValidHash(
										tokenHash,
										emailTokenMap.getName()
						);
						
						if (validTokenHash == null) {
								throw new RuntimeException("token 無效或已失效");
						}
						
						return validTokenHash;
						
				} catch (NoSuchAlgorithmException e) {
						throw new RuntimeException("token 無效或已失效");
				}
		}
		
		/**
		 * verify email
		 *
		 * @param dto email from front end
		 * @return void
		 */
		@Transactional(rollbackFor = Exception.class)
		public BaseVo<Void> verifyEmail(ValidateEmailTokenDto dto) {
				EmailToken validTokenHash = findValidEmailToken(dto.token(), EmailTokenMap.VERIFY);
				
				String userId = validTokenHash.getUserId();
				
				User foundUser = accountBaseService.selectByPK(userId);
				String email = validTokenHash.getEmail();
				
				if (foundUser == null || foundUser.getEmail() == null || !foundUser.getEmail().equals(email)) {
						throw new RuntimeException("token 無效或已失效");
				}
				
				boolean updateHashUsedAtSuccess = emailTokenService.updateHashUsedAt(validTokenHash.getTokenHash());
				
				if (!updateHashUsedAtSuccess) {
						throw new RuntimeException("驗證電子郵箱出現了錯誤");
				}
				
				boolean updateEmailStatusSuccess = accountBaseService.updateEmailByPK(userId, email, LocalDateTime.now());
				
				if (!updateEmailStatusSuccess) {
						throw new RuntimeException("更新電子郵箱狀態失敗");
				}
				
				return BaseVo.success(null);
		}
		
		/**
		 * forgot password, request to send email
		 * ttl 60 seconds
		 *
		 * @param httpSession HttpSession
		 * @param dto         ForgotPasswordDto
		 * @return CoolDownVo (cooldown seconds)
		 */
		public BaseVo<CoolDownVo> forgotPassword(HttpSession httpSession, ForgotPasswordDto dto) {
				String captcha = dto.captcha();
				String email = dto.email();
				final long CACHE_TTL = 60;
				final TimeUnit CACHE_TTL_UNIT = TimeUnit.SECONDS;
				String cacheKey = CacheKeys.forgotPassword(email.toLowerCase());
				
				if (captcha == null || !captcha.equals(httpSession.getAttribute(captchaSessionName))) {
						throw new RuntimeException("captcha 錯誤");
				}
				
				httpSession.removeAttribute(captchaSessionName);
				
				Long ttl = myRedisTemplate.getExpire(cacheKey, CACHE_TTL_UNIT);
				
				if (ttl != null && ttl > 0) {
						CoolDownVo vo = new CoolDownVo();
						vo.setCd(ttl);
						return BaseVo.fail(vo, "Too many requests, please try again later.");
				}
				
				User user = accountBaseService.selectByVerifiedEmail(email);
				
				if (user != null) {
						String toEmail = user.getEmail();
						String userId = user.getId();
						accountService.sendVerifyTokenEmail(toEmail, userId, EmailTokenMap.RESET);
				}
				
				myRedisTemplate.set(cacheKey, "1", CACHE_TTL, CACHE_TTL_UNIT);
				
				CoolDownVo coolDownVo = new CoolDownVo();
				coolDownVo.setCd(60);
				
				return BaseVo.success(coolDownVo, "If this email is registered and verified, we have sent a password reset link.");
		}
		
		/**
		 * reset password, clean cookie
		 *
		 * @param dto      ResetPasswordDto
		 * @param response HttpServletResponse
		 * @return void
		 */
		@Transactional(rollbackFor = Exception.class)
		public BaseVo<Void> resetPassword(ResetPasswordDto dto, HttpServletResponse response) {
				EmailToken validTokenHash = findValidEmailToken(dto.token(), EmailTokenMap.RESET);
				
				String userId = validTokenHash.getUserId();
				
				User foundUser = accountBaseService.selectByPK(userId);
				
				if (foundUser == null || foundUser.getDeletedAt() != null) {
						throw new RuntimeException("token 無效或已失效");
				}
				
				if (passwordEncoder.matches(dto.password(), foundUser.getPassword())) {
						throw new RuntimeException("新舊密碼一致，不需要重置");
				}
				
				String newPassword = passwordEncoder.encode(dto.password());
				
				boolean isUpdateSuccess = accountBaseService.updatePasswordByPK(userId, newPassword);
				
				if (!isUpdateSuccess) {
						throw new RuntimeException("更新密碼出現了錯誤");
				}
				
				boolean updateHashUsedAtSuccess = emailTokenService.updateHashUsedAt(validTokenHash.getTokenHash());
				
				if (!updateHashUsedAtSuccess) {
						throw new RuntimeException("更新密碼出現了錯誤");
				}
				
				accountService.actionCallback(userId, response);
				
				return BaseVo.success(null);
		}
		
		/**
		 * reset password token check
		 *
		 * @param dto ValidateEmailTokenDto
		 * @return void
		 */
		public BaseVo<Void> validateResetToken(ValidateEmailTokenDto dto) {
				EmailToken validTokenHash = findValidEmailToken(dto.token(), EmailTokenMap.RESET);
				User user = accountBaseService.selectByPK(validTokenHash.getUserId());
				if (user == null || user.getDeletedAt() != null) {
						throw new RuntimeException("token 無效或已失效");
				}
				return BaseVo.success(null);
		}
		
}
