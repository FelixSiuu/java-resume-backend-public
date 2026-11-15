package com.felixsiu.resume.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest
public class SimpleEmailTest {
		@Autowired
		private JavaMailSender mailSender;
		
		@Value("${app.mail.from}")
		private String mailFrom;
		
		@Test
		public void testSimpleEmail() {
				SimpleMailMessage message = new SimpleMailMessage();
				message.setFrom(mailFrom);
				message.setTo("b1044327634@gmail.com");
				message.setSubject("Hi, this is from Spring Boot Mail Service");
				message.setText("WoW!!!!!!");
//				mailSender.send(message);
		}
}
