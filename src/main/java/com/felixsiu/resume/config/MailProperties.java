package com.felixsiu.resume.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.mail")
@Data
public class MailProperties {
		private String from;
		private String frontendBaseUrl;
}
