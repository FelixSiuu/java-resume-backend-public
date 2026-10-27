package com.felixsiu.resume.controller;

import com.felixsiu.resume.annotation.StandardApiResponses;
import com.felixsiu.resume.model.dto.ProjectDto;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.model.vo.ProjectDetailVo;
import com.felixsiu.resume.model.vo.ProjectVo;
import com.felixsiu.resume.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 項目經驗
 */
@RestController
@RequestMapping("/api/v1/myInfo/projects")
@Tag(name = "Project", description = "項目經驗相關接口")
public class ProjectsController {
		
		@Autowired
		ProjectService projectService;
		
		@GetMapping
		@Operation(
						summary = "獲取項目經驗",
						description = "獲取當前登入用戶的項目經驗"
		)
		@StandardApiResponses
		public BaseVo<List<ProjectVo>> getProjects(
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId,
						@Parameter(description = "是否只顯示突出項目")
						@RequestParam(required = false, defaultValue = "false") boolean featured
		) {
				return projectService.getProjects(userId, featured);
		}
		
		@GetMapping("/{slug}")
		@Operation(
						summary = "獲取項目細節",
						description = "獲取指定 slug 的項目細節"
		)
		@StandardApiResponses
		public BaseVo<ProjectDetailVo> getProjectDetail(
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId,
						@Parameter(description = "項目 slug")
						@PathVariable String slug
		) {
				return projectService.getProjectDetail(userId, slug);
		}
		
		@PostMapping
		@Operation(summary = "新增項目")
		@StandardApiResponses
		public BaseVo<Void> addProject(
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId,
						@Parameter(description = "data from front end")
						@Valid @RequestBody ProjectDto projectDto
		){
				return projectService.addProject(userId, projectDto);
		}
		
		@PutMapping("/{id}")
		@Operation(summary = "修改指定項目")
		@StandardApiResponses
		public BaseVo<Void> editProject(
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId,
						@Parameter(description = "data from front end")
						@Valid @RequestBody ProjectDto projectDto,
						@Parameter(description = "項目 id")
						@PathVariable Integer id
		){
				return projectService.editProject(userId, id, projectDto);
		}
		
		@DeleteMapping("/{id}")
		@Operation(summary = "刪除指定項目")
		@StandardApiResponses
		public BaseVo<Void> deleteProject(
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId,
						@Parameter(description = "項目 id")
						@PathVariable Integer id
		){
				return projectService.deleteProject(userId, id);
		}
}
