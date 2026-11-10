package com.felixsiu.resume.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt.cookie")
@Data
public class CookieProperties {
		private String nameAccess;
		private String nameRefresh;
		private String nameXsrf;
		private Boolean secure;
		private String sameSite;
		private String path;
		private Integer accessTtlSeconds;
		private Integer refreshTtlSecondsShort;
		private Integer refreshTtlSecondsLong;
}
