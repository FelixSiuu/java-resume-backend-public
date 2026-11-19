package com.felixsiu.resume.service;

import lombok.extern.slf4j.Slf4j;
import com.felixsiu.resume.exception.BusinessException;
import com.felixsiu.resume.model.ErrorMap;
import com.felixsiu.resume.model.converter.ProjectConverter;
import com.felixsiu.resume.model.dto.ProjectDto;
import com.felixsiu.resume.model.dto.ProjectSortDto;
import com.felixsiu.resume.model.dto.ProjectStackDto;
import com.felixsiu.resume.model.entity.Project;
import com.felixsiu.resume.model.entity.ProjectDetail;
import com.felixsiu.resume.model.entity.ProjectStack;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.model.vo.ProjectDetailVo;
import com.felixsiu.resume.model.vo.ProjectVo;
import com.felixsiu.resume.service.project.ProjectBaseService;
import com.felixsiu.resume.service.project.ProjectDetailService;
import com.felixsiu.resume.service.project.ProjectStacksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProjectService {
		
		@Autowired
		private ProjectBaseService projectBaseService;
		
		@Autowired
		private ProjectDetailService projectDetailService;
		
		@Autowired
		private ProjectStacksService projectStacksService;
		
		@Autowired
		ProjectConverter projectConverter;
		
		/**
		 * 檢查 request body 是否 poss
		 *
		 * @param dto request body
		 * @return boolean
		 */
		private boolean isPostBodyNotOk(ProjectDto dto) {
				if (
								dto == null
												|| !StringUtils.hasText(dto.slug())
												|| !StringUtils.hasText(dto.name())
												|| !StringUtils.hasText(dto.value())
												|| !StringUtils.hasText(dto.summary())
												|| dto.stacks() == null || dto.stacks().isEmpty()
				) {
						return true;
				}
				
				for (ProjectStackDto stackDto : dto.stacks()) {
						if (!StringUtils.hasText(stackDto.stackName())) {
								return true;
						}
				}
				
				return false;
		}
		
		private boolean isSlugsInValid(ProjectSortDto dto) {
				return dto.slugs().stream().anyMatch(slug -> !StringUtils.hasText(slug));
		}
		
		/**
		 * 獲取項目列表
		 *
		 * @param userId   用戶ID
		 * @param featured 是否只查詢突出的文章
		 * @return 項目列表
		 */
		public BaseVo<List<ProjectVo>> getProjects(String userId, boolean featured) {
				
				List<Project> projectList = projectBaseService.selectList(userId, featured);
				
				if (projectList.isEmpty()) return BaseVo.success(List.of());
				
				List<Integer> projectIds = projectList.stream().map(Project::getId).toList();
				List<ProjectStack> projectStackList = projectStacksService.selectListByProjectIds(projectIds);
				
				Map<Integer, List<ProjectStack>> projectStackGroup = projectStackList
								.stream()
								.collect(Collectors.groupingBy(ProjectStack::getProjectId));
				
				List<ProjectVo> projectVoList = projectList
								.stream()
								.map(project -> projectConverter.convertProjectVo(project, projectConverter.convertStackVo(projectStackGroup.getOrDefault(project.getId(), new ArrayList<>()))))
								.collect(Collectors.toList());
				
				return BaseVo.success(projectVoList);
		}
		
		/**
		 * 獲取項目詳情
		 *
		 * @param userId    用戶ID
		 * @param querySlug 查詢的slug
		 * @return 項目細節
		 */
		public BaseVo<ProjectDetailVo> getProjectDetail(String userId, String querySlug) {
				
				if (querySlug == null || querySlug.trim().isEmpty()) {
						throw new BusinessException(ErrorMap.INVALID_SLUG);
				}
				
				Project foundProject = projectBaseService.selectOneBySlug(userId, querySlug);
				
				if (foundProject == null) {
						throw new BusinessException(ErrorMap.PROJECT_NOT_FOUND);
				}
				
				Integer queryProjectId = foundProject.getId();
				
				ProjectDetail foundProjectDetail = projectDetailService.selectOne(queryProjectId);
				
				if (foundProjectDetail == null) {
						throw new BusinessException(ErrorMap.PROJECT_DETAIL_NOT_FOUND);
				}
				
				List<ProjectStack> projectStackList = projectStacksService.selectListByProjectId(queryProjectId);
				
				List<String> projectStackVoList = projectConverter.convertStackVo(projectStackList);
				
				ProjectDetailVo projectDetailVo = projectConverter.convertProjectDetailVo(foundProject, foundProjectDetail, projectStackVoList);
				
				return BaseVo.success(projectDetailVo);
		}
		
		/**
		 * 添加新的項目
		 *
		 * @param userId 用戶ID
		 * @param dto    前端傳過來的project數據
		 * @return void
		 */
		@Transactional(rollbackFor = Exception.class)
		public BaseVo<Void> addProject(String userId, ProjectDto dto) {
				if (isPostBodyNotOk(dto)) {
						throw new BusinessException(ErrorMap.MISSING_PARAM);
				}
				
				// 先排除重複slug
				Project existProject = projectBaseService.selectOneBySlug(userId, dto.slug());
				if (existProject != null) {
						throw new BusinessException(ErrorMap.SLUG_TAKEN);
				}
				
				// 用戶項目當前數量
				Integer lastSortOrder = projectBaseService.getLastSortOrder(userId);
				
				// 新sort order默認排最大
				Integer newSortOrder = lastSortOrder + 10;
				
				// 添加主表的數據
				Project projectEntity = projectConverter.convertProjectEntity(userId, dto, newSortOrder);
				
				boolean isInsertSuccess = projectBaseService.insert(projectEntity);
				
				if (!isInsertSuccess) {
						throw new BusinessException(ErrorMap.CREATE_FAILED);
				}
				
				// 獲取剛剛插入的new project id
				Integer newProjectId = projectEntity.getId();
				if (newProjectId == null) {
						log.warn("獲取最新的項目 ID 失敗");
						throw new BusinessException(ErrorMap.CREATE_FAILED);
				}
				
				List<ProjectStack> projectStackList = projectConverter.convertStackEntity(dto.stacks(), newProjectId);
				
				// 批量插入stack
				boolean isInsertStacksSuccess = projectStacksService.insertBatch(projectStackList);
				
				if (!isInsertStacksSuccess) {
						throw new BusinessException(ErrorMap.CREATE_FAILED);
				}
				
				ProjectDetail projectDetailEntity = projectConverter.convertDetailEntity(dto, newProjectId);
				
				// 插入項目細節
				boolean isInsertDetailSuccess = projectDetailService.insert(projectDetailEntity);
				
				if (!isInsertDetailSuccess) {
						throw new BusinessException(ErrorMap.CREATE_FAILED);
				}
				
				return BaseVo.success(null);
		}
		
		/**
		 * 修改當前項目
		 *
		 * @param userId 用戶ID
		 * @param slug   項目slug
		 * @param dto    修改的項目dto
		 * @return void
		 */
		@Transactional(rollbackFor = Exception.class)
		public BaseVo<Void> editProject(String userId, String slug, ProjectDto dto) {
				if (isPostBodyNotOk(dto)) {
						throw new BusinessException(ErrorMap.MISSING_PARAM);
				}
				
				Project foundProject = projectBaseService.selectOneBySlug(userId, slug);
				
				if (foundProject == null) {
						throw new BusinessException(ErrorMap.PROJECT_NOT_FOUND);
				}
				
				String newSlug = dto.slug();
				String oldSlug = foundProject.getSlug();
				if (!newSlug.equals(oldSlug)) {
						Project existProject = projectBaseService.selectOneBySlug(userId, newSlug);
						if (existProject != null) {
								throw new BusinessException(ErrorMap.SLUG_TAKEN);
						}
				}
				
				Integer existingProjectId = foundProject.getId();
				
				// 修改主表數據
				Project newProjectEntity = projectConverter.convertProjectEntity(userId, dto, foundProject.getSortOrder());
				newProjectEntity.setId(existingProjectId);
				
				boolean isUpdateSuccess = projectBaseService.updateByPK(newProjectEntity);
				
				if (!isUpdateSuccess) {
						throw new BusinessException(ErrorMap.UPDATE_FAILED, "更新項目失敗");
				}
				
				ProjectDetail foundProjectDetail = projectDetailService.selectOne(existingProjectId);
				
				if (foundProjectDetail == null) {
						throw new BusinessException(ErrorMap.PROJECT_DETAIL_NOT_FOUND);
				}
				
				// 修改項目細節
				ProjectDetail newDetailEntity = projectConverter.convertDetailEntity(dto, existingProjectId);
				
				boolean isUpdateDetailSuccess = projectDetailService.updateByPK(newDetailEntity);
				
				if (!isUpdateDetailSuccess) {
						throw new BusinessException(ErrorMap.UPDATE_FAILED, "更新項目細節失敗");
				}
				
				// 先批量刪除技術棧
				boolean isDeleteStacksSuccess = projectStacksService.deleteByProjectId(existingProjectId);
				
				if (!isDeleteStacksSuccess) {
						throw new BusinessException(ErrorMap.UPDATE_FAILED, "更新項目技術棧失敗");
				}
				
				List<ProjectStack> projectStackList = projectConverter.convertStackEntity(dto.stacks(), existingProjectId);
				
				// 重新批量插入技術棧
				boolean isInsertStacksSuccess = projectStacksService.insertBatch(projectStackList);
				
				if (!isInsertStacksSuccess) {
						throw new BusinessException(ErrorMap.UPDATE_FAILED, "更新項目技術棧失敗");
				}
				
				return BaseVo.success(null);
				
		}
		
		/**
		 * 刪除某項目
		 *
		 * @param userId 用戶ID
		 * @param slug   項目slug
		 * @return void
		 */
		@Transactional(rollbackFor = Exception.class)
		public BaseVo<Void> deleteProject(String userId, String slug) {
				
				Project foundProject = projectBaseService.selectOneBySlug(userId, slug);
				
				if (foundProject == null) {
						throw new BusinessException(ErrorMap.RESOURCE_NOT_FOUND);
				}
				
				Integer existingProjectId = foundProject.getId();
				
				boolean isDelete = projectBaseService.deleteByPKAndUser(userId, existingProjectId);
				
				if (!isDelete) {
						throw new BusinessException(ErrorMap.DELETE_FAILED);
				}
				
				return BaseVo.success(null);
		}
		
		@Transactional(rollbackFor = Exception.class)
		public BaseVo<Void> reorderProject(String userId, ProjectSortDto dto) {
				if (dto == null || dto.slugs().isEmpty() || isSlugsInValid(dto)) {
						throw new BusinessException(ErrorMap.MISSING_PARAM);
				}
				
				Set<String> slugsSet = new HashSet<>(dto.slugs());
				if (slugsSet.size() != dto.slugs().size()) {
						throw new BusinessException(ErrorMap.DUPLICATE_SLUG_IN_REQUEST);
				}
				
				List<Project> projectList = projectBaseService.selectList(userId, false);
				if (projectList.isEmpty()) {
						throw new BusinessException(ErrorMap.NO_PROJECTS);
				}
				
				// only 1 project, should not call db
				if (projectList.size() == 1) {
						return BaseVo.success(null);
				}
				
				if (projectList.size() != dto.slugs().size()) {
						throw new BusinessException(ErrorMap.SLUG_MISSING_OR_EXTRA);
				}
				
				Set<String> dbSlugs = projectList.stream()
								.map(Project::getSlug)
								.collect(Collectors.toSet());
				
				if (!dbSlugs.equals(slugsSet)) {
						throw new BusinessException(ErrorMap.SLUG_MISMATCH);
				}
				
				Map<String, Project> projectMap = projectList
								.stream()
								.collect(Collectors.toMap(Project::getSlug, p -> p));
				
				List<Project> updateList = new ArrayList<>();
				int sortOrder = 10;
				
				for (String slug : dto.slugs()) {
						Project p = projectMap.get(slug);
						
						Project updateEntity = new Project();
						updateEntity.setId(p.getId());
						updateEntity.setSortOrder(sortOrder);
						
						updateList.add(updateEntity);
						sortOrder += 10;
				}
				
				boolean isSuccess = projectBaseService.updateBatchById(updateList);
				if (!isSuccess) {
						throw new BusinessException(ErrorMap.UPDATE_FAILED, "更新排序失敗");
				}
				
				return BaseVo.success(null);
		}
}
