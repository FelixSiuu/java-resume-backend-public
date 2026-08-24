package com.felixsiu.resume.controller;

import com.felixsiu.resume.annotation.StandardApiResponses;
import com.felixsiu.resume.model.dto.SkillDto;
import com.felixsiu.resume.model.entity.CoreSkills;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.service.SkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 核心技能
 */
@RestController
@RequestMapping("/api/v1/myInfo/coreSkills")
@Tag(name = "CoreSkills", description = "核心技能相關接口")
public class CoreSkillsController {
		
		@Autowired
		SkillService skillService;
		
		
		@GetMapping
		@Operation(
						summary = "獲取核心技能",
						description = "獲取當前登入用戶的核心技能資訊列表"
		)
		@StandardApiResponses
		public BaseVo<List<CoreSkills>> coreSkills(
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId
		) {
				return skillService.getCoreSkills(userId);
		}
		
		@PutMapping("/{id}")
		@Operation(
						summary = "編輯核心技能",
						description = "修改指定 ID 的核心技能內容"
		)
		@StandardApiResponses
		public BaseVo<Void> coreSkills(
						@Parameter(description = "技能 ID")
						@PathVariable Integer id,
						@Valid @RequestBody SkillDto skillDto,
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId
		) {
				return skillService.editSkill(id, skillDto, userId);
		}
		
		@DeleteMapping("/{id}")
		@Operation(summary = "刪除核心技能")
		@StandardApiResponses
		public BaseVo<Void> coreSkills(
						@Parameter(description = "技能 ID")
						@PathVariable Integer id,
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId
		) {
				return skillService.deleteSkill(id, userId);
		}
		
		@PostMapping
		@Operation(summary = "新增核心技能")
		@StandardApiResponses
		public BaseVo<Void> coreSkills(
						@Valid @RequestBody SkillDto skillDto,
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId) {
				return skillService.addSkill(skillDto, userId);
		}
}
