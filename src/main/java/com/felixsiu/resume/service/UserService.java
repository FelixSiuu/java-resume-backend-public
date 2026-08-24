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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Service
public class UserService {
		@Autowired
		UserMapper userMapper;
		@Autowired
		DefaultKaptcha defaultKaptcha;
		@Autowired
		BCryptPasswordEncoder passwordEncoder;
		@Autowired
		JwtUtil jwtUtil;
		
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
		public BaseVo<LoginVo> login(LoginDto loginDto, HttpSession httpSession) {
				String un = loginDto.username();
				String pwd = loginDto.password();
				String capt = loginDto.captcha();
				
				if (!capt.equals(httpSession.getAttribute("captcha"))) {
						throw new RuntimeException("captcha 錯誤");
				}
				
				LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
				lambdaQueryWrapper.eq(User::getUsername, un);
				User foundUser = userMapper.selectOne(lambdaQueryWrapper);
				
				if (foundUser == null) {
						throw new RuntimeException("username 或 password錯誤");
				}
				
				boolean isPwdMatch = passwordEncoder.matches(pwd, foundUser.getPassword());
				
				if (!isPwdMatch) {
						throw new RuntimeException("username 或 password錯誤");
				}
				
				String token = jwtUtil.generateToken(foundUser.getId());
				
				LoginVo loginVo = new LoginVo();
				loginVo.setId(foundUser.getId());
				loginVo.setToken(token);
				loginVo.setUsername(foundUser.getUsername());
				
				return BaseVo.success(loginVo);
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
				
				if(!StringUtils.hasText(username)) {
						throw new RuntimeException("請輸入用戶名");
				}
				
				LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
				lambdaQueryWrapper.eq(User::getUsername, username);
				User user = userMapper.selectOne(lambdaQueryWrapper);
				
				if(user == null) {
						throw new RuntimeException("用戶不存在");
				}
				
				return user.getId();
		}
}
