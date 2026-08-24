package com.felixsiu.resume.controller;

import com.felixsiu.resume.model.entity.About;
import com.felixsiu.resume.model.entity.CoreSkills;
import com.felixsiu.resume.model.entity.Education;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.model.vo.ContactVo;
import com.felixsiu.resume.model.vo.WorkingExperienceVo;
import com.felixsiu.resume.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
		
}
