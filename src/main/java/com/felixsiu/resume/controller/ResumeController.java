package com.felixsiu.resume.controller;

import com.felixsiu.resume.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

/**
 * 渲染個人履歷頁面 (Thymeleaf)
 * 模板位置: src/main/resources/templates/resume-pdf.html
 */
@Controller
@RequestMapping("/api/v1/resume")
public class ResumeController {
		@Autowired
		ResumeService resumeService;
		
		@GetMapping
		public String resume(Model model, @RequestAttribute("userId") String userId) {
				return resumeService.getResume(model, userId);
		}
		
		@GetMapping("/export")
		public ResponseEntity<InputStreamResource> exportPdf(@RequestAttribute("userId") String userId) throws IOException {
				return resumeService.exportPdf(userId);
		}
}
