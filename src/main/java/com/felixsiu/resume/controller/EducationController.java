package com.felixsiu.resume.controller;

import com.felixsiu.resume.annotation.StandardApiResponses;
import com.felixsiu.resume.model.dto.EduDto;
import com.felixsiu.resume.model.entity.Education;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.service.EducationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教育程度
 */
@RestController
@RequestMapping("/api/v1/myInfo/education")
@Tag(name = "Education", description = "教育程度相關接口")
public class EducationController {
		
		@Autowired
		EducationService educationService;
		
		@GetMapping
		@Operation(
						summary = "獲取教育程度",
						description = "獲取當前登入用戶的教育程度資訊列表"
		)
		@StandardApiResponses
		public BaseVo<List<Education>> education(
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId
		) {
				return educationService.getEducation(userId);
		}
		
		@PostMapping(value = {"", "/{id}"})
		@Operation(
						summary = "新增或修改教育程度",
						description = """
										**使用情境：**
										- **新增**：不傳 `id`，系統自動生成
										- **編輯**：傳入 `id`，修改對應記錄
										"""
		)
		@StandardApiResponses
		public BaseVo<Void> education(
						@Parameter(
										description = "教育程度 ID（編輯時必填，新增時省略）",
										required = false
						)
						@PathVariable(required = false) Integer id,
						@Valid @RequestBody EduDto eduDto,
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId
		) {
				if (id == null) {
						return educationService.addEducation(eduDto, userId);
				} else {
						return educationService.editEducation(id, eduDto, userId);
				}
		}
		
		@DeleteMapping("/{id}")
		@Operation(summary = "刪除教育程度")
		@StandardApiResponses
		public BaseVo<Void> education(
						@Parameter(description = "教育程度 ID")
						@PathVariable Integer id,
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId
		) {
				return educationService.deleteEducation(id, userId);
		}
}
