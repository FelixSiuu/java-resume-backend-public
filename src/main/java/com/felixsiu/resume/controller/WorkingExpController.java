package com.felixsiu.resume.controller;

import com.felixsiu.resume.annotation.StandardApiResponses;
import com.felixsiu.resume.model.dto.WorkingExpDto;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.model.vo.WorkingExperienceVo;
import com.felixsiu.resume.service.WorkingExpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作經驗
 */
@RestController
@RequestMapping("/api/v1/myInfo/workingExperiences")
@Tag(name = "Working Experiences", description = "工作經驗相關接口")
public class WorkingExpController {
		
		@Autowired
		WorkingExpService workingExpService;
		
		@GetMapping
		@Operation(
						summary = "獲取工作經驗",
						description = "獲取當前登入用戶的工作經驗資訊列表"
		)
		@StandardApiResponses
		public BaseVo<List<WorkingExperienceVo>> workingExperiences(
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId
		) {
				return workingExpService.getWorkingExperiences(userId);
		}
		
		/**
		 * 同時監聽「帶參數」與「不帶參數」的兩個路由
		 */
		@PostMapping(value = {"", "/{id}"})
		@Operation(
						summary = "新增或修改工作經驗",
						description = """
										**使用情境：**
										- **新增**：不傳 `id`，系統自動生成
										- **編輯**：傳入 `id`，修改對應記錄
										"""
		)
		@StandardApiResponses
		public BaseVo<Void> workingExperiences(
						@Parameter(
										description = "工作經驗 ID（編輯時必填，新增時省略）",
										required = false
						)
						@PathVariable(required = false) Integer id,
						@Valid @RequestBody WorkingExpDto workingExpDto,
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId) {
				if (id == null) {
						return workingExpService.addWorkingExperiences(workingExpDto, userId);
				} else {
						return workingExpService.editWorkingExperiences(id, workingExpDto, userId);
				}
		}
		
		@DeleteMapping("/{id}")
		@Operation(summary = "刪除工作經驗")
		@StandardApiResponses
		public BaseVo<Void> workingExperiences(
						@Parameter(description = "工作經驗 ID")
						@PathVariable Integer id,
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId) {
				return workingExpService.deleteWorkingExperiences(id, userId);
		}
}
