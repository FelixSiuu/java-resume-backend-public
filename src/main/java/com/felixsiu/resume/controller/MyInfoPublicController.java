package com.felixsiu.resume.controller;

import com.felixsiu.resume.model.entity.About;
import com.felixsiu.resume.model.entity.Avatar;
import com.felixsiu.resume.model.entity.CoreSkills;
import com.felixsiu.resume.model.entity.Education;
import com.felixsiu.resume.model.vo.*;
import com.felixsiu.resume.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * 公開的個人資料接口
 */
@RestController
@RequestMapping("/api/v1/public")
@Tag(name = "Public Info", description = "公開的個人資料接口")
public class MyInfoPublicController {
		@Autowired
		EducationService educationService;
		@Autowired
		AboutService aboutService;
		@Autowired
		SkillService skillService;
		@Autowired
		WorkingExpService workingExpService;
		@Autowired
		ContactService contactService;
		@Autowired
		UserService userService;
		@Autowired
		AvatarService avatarService;
		@Autowired
		ResumeService resumeService;
		@Autowired
		ProjectService projectService;
		
		/**
		 * 關於我
		 */
		@GetMapping("/{username}/about")
		@Operation(summary = "透過用戶名獲取關於我")
		public BaseVo<List<About>> about(
						@Parameter(description = "username 用戶名")
						@PathVariable String username
		) {
				String userId = userService.getUserId(username);
				return aboutService.getAboutMe(userId);
		}
		
		/**
		 * 核心技能
		 */
		@GetMapping("/{username}/coreSkills")
		@Operation(summary = "透過用戶名獲取核心技能")
		public BaseVo<List<CoreSkills>> coreSkills(
						@Parameter(description = "username 用戶名")
						@PathVariable String username
		) {
				String userId = userService.getUserId(username);
				return skillService.getCoreSkills(userId);
		}
		
		/**
		 * 工作經驗
		 */
		@GetMapping("/{username}/workingExperiences")
		@Operation(summary = "透過用戶名獲取工作經驗")
		public BaseVo<List<WorkingExperienceVo>> workingExperiences(
						@Parameter(description = "username 用戶名")
						@PathVariable String username
		) {
				String userId = userService.getUserId(username);
				return workingExpService.getWorkingExperiences(userId);
		}
		
		/**
		 * 教育程度
		 */
		@GetMapping("/{username}/education")
		@Operation(summary = "透過用戶名獲取教育程度")
		public BaseVo<List<Education>> education(
						@Parameter(description = "username 用戶名")
						@PathVariable String username
		) {
				String userId = userService.getUserId(username);
				return educationService.getEducation(userId);
		}
		
		/**
		 * 聯繫方式
		 */
		@GetMapping("/{username}/contact")
		@Operation(summary = "透過用戶名獲取聯繫方式")
		public BaseVo<ContactVo> contact(
						@Parameter(description = "username 用戶名")
						@PathVariable String username
		) {
				String userId = userService.getUserId(username);
				return contactService.getContact(userId);
		}
		
		/**
		 * Avatar 頭像
		 */
		@GetMapping("/{username}/avatar")
		@Operation(summary = "透過用戶名獲取頭像")
		public ResponseEntity<byte[]> avatar(
						@Parameter(description = "username 用戶名")
						@PathVariable String username
		) {
				String userId = userService.getUserId(username);
				Optional<Avatar> avatarOpt = avatarService.getAvatar(userId);
				
				if (avatarOpt.isPresent()) {
						return ResponseEntity.ok()
										.contentType(MediaType.parseMediaType(avatarOpt.get().getContentType()))
										.header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
										.body(avatarOpt.get().getImageData());
				}
				
				return ResponseEntity.notFound().build();
		}
		
		/**
		 * 履歷預覽
		 */
		@GetMapping("/{username}/resume")
		@Operation(summary = "透過用戶名獲取履歷")
		public String resume(
						Model model,
						@Parameter(description = "username 用戶名")
						@PathVariable String username
		) {
				String userId = userService.getUserId(username);
				return resumeService.getResume(model, userId);
		}
		
		/**
		 * 履歷導出
		 */
		@GetMapping("/{username}/resume/export")
		@Operation(summary = "透過用戶名導出履歷")
		public ResponseEntity<InputStreamResource> exportPdf(
						@Parameter(description = "username 用戶名")
						@PathVariable String username
		) throws IOException {
				String userId = userService.getUserId(username);
				return resumeService.exportPdf(userId);
		}
		
		/**
		 * 項目列表
		 */
		@GetMapping("/{username}/projects")
		@Operation(summary = "透過用戶名獲取項目列表")
		public BaseVo<List<ProjectVo>> projects(
						@Parameter(description = "username 用戶名")
						@PathVariable String username,
						@Parameter(description = "是否只顯示突出項目")
						@RequestParam(required = false, defaultValue = "false")
						Boolean featured
		) {
				String userId = userService.getUserId(username);
				return projectService.getProjects(userId, featured);
		}
		
		/**
		 * 項目細節
		 */
		@GetMapping("/{username}/projects/{slug}")
		@Operation(summary = "透過用戶名與slug獲取項目細節")
		public BaseVo<ProjectDetailVo> projectDetail(
						@Parameter(description = "username 用戶名")
						@PathVariable String username,
						@Parameter(description = "項目 slug")
						@PathVariable String slug
		) {
				String userId = userService.getUserId(username);
				return projectService.getProjectDetail(userId, slug);
		}
}
