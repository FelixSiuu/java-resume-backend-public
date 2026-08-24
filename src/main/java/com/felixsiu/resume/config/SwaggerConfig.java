package com.felixsiu.resume.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev") // 只在 dev 環境載入
public class SwaggerConfig {
		@Bean
		public OpenAPI customeOpenAPI() {
				return new OpenAPI().info(
								new Info()
												.title("Resume API")
												.version("1.0.0")
												.description("個人履歷後端 API 文件")
												.contact(new Contact()
																.name("Felix Siu")
																.email("b1044327634@gmail.com"))
												.license(new License()
																.name("MIT")
																.url("https://opensource.org/licenses/MIT"))
				);
		}
}
