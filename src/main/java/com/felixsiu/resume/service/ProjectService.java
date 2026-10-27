package com.felixsiu.resume.service;

import com.felixsiu.resume.model.converter.ProjectConverter;
import com.felixsiu.resume.model.dto.ProjectDto;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
		 * @param querySlug 查詢的 slug
		 * @return 項目細節
		 */
		public BaseVo<ProjectDetailVo> getProjectDetail(String userId, String querySlug) {
				
				if (querySlug == null || querySlug.trim().isEmpty()) {
						throw new RuntimeException("請輸入 slug");
				}
				
				Project foundProject = projectBaseService.selectOneBySlug(userId, querySlug);
				
				if (foundProject == null) {
						throw new RuntimeException("未找到對應項目");
				}
				
				Integer queryProjectId = foundProject.getId();
				
				ProjectDetail foundProjectDetail = projectDetailService.selectOne(queryProjectId);
				
				if (foundProjectDetail == null) {
						throw new RuntimeException("未找到對應項目詳情");
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
						throw new RuntimeException("參數缺失");
				}
				
				// 用戶項目當前數量
				Integer lastSortOrder = projectBaseService.getLastSortOrder(userId);
				
				// 新sort order默認排最大
				Integer newSortOrder = lastSortOrder + 10;
				
				// 添加主表的數據
				Project projectEntity = projectConverter.convertProjectEntity(userId, dto, newSortOrder);
				
				boolean isInsertSuccess = projectBaseService.insert(projectEntity);
				
				if (!isInsertSuccess) {
						throw new RuntimeException("添加失敗");
				}
				
				// 獲取剛剛插入的new project id
				Integer newProjectId = projectEntity.getId();
				if (newProjectId == null) {
						System.err.println("獲取最新的項目 ID 失敗");
						throw new RuntimeException("添加失敗");
				}
				
				List<ProjectStack> projectStackList = projectConverter.convertStackEntity(dto.stacks(), newProjectId);
				
				// 批量插入stack
				boolean isInsertStacksSuccess = projectStacksService.insertBatch(projectStackList);
				
				if (!isInsertStacksSuccess) {
						throw new RuntimeException("添加失敗");
				}
				
				ProjectDetail projectDetailEntity = projectConverter.convertDetailEntity(dto, newProjectId);
				
				// 插入項目細節
				boolean isInsertDetailSuccess = projectDetailService.insert(projectDetailEntity);
				
				if (!isInsertDetailSuccess) {
						throw new RuntimeException("添加失敗");
				}
				
				return BaseVo.success(null);
		}
		
		/**
		 * 修改當前項目
		 *
		 * @param userId 用戶ID
		 * @param id     項目ID(PK)
		 * @param dto    修改的項目dto
		 * @return void
		 */
		@Transactional(rollbackFor = Exception.class)
		public BaseVo<Void> editProject(String userId, Integer id, ProjectDto dto) {
				if (isPostBodyNotOk(dto)) {
						throw new RuntimeException("參數缺失");
				}
				
				Project foundProject = projectBaseService.selectOneById(userId, id);
				
				if (foundProject == null) {
						throw new RuntimeException("找不到對應的項目");
				}
				
				// 修改主表數據
				Project newProjectEntity = projectConverter.convertProjectEntity(userId, dto, foundProject.getSortOrder());
				newProjectEntity.setId(id);
				
				boolean isUpdateSuccess = projectBaseService.updateByPK(newProjectEntity);
				
				if (!isUpdateSuccess) {
						throw new RuntimeException("更新項目失敗");
				}
				
				ProjectDetail foundProjectDetail = projectDetailService.selectOne(id);
				
				if (foundProjectDetail == null) {
						throw new RuntimeException("找不到對應的項目細節");
				}
				
				// 修改項目細節
				ProjectDetail newDetailEntity = projectConverter.convertDetailEntity(dto, id);
				
				boolean isUpdateDetailSuccess = projectDetailService.updateByPK(newDetailEntity);
				
				if (!isUpdateDetailSuccess) {
						throw new RuntimeException("更新項目細節失敗");
				}
				
				// 先批量刪除技術棧
				boolean isDeleteStacksSuccess = projectStacksService.deleteByProjectId(id);
				
				if (!isDeleteStacksSuccess) {
						throw new RuntimeException("更新項目技術棧失敗");
				}
				
				List<ProjectStack> projectStackList = projectConverter.convertStackEntity(dto.stacks(), id);
				
				// 重新批量插入技術棧
				boolean isInsertStacksSuccess = projectStacksService.insertBatch(projectStackList);
				
				if (!isInsertStacksSuccess) {
						throw new RuntimeException("更新項目技術棧失敗");
				}
				
				return BaseVo.success(null);
				
		}
		
		/**
		 * 刪除某項目
		 * @param userId 用戶ID
		 * @param id 項目ID
		 * @return void
		 */
		@Transactional(rollbackFor = Exception.class)
		public BaseVo<Void> deleteProject(String userId, Integer id){
				boolean isDelete = projectBaseService.deleteByPKAndUser(userId, id);
				
				if(!isDelete){
						throw new RuntimeException("刪除失敗，資料不存在");
				}
				
				return BaseVo.success(null);
		}
}
