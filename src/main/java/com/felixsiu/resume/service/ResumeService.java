package com.felixsiu.resume.service;

import com.felixsiu.resume.model.entity.About;
import com.felixsiu.resume.model.entity.Avatar;
import com.felixsiu.resume.model.entity.CoreSkills;
import com.felixsiu.resume.model.entity.Education;
import com.felixsiu.resume.model.vo.ContactVo;
import com.felixsiu.resume.model.vo.WorkingExperienceVo;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ResumeService {
		@Autowired
		private SpringTemplateEngine templateEngine;
		
		@Autowired
		private AboutService aboutService;
		
		@Autowired
		private SkillService skillService;
		
		@Autowired
		private EducationService educationService;
		
		@Autowired
		private WorkingExpService workingExpService;
		
		@Autowired
		private ContactService contactService;
		
		@Autowired
		private AvatarService avatarService;
		
		public String getResume(Model model, @RequestAttribute("userId") String userId) {
				// 1. 獲取所有數據
				List<About> aboutList = aboutService.getAboutMe(userId).getData();
				List<CoreSkills> skillsList = skillService.getCoreSkills(userId).getData();
				List<Education> eduList = educationService.getEducation(userId).getData();
				List<WorkingExperienceVo> expList = workingExpService.getWorkingExperiences(userId).getData();
				ContactVo contact = contactService.getContact(userId).getData();
				Map<String, Boolean> publicMap = contact.getPublicMap();
				Map<String, String> socialLinks = contact.getSocialLinks();
				Optional<Avatar> avatarOpt = avatarService.getAvatar(userId);
				
				// 2. 放入 Model (對應模板中的變數名稱)
				model.addAttribute("aboutList", aboutList);
				model.addAttribute("skillsList", skillsList);
				model.addAttribute("eduList", eduList);
				model.addAttribute("expList", expList);
				model.addAttribute("contact", contact);
				model.addAttribute("publicMap", publicMap);
				model.addAttribute("socialLinks", socialLinks);
				
				String avatarDataUrl = avatarOpt.map(a -> "data:" + a.getContentType() + ";base64," + Base64.getEncoder().encodeToString(a.getImageData())).orElse(null);
				model.addAttribute("avatarDataUrl", avatarDataUrl);
				
				// 3. 返回模板名稱 (對應 resume-pdf.html)
				return "resume-pdf";
		}
		
		public ResponseEntity<InputStreamResource> exportPdf(String userId) throws IOException {
				// 1. 獲取所有數據
				List<About> aboutList = aboutService.getAboutMe(userId).getData();
				List<CoreSkills> skillsList = skillService.getCoreSkills(userId).getData();
				List<Education> eduList = educationService.getEducation(userId).getData();
				List<WorkingExperienceVo> expList = workingExpService.getWorkingExperiences(userId).getData();
				ContactVo contact = contactService.getContact(userId).getData();
				Map<String, Boolean> publicMap = contact.getPublicMap();
				Map<String, String> socialLinks = contact.getSocialLinks();
				Optional<Avatar> avatarOpt = avatarService.getAvatar(userId);
				
				// 2. 使用 Thymeleaf 渲染 HTML
				Context context = new Context();
				context.setVariable("aboutList", aboutList);
				context.setVariable("skillsList", skillsList);
				context.setVariable("eduList", eduList);
				context.setVariable("expList", expList);
				context.setVariable("contact", contact);
				context.setVariable("publicMap", publicMap);
				context.setVariable("socialLinks", socialLinks);
				
				String avatarDataUrl = avatarOpt.map(a -> "data:" + a.getContentType() + ";base64," + Base64.getEncoder().encodeToString(a.getImageData())).orElse(null);
				context.setVariable("avatarDataUrl", avatarDataUrl);
				
				String html = templateEngine.process("resume-pdf", context);
				
				// 3. 轉換為 PDF
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				PdfRendererBuilder builder = new PdfRendererBuilder();
				builder.useFastMode();
				builder.withHtmlContent(html, "http://localhost:8080");
				builder.toStream(outputStream);
				builder.run();
				
				// 4. 下載響應
				byte[] pdfBytes = outputStream.toByteArray();
				ByteArrayInputStream inputStream = new ByteArrayInputStream(pdfBytes);
				
				// 5. 構建下載響應
				HttpHeaders headers = new HttpHeaders();
				headers.add(
								HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Resume_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf"
				);
				
				return ResponseEntity.ok().headers(headers)
								.contentType(MediaType.APPLICATION_PDF)
								.body(new InputStreamResource(inputStream));
		}
}
