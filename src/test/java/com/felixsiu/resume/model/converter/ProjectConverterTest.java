package com.felixsiu.resume.model.converter;

import com.felixsiu.resume.model.dto.ProjectDto;
import com.felixsiu.resume.model.dto.ProjectStackDto;
import com.felixsiu.resume.model.entity.Project;
import com.felixsiu.resume.model.entity.ProjectDetail;
import com.felixsiu.resume.model.entity.ProjectStack;
import com.felixsiu.resume.model.vo.ProjectDetailVo;
import com.felixsiu.resume.model.vo.ProjectVo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectConverterTest {
		
		private final ProjectConverter converter = new ProjectConverter();
		
		private Project createProjectEntity(
						Integer id,
						String userId,
						String slug,
						Integer sortOrder,
						String name,
						String value,
						String summary,
						Boolean featured
		) {
				Project project = new Project();
				project.setId(id);
				project.setUserId(userId);
				project.setSlug(slug);
				project.setSortOrder(sortOrder);
				project.setName(name);
				project.setValue(value);
				project.setSummary(summary);
				project.setFeatured(featured);
				project.setCreateTime(LocalDateTime.now());
				project.setUpdateTime(LocalDateTime.now());
				return project;
		}
		
		private ProjectStack createStackEntity(
						Integer id,
						Integer projectId,
						String stackName,
						Integer sortOrder
		) {
				ProjectStack projectStack = new ProjectStack();
				projectStack.setId(id);
				projectStack.setProjectId(projectId);
				projectStack.setStackName(stackName);
				projectStack.setSortOrder(sortOrder);
				return projectStack;
		}
		
		private ProjectDetail createDetailEntity(
						Integer projectId,
						String role,
						String duration,
						String challenge,
						String solution,
						String impact,
						String liveUrl,
						String repoUrl
		) {
				ProjectDetail projectDetail = new ProjectDetail();
				projectDetail.setProjectId(projectId);
				projectDetail.setRole(role);
				projectDetail.setDuration(duration);
				projectDetail.setChallenge(challenge);
				projectDetail.setSolution(solution);
				projectDetail.setImpact(impact);
				projectDetail.setLiveUrl(liveUrl);
				projectDetail.setRepoUrl(repoUrl);
				return projectDetail;
		}
		
		@Test
		void convertProjectEntity_shouldMapAllFieldsCorrectly() {
				// given
				String userId = "user-1";
				ProjectDto dto = new ProjectDto(
								"slug-1", "name 1", "value 1", "summary 1", true,
								List.of(), "role1", "duration1", "challenge1", "solution1", "impact1", "live1", "repo1"
				);
				Integer sortOrder = 10;
				
				// when
				Project result = converter.convertProjectEntity(userId, dto, sortOrder);
				
				assertThat(result.getUserId()).isEqualTo(userId);
				assertThat(result.getSlug()).isEqualTo("slug-1");
				assertThat(result.getName()).isEqualTo("name 1");
				assertThat(result.getValue()).isEqualTo("value 1");
				assertThat(result.getSummary()).isEqualTo("summary 1");
				assertThat(result.getFeatured()).isTrue();
				assertThat(result.getSortOrder()).isEqualTo(sortOrder);
		}
		
		@Test
		void convertProjectVo_shouldMapAllFieldsCorrectly() {
				String userId = "user-1";
				Project project = createProjectEntity(1, userId, "slug-1", 10, "name", "value", "summary", true);
				List<String> projectStackVoList = List.of("stack1");
				
				ProjectVo projectVo = converter.convertProjectVo(project, projectStackVoList);
				
				assertThat(projectVo.getId()).isEqualTo(1);
				assertThat(projectVo.getSlug()).isEqualTo("slug-1");
				assertThat(projectVo.getSortOrder()).isEqualTo(10);
				assertThat(projectVo.getName()).isEqualTo("name");
				assertThat(projectVo.getValue()).isEqualTo("value");
				assertThat(projectVo.getSummary()).isEqualTo("summary");
				assertThat(projectVo.getFeatured()).isTrue();
				assertThat(projectVo.getStacks()).hasSize(1);
		}
		
		@Test
		void convertDetailEntity_shouldMapAllFieldsCorrectly() {
				Integer projectId = 1;
				ProjectDto projectDto = new ProjectDto(
								"slug-2", "name 2", "value 2", "summary 1", true,
								List.of(new ProjectStackDto("stack 2", 0)),
								"role 2", "duration 2", "challenge 2", "solution 2", "impact 2", "https://www", "https://www"
				);
				
				ProjectDetail projectDetail = converter.convertDetailEntity(projectDto, projectId);
				
				assertThat(projectDetail.getProjectId()).isEqualTo(1);
				assertThat(projectDetail.getRole()).isEqualTo("role 2");
				assertThat(projectDetail.getDuration()).isEqualTo("duration 2");
				assertThat(projectDetail.getChallenge()).isEqualTo("challenge 2");
				assertThat(projectDetail.getSolution()).isEqualTo("solution 2");
				assertThat(projectDetail.getImpact()).isEqualTo("impact 2");
				assertThat(projectDetail.getLiveUrl()).isEqualTo("https://www");
				assertThat(projectDetail.getRepoUrl()).isEqualTo("https://www");
				
		}
		
		@Test
		void convertProjectDetailVo_shouldMapAllFieldsCorrectly() {
				String userId = "user-1";
				Project project = createProjectEntity(3, userId, "slug-3", 30, "name 3", "value 3", "summary 3", true);
				
				ProjectDetail projectDetail = createDetailEntity(3, "role 3", "duration 3", "challenge 3", "solution 3", "impact 3", "https://live", "https://repo");
				
				List<ProjectStack> projectStacks = List.of(
								createStackEntity(1, 3, "stack 1", 0),
								createStackEntity(2, 3, "stack 2", 1)
				);
				
				List<String> stackVoList = converter.convertStackVo(projectStacks);
				
				ProjectDetailVo vo = converter.convertProjectDetailVo(project, projectDetail, stackVoList);
				
				assertThat(vo.getSlug()).isEqualTo("slug-3");
				assertThat(vo.getName()).isEqualTo("name 3");
				assertThat(vo.getValue()).isEqualTo("value 3");
				assertThat(vo.getSummary()).isEqualTo("summary 3");
				assertThat(vo.getFeatured()).isTrue();
				assertThat(vo.getStacks()).containsExactly("stack 1", "stack 2");
				assertThat(vo.getRole()).isEqualTo("role 3");
				assertThat(vo.getDuration()).isEqualTo("duration 3");
				assertThat(vo.getChallenge()).isEqualTo("challenge 3");
				assertThat(vo.getSolution()).isEqualTo("solution 3");
				assertThat(vo.getImpact()).isEqualTo("impact 3");
				assertThat(vo.getLiveUrl()).isEqualTo("https://live");
				assertThat(vo.getRepoUrl()).isEqualTo("https://repo");
		}
}