package com.felixsiu.resume.model.converter;

import com.felixsiu.resume.model.dto.ProjectDto;
import com.felixsiu.resume.model.dto.ProjectStackDto;
import com.felixsiu.resume.model.entity.Project;
import com.felixsiu.resume.model.entity.ProjectDetail;
import com.felixsiu.resume.model.entity.ProjectStack;
import com.felixsiu.resume.model.vo.ProjectDetailVo;
import com.felixsiu.resume.model.vo.ProjectVo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProjectConverter {
		/**
		 * convert stack entity to vo
		 *
		 * @param entityList stack entity
		 * @return stack vo
		 */
		public List<String> convertStackVo(List<ProjectStack> entityList) {
				return entityList.stream().map(ProjectStack::getStackName).collect(Collectors.toList());
		}
		
		/**
		 * convert stack dto to entity
		 *
		 * @param dtoList   stack dto
		 * @param projectId new project id
		 * @return stack entity list
		 */
		public List<ProjectStack> convertStackEntity(List<ProjectStackDto> dtoList, Integer projectId) {
				List<ProjectStack> projectStacks = new ArrayList<>();
				for (int i = 0; i < dtoList.size(); i++) {
						ProjectStack stackEntity = new ProjectStack();
						stackEntity.setProjectId(projectId);
						stackEntity.setSortOrder(i);
						stackEntity.setStackName(dtoList.get(i).stackName());
						projectStacks.add(stackEntity);
				}
				return projectStacks;
		}
		
		/**
		 * convert project entity to vo
		 *
		 * @param projectEntity      project entity
		 * @param projectStackVoList stack vo
		 * @return project vo
		 */
		public ProjectVo convertProjectVo(Project projectEntity, List<String> projectStackVoList) {
				ProjectVo projectVo = new ProjectVo();
				projectVo.setSlug(projectEntity.getSlug());
				projectVo.setSortOrder(projectEntity.getSortOrder());
				projectVo.setName(projectEntity.getName());
				projectVo.setValue(projectEntity.getValue());
				projectVo.setSummary(projectEntity.getSummary());
				projectVo.setFeatured(projectEntity.getFeatured());
				projectVo.setStacks(projectStackVoList);
				projectVo.setCreateTime(projectEntity.getCreateTime());
				projectVo.setUpdateTime(projectEntity.getUpdateTime());
				return projectVo;
		}
		
		/**
		 * convert project dto to entity
		 *
		 * @param userId    user id
		 * @param dto       project dto
		 * @param sortOrder sort order
		 * @return project entity
		 */
		public Project convertProjectEntity(String userId, ProjectDto dto, Integer sortOrder) {
				Project projectEntity = new Project();
				projectEntity.setUserId(userId);
				projectEntity.setSlug(dto.slug());
				projectEntity.setSortOrder(sortOrder);
				projectEntity.setName(dto.name());
				projectEntity.setValue(dto.value());
				projectEntity.setSummary(dto.summary());
				projectEntity.setFeatured(dto.featured());
				return projectEntity;
		}
		
		/**
		 * convert detail dto to entity
		 *
		 * @param dto       project dto
		 * @param projectId project id
		 * @return detail entity
		 */
		public ProjectDetail convertDetailEntity(ProjectDto dto, Integer projectId) {
				ProjectDetail projectDetailEntity = new ProjectDetail();
				projectDetailEntity.setProjectId(projectId);
				projectDetailEntity.setRole(dto.role());
				projectDetailEntity.setDuration(dto.duration());
				projectDetailEntity.setChallenge(dto.challenge());
				projectDetailEntity.setSolution(dto.solution());
				projectDetailEntity.setImpact(dto.impact());
				projectDetailEntity.setLiveUrl(dto.liveUrl());
				projectDetailEntity.setRepoUrl(dto.repoUrl());
				return projectDetailEntity;
		}
		
		/**
		 * convert project detail entity to vo
		 *
		 * @param projectEntity       project entity
		 * @param projectDetailEntity project detail entity
		 * @param projectStackVoList  stack vo
		 * @return detail vo
		 */
		public ProjectDetailVo convertProjectDetailVo(Project projectEntity, ProjectDetail projectDetailEntity, List<String> projectStackVoList) {
				ProjectDetailVo projectDetailVo = new ProjectDetailVo();
				projectDetailVo.setSlug(projectEntity.getSlug());
				projectDetailVo.setName(projectEntity.getName());
				projectDetailVo.setValue(projectEntity.getValue());
				projectDetailVo.setSummary(projectEntity.getSummary());
				projectDetailVo.setFeatured(projectEntity.getFeatured());
				projectDetailVo.setStacks(projectStackVoList);
				projectDetailVo.setRole(projectDetailEntity.getRole());
				projectDetailVo.setDuration(projectDetailEntity.getDuration());
				projectDetailVo.setChallenge(projectDetailEntity.getChallenge());
				projectDetailVo.setSolution(projectDetailEntity.getSolution());
				projectDetailVo.setImpact(projectDetailEntity.getImpact());
				projectDetailVo.setLiveUrl(projectDetailEntity.getLiveUrl());
				projectDetailVo.setRepoUrl(projectDetailEntity.getRepoUrl());
				projectDetailVo.setCreateTime(projectDetailEntity.getCreateTime());
				projectDetailVo.setUpdateTime(projectDetailEntity.getUpdateTime());
				return projectDetailVo;
		}
}
