package com.felixsiu.resume.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class SocialLinksValidator implements ConstraintValidator<ValidSocialLinks, Map<String, String>> {
		
		//  只允許這兩種平台
		private static final Set<String> ALLOWED_PLATFORMS = Set.of("github", "linkedin");
		
		//  GitHub URL 格式：https://github.com/username
		private static final Pattern GITHUB_PATTERN =
						Pattern.compile("^https?://(www\\.)?github\\.com/[A-Za-z0-9_-]+/?$");
		
		//  LinkedIn URL 格式：https://linkedin.com/in/username
		private static final Pattern LINKEDIN_PATTERN =
						Pattern.compile("^https?://(www\\.)?linkedin\\.com/(in|company)/[A-Za-z0-9_-]+/?$");
		
		@Override
		public boolean isValid(Map<String, String> socialLinks, ConstraintValidatorContext context) {
				if (socialLinks == null || socialLinks.isEmpty()) {
						return true;
				}
				
				for (Map.Entry<String, String> entry : socialLinks.entrySet()) {
						String platform = entry.getKey();
						String url = entry.getValue();
						
						// 1. 平台必須在白名單內
						if (!ALLOWED_PLATFORMS.contains(platform)) {
								context.disableDefaultConstraintViolation();
								context.buildConstraintViolationWithTemplate(
												"只支援 GitHub 和 LinkedIn"
								).addConstraintViolation();
								return false;
						}
						
						// 2. 如果 URL 為空，跳過驗證
						if (url == null || url.trim().isEmpty()) {
								continue;
						}
						
						// 3. 驗證 URL 格式
						boolean isValid = switch (platform) {
								case "github" -> GITHUB_PATTERN.matcher(url).matches();
								case "linkedin" -> LINKEDIN_PATTERN.matcher(url).matches();
								default -> false;
						};
						
						if (!isValid) {
								context.disableDefaultConstraintViolation();
								String message = platform.equals("github")
												? "GitHub 連結格式無效，請使用格式：https://github.com/username"
												: "LinkedIn 連結格式無效，請使用格式：https://linkedin.com/in/username";
								context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
								return false;
						}
				}
				
				return true;
		}
}