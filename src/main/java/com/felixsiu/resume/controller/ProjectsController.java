package com.felixsiu.resume.controller;

import com.felixsiu.resume.annotation.StandardApiResponses;
import com.felixsiu.resume.model.dto.ProjectDto;
import com.felixsiu.resume.model.dto.ProjectSortDto;
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
						summary = "獲取項目列表",
						description = "獲取當前登入用戶的項目列表"
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
		) {
				return projectService.addProject(userId, projectDto);
		}
		
		@PutMapping("/{slug}")
		@Operation(
						summary = "修改指定項目",
						description = "修改指定 slug 的項目"
		
		)
		@StandardApiResponses
		public BaseVo<Void> editProject(
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId,
						@Parameter(description = "project data from front end")
						@Valid @RequestBody ProjectDto projectDto,
						@Parameter(description = "項目 slug")
						@PathVariable String slug
		) {
				return projectService.editProject(userId, slug, projectDto);
		}
		
		@DeleteMapping("/{slug}")
		@Operation(
						summary = "刪除指定項目",
						description = "刪除指定 slug 的項目"
		)
		@StandardApiResponses
		public BaseVo<Void> deleteProject(
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId,
						@Parameter(description = "項目 slug")
						@PathVariable String slug
		) {
				return projectService.deleteProject(userId, slug);
		}
		
		@PutMapping("/reorder")
		@Operation(
						summary = "重新排序項目"
		)
		@StandardApiResponses
		public BaseVo<Void> reorderProject(
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId,
						@Parameter(description = "完整slug列表")
						@Valid @RequestBody ProjectSortDto projectSortDto
		){
				return projectService.reorderProject(userId, projectSortDto);
		}
}
