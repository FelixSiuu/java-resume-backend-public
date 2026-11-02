package com.felixsiu.resume.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {
		
		@Mock
		private ProjectBaseService projectBaseService;
		@Mock
		private ProjectStacksService projectStacksService;
		@Mock
		private ProjectDetailService projectDetailService;
		
		ProjectConverter projectConverter = new ProjectConverter();
		
		ProjectService projectService;
		
		private final String userId = "test-user-123";
		
		@BeforeEach
		void setUp() {
				projectService = new ProjectService();
				ReflectionTestUtils.setField(projectService, "projectBaseService", projectBaseService);
				ReflectionTestUtils.setField(projectService, "projectDetailService", projectDetailService);
				ReflectionTestUtils.setField(projectService, "projectStacksService", projectStacksService);
				ReflectionTestUtils.setField(projectService, "projectConverter", projectConverter);
		}
		
		private Project project(Integer id, String slug) {
				Project project = new Project();
				project.setId(id);
				project.setSlug(slug);
				return project;
		}
		
		private ProjectStack stack(Integer projectId, String stackName, Integer sortOrder) {
				ProjectStack projectStack = new ProjectStack();
				projectStack.setProjectId(projectId);
				projectStack.setStackName(stackName);
				projectStack.setSortOrder(sortOrder);
				return projectStack;
		}
		
		private ProjectDto createDto(
						String slug,
						String name,
						String value,
						String summary,
						List<ProjectStackDto> stackDtoList
		) {
				return new ProjectDto(
								slug, name, value, summary, true,
								stackDtoList,
								"role1", null, null, null, null, null, null
				);
		}
		
		@Test
		void getProjects_shouldReturnEmptyList_whenUserHasNoProjects() {
				when(projectBaseService.selectList(userId, false)).thenReturn(List.of());
				
				BaseVo<List<ProjectVo>> result = projectService.getProjects(userId, false);
				
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData()).isEmpty();
				
				verify(projectBaseService).selectList(userId, false);
				verifyNoInteractions(projectStacksService);
		}
		
		@Test
		void getProjects_shouldGroupStacksByProject_whenProjectsExist() {
				Project p1 = project(1, "slug-1");
				Project p2 = project(2, "slug-2");
				ProjectStack s1 = stack(1, "java", 0);
				ProjectStack s2 = stack(1, "spring", 1);
				ProjectStack s3 = stack(2, "mysql", 0);
				
				when(projectBaseService.selectList(userId, false)).thenReturn(List.of(p1, p2));
				when(projectStacksService.selectListByProjectIds(anyList())).thenReturn(List.of(s1, s2, s3));
				
				BaseVo<List<ProjectVo>> result = projectService.getProjects(userId, false);
				
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData()).hasSize(2);
				assertThat(result.getData().get(0).getStacks()).hasSize(2);
				assertThat(result.getData().get(1).getStacks()).hasSize(1);
				assertThat(result.getData().get(0).getStacks()).containsExactly("java", "spring");
				assertThat(result.getData().get(1).getStacks()).containsExactly("mysql");
		}
		
		@Test
		void getProjectDetail_shouldThrow_whenSlugInValid() {
				String invalidSlug = "  ";
				
				assertThatThrownBy(() -> projectService.getProjectDetail(userId, invalidSlug))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("請輸入 slug");
				
				verifyNoInteractions(projectBaseService, projectDetailService, projectStacksService);
		}
		
		@Test
		void getProjectDetail_shouldThrow_whenProjectNotFound() {
				String slug = "slug-1";
				when(projectBaseService.selectOneBySlug(userId, slug)).thenReturn(null);
				
				assertThatThrownBy(() -> projectService.getProjectDetail(userId, slug))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("未找到對應項目");
				
				verifyNoInteractions(projectDetailService, projectStacksService);
		}
		
		@Test
		void getProjectDetail_shouldThrow_whenDetailNotFound() {
				String slug = "slug-1";
				
				Project p1 = project(1, "slug-1");
				
				when(projectBaseService.selectOneBySlug(userId, slug)).thenReturn(p1);
				
				when(projectDetailService.selectOne(p1.getId())).thenReturn(null);
				
				assertThatThrownBy(() -> projectService.getProjectDetail(userId, slug))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("未找到對應項目詳情");
				
				verify(projectStacksService, never()).selectListByProjectId(anyInt());
		}
		
		@Test
		void getProjectDetail_shouldReturnSuccess_whenProjectFound() {
				String slug = "slug-1";
				
				Project p1 = project(1, "slug-1");
				ProjectStack s1 = stack(1, "Next.js", 0);
				ProjectDetail d1 = new ProjectDetail();
				d1.setRole("role1");
				
				when(projectBaseService.selectOneBySlug(userId, slug)).thenReturn(p1);
				when(projectStacksService.selectListByProjectId(1)).thenReturn(List.of(s1));
				when(projectDetailService.selectOne(1)).thenReturn(d1);
				
				BaseVo<ProjectDetailVo> result = projectService.getProjectDetail(userId, slug);
				
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData().getSlug()).isEqualTo("slug-1");
				assertThat(result.getData().getStacks()).containsExactly("Next.js");
				assertThat(result.getData().getRole()).isEqualTo("role1");
				
		}
		
		@Test
		void addProject_shouldSuccess_whenValidInput() {
				
				ProjectDto dto = createDto("slug-1", "name-1", "value-1", "summary-1", List.of(new ProjectStackDto("stack1")));
				
				when(projectBaseService.getLastSortOrder(userId)).thenReturn(50);
				when(projectBaseService.insert(any(Project.class))).thenAnswer(invocation -> {
						Project p = invocation.getArgument(0);
						p.setId(100);
						return true;
				});
				when(projectStacksService.insertBatch(anyList())).thenReturn(true);
				when(projectDetailService.insert(any(ProjectDetail.class))).thenReturn(true);
				
				BaseVo<Void> result = projectService.addProject(userId, dto);
				
				// 驗證sortOrder計算
				ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
				verify(projectBaseService).insert(projectCaptor.capture());
				assertThat(projectCaptor.getValue().getSortOrder()).isEqualTo(60); // 50 + 10
				
				// 驗證stack拿到回填的id
				ArgumentCaptor<List<ProjectStack>> stackCaptor = ArgumentCaptor.forClass(List.class);
				verify(projectStacksService).insertBatch(stackCaptor.capture());
				assertThat(stackCaptor.getValue())
								.hasSize(1)
								.allMatch(s -> s.getProjectId() == 100);
				
				// 驗證detail拿到回填的id與role
				ArgumentCaptor<ProjectDetail> detailCaptor = ArgumentCaptor.forClass(ProjectDetail.class);
				verify(projectDetailService).insert(detailCaptor.capture());
				assertThat(detailCaptor.getValue().getProjectId()).isEqualTo(100);
				assertThat(detailCaptor.getValue().getRole()).isEqualTo("role1");
				
				assertThat(result.getIsSuccess()).isTrue();
		}
		
		@Test
		void addProject_shouldThrow_whenSlugIsEmpty() {
				ProjectDto dto = createDto(null, "name-1", "value-1", "summary-1", List.of(new ProjectStackDto("stack1")));
				
				assertThatThrownBy(() -> projectService.addProject(userId, dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("參數缺失");
				
				verifyNoInteractions(projectBaseService, projectStacksService, projectDetailService);
		}
		
		@Test
		void addProject_shouldThrow_whenNameIsMissing() {
				ProjectDto dto = createDto("slug-1", null, "value-1", "summary-1", List.of(new ProjectStackDto("stack1")));
				
				assertThatThrownBy(() -> projectService.addProject(userId, dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("參數缺失");
				
				verifyNoInteractions(projectBaseService, projectStacksService, projectDetailService);
		}
		
		@Test
		void addProject_shouldThrow_whenStackListIsEmpty() {
				
				ProjectDto dto = createDto("slug-1", "name-1", "value-1", "summary-1", List.of());
				
				assertThatThrownBy(() -> projectService.addProject(userId, dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("參數缺失");
				
				verifyNoInteractions(projectBaseService, projectStacksService, projectDetailService);
		}
		
		@Test
		void addProject_shouldThrow_whenSlugIsRepeated() {
				
				ProjectDto dto = createDto("repeated-slug", "name-1", "value-1", "summary-1", List.of(new ProjectStackDto("stack1")));
				
				when(projectBaseService.selectOneBySlug(userId, dto.slug())).thenReturn(project(1, dto.slug()));
				
				assertThatThrownBy(() -> projectService.addProject(userId, dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("該 slug 已被使用，請更換一個");
				
				verifyNoMoreInteractions(projectBaseService);
				verifyNoInteractions(projectStacksService, projectDetailService);
		}
		
		@Test
		void addProject_shouldThrow_whenIdNotBackfilled() {
				
				ProjectDto dto = createDto("slug-1", "name-1", "value-1", "summary-1", List.of(new ProjectStackDto("stack1")));
				
				// 模擬資料庫插入成功，但沒有回填 ID (ID 依然是 null)
				when(projectBaseService.insert(any(Project.class))).thenAnswer(invocation -> {
						Project p = invocation.getArgument(0);
						// p.setId(100);
						return true;
				});
				
				assertThatThrownBy(() -> projectService.addProject(userId, dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("添加失敗");
				
				verify(projectStacksService, never()).insertBatch(anyList());
		}
		
		@Test
		void editProject_shouldThrow_whenProjectNotFound() {
				
				ProjectDto dto = createDto("slug-1", "name-1", "value-1", "summary-1", List.of(new ProjectStackDto("stack1")));
				
				when(projectBaseService.selectOneBySlug(userId, "slug-1")).thenReturn(null);
				
				assertThatThrownBy(() -> projectService.editProject(userId, "slug-1", dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("找不到對應的項目");
				
				verify(projectBaseService, never()).updateByPK(any());
				verifyNoInteractions(projectDetailService, projectStacksService);
				
		}
		
		@Test
		void editProject_shouldThrow_whenNewSlugIsRepeated() {
				ProjectDto dto = createDto("new-slug", "name-1", "value-1", "summary-1", List.of(new ProjectStackDto("stack1")));
				String newSlug = dto.slug();
				
				when(projectBaseService.selectOneBySlug(userId, "slug")).thenReturn(project(1, "slug"));
				when(projectBaseService.selectOneBySlug(userId, newSlug)).thenReturn(project(2, newSlug));
				
				assertThatThrownBy(() -> projectService.editProject(userId, "slug", dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("該 slug 已被使用，請更換一個");
				
				verify(projectBaseService, never()).updateByPK(any());
				verifyNoInteractions(projectDetailService, projectStacksService);
				
		}
		
		@Test
		void editProject_shouldThrow_whenUpdateProjectFailed() {
				ProjectDto dto = createDto("slug-1", "name-1", "value-1", "summary-1", List.of(new ProjectStackDto("stack1")));
				
				when(projectBaseService.selectOneBySlug(userId, "slug-1")).thenReturn(project(1, "slug-1"));
				when(projectBaseService.updateByPK(any())).thenReturn(false);
				
				assertThatThrownBy(() -> projectService.editProject(userId, "slug-1", dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("更新項目失敗");
				
				verifyNoInteractions(projectDetailService, projectStacksService);
		}
		
		@Test
		void editProject_shouldThrow_whenDetailNotFound() {
				ProjectDto dto = createDto("slug-1", "name-1", "value-1", "summary-1", List.of(new ProjectStackDto("stack1")));
				
				when(projectBaseService.selectOneBySlug(userId, "slug-1")).thenReturn(project(1, "slug-1"));
				when(projectBaseService.updateByPK(any())).thenReturn(true);
				
				when(projectDetailService.selectOne(anyInt())).thenReturn(null);
				
				assertThatThrownBy(() -> projectService.editProject(userId, "slug-1", dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("找不到對應的項目細節");
				
				verify(projectDetailService, never()).updateByPK(any());
				verifyNoInteractions(projectStacksService);
		}
		
		@Test
		void editProject_shouldThrow_whenDetailUpdateFailed() {
				ProjectDto dto = createDto("slug-1", "name-1", "value-1", "summary-1", List.of(new ProjectStackDto("stack1")));
				
				ProjectDetail projectDetail = new ProjectDetail();
				projectDetail.setProjectId(1);
				
				when(projectBaseService.selectOneBySlug(userId, "slug-1")).thenReturn(project(1, "slug-1"));
				when(projectBaseService.updateByPK(any())).thenReturn(true);
				
				when(projectDetailService.selectOne(anyInt())).thenReturn(projectDetail);
				when(projectDetailService.updateByPK(any())).thenReturn(false);
				
				assertThatThrownBy(() -> projectService.editProject(userId, "slug-1", dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("更新項目細節失敗");
				
				verifyNoInteractions(projectStacksService);
		}
		
		@Test
		void editProject_shouldThrow_whenStackRemoveFailed() {
				ProjectDto dto = createDto("slug-1", "name-1", "value-1", "summary-1", List.of(new ProjectStackDto("stack1")));
				
				ProjectDetail projectDetail = new ProjectDetail();
				projectDetail.setProjectId(1);
				
				when(projectBaseService.selectOneBySlug(userId, "slug-1")).thenReturn(project(1, "slug-1"));
				when(projectBaseService.updateByPK(any())).thenReturn(true);
				
				when(projectDetailService.selectOne(anyInt())).thenReturn(projectDetail);
				when(projectDetailService.updateByPK(any())).thenReturn(true);
				
				when(projectStacksService.deleteByProjectId(anyInt())).thenReturn(false);
				
				assertThatThrownBy(() -> projectService.editProject(userId, "slug-1", dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("更新項目技術棧失敗");
				
				verify(projectStacksService, never()).insertBatch(anyList());
		}
		
		@Test
		void editProject_shouldSuccess_whenAllServicesPass() {
				ProjectDto dto = createDto("new-slug", "new-name", "new-value", "new-summary", List.of(new ProjectStackDto("stack1")));
				String newSlug = dto.slug();
				
				Project existing = new Project();
				existing.setId(100);
				existing.setSlug("slug-1");
				existing.setSortOrder(50);
				
				when(projectBaseService.selectOneBySlug(userId, "slug-1")).thenReturn(existing);
				when(projectBaseService.selectOneBySlug(userId, newSlug)).thenReturn(null);
				when(projectBaseService.updateByPK(any(Project.class))).thenReturn(true);
				
				ProjectDetail existingDetail = new ProjectDetail();
				existingDetail.setProjectId(100);
				when(projectDetailService.selectOne(100)).thenReturn(existingDetail);
				when(projectDetailService.updateByPK(any(ProjectDetail.class))).thenReturn(true);
				
				when(projectStacksService.deleteByProjectId(100)).thenReturn(true);
				when(projectStacksService.insertBatch(anyList())).thenReturn(true);
				
				BaseVo<Void> result = projectService.editProject(userId, "slug-1", dto);
				
				ArgumentCaptor<ProjectDetail> detailCaptor = ArgumentCaptor.forClass(ProjectDetail.class);
				verify(projectDetailService).updateByPK(detailCaptor.capture());
				assertThat(detailCaptor.getValue().getProjectId()).isEqualTo(100);
				
				assertThat(result.getIsSuccess()).isTrue();
		}
		
		@Test
		void deleteProject_shouldThrow_whenProjectNotFound() {
				when(projectBaseService.selectOneBySlug(userId, "slug-1")).thenReturn(null);
				
				assertThatThrownBy(() -> projectService.deleteProject(userId, "slug-1"))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("刪除失敗，資料不存在");
				
				verify(projectBaseService, never()).deleteByPKAndUser(any(), any());
		}
		
		@Test
		void deleteProject_shouldReturnSuccess_whenDeleteSucceeds() {
				when(projectBaseService.selectOneBySlug(userId, "slug-1")).thenReturn(project(100, "slug-1"));
				when(projectBaseService.deleteByPKAndUser(userId, 100)).thenReturn(true);
				
				BaseVo<Void> result = projectService.deleteProject(userId, "slug-1");
				
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData()).isNull();
				
				// userId 和 id 必須原封不動傳給下游（保護「傳錯 userId 導致刪到別人資料」）
				verify(projectBaseService).deleteByPKAndUser(userId, 100);
		}
		
		@Test
		void deleteProject_shouldNotTouchOtherServices_whenDeleteSucceeds() {
				when(projectBaseService.selectOneBySlug(userId, "slug-1")).thenReturn(project(100, "slug-1"));
				when(projectBaseService.deleteByPKAndUser(userId, 100)).thenReturn(true);
				
				projectService.deleteProject(userId, "slug-1");
				
				// 刪除只委託 base service，不應碰 detail / stacks（依賴 DB CASCADE）
				verify(projectBaseService).deleteByPKAndUser(userId, 100);
				verifyNoMoreInteractions(projectBaseService);
				verifyNoInteractions(projectDetailService, projectStacksService);
		}
		
		@Test
		void reorderProject_shouldThrow_whenSlugsInValid() {
				ProjectSortDto dto = new ProjectSortDto(List.of("slug-1", "   "));
				
				assertThatThrownBy(() -> projectService.reorderProject(userId, dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("參數缺失");
				
				verifyNoInteractions(projectBaseService);
		}
		
		@Test
		void reorderProject_shouldThrow_whenSlugRepeated() {
				ProjectSortDto dto = new ProjectSortDto(List.of("slug-1", "slug-1"));
				
				assertThatThrownBy(() -> projectService.reorderProject(userId, dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("傳入的 slug 包含重複項");
				
				verifyNoInteractions(projectBaseService);
		}
		
		@Test
		void reorderProject_shouldThrow_whenUserNoProject() {
				ProjectSortDto dto = new ProjectSortDto(List.of("slug-1", "slug-2"));
				
				when(projectBaseService.selectList(userId, false)).thenReturn(List.of());
				
				assertThatThrownBy(() -> projectService.reorderProject(userId, dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("暫無項目，請添加項目後重試");
				
				verifyNoMoreInteractions(projectBaseService);
		}
		
		@Test
		void reorderProject_shouldReturnSuccessAndNotUpdate_whenUserHasOnlyOneProject() {
				ProjectSortDto dto = new ProjectSortDto(List.of("slug-1"));
				when(projectBaseService.selectList(userId, false))
								.thenReturn(List.of(project(1, "slug-1")));
				
				BaseVo<Void> result = projectService.reorderProject(userId, dto);
				
				assertThat(result.getIsSuccess()).isTrue();
				
				verify(projectBaseService, never()).updateBatchById(anyList());
		}
		
		@Test
		void reorderProject_shouldThrow_whenUserProjectSizeNotEqualsToDto() {
				ProjectSortDto dto = new ProjectSortDto(List.of("slug-1", "slug-2", "slug-3"));
				List<Project> projectList = List.of(
								project(1, "slug-1"),
								project(2, "slug-2")
				);
				
				when(projectBaseService.selectList(userId, false)).thenReturn(projectList);
				
				assertThatThrownBy(() -> projectService.reorderProject(userId, dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("slug 缺失或多餘，請重新整理後重試");
				
				verifyNoMoreInteractions(projectBaseService);
		}
		
		@Test
		void reorderProject_shouldThrow_whenUserProjectSlugsMisMatchDto() {
				ProjectSortDto dto = new ProjectSortDto(List.of("slug-1", "slug-2", "slug-3"));
				List<Project> projectList = List.of(
								project(1, "slug-1"),
								project(2, "slug-2"),
								project(3, "slug-4")
				);
				
				when(projectBaseService.selectList(userId, false)).thenReturn(projectList);
				
				assertThatThrownBy(() -> projectService.reorderProject(userId, dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("slug 不匹配，包含無效的項目");
				
				verifyNoMoreInteractions(projectBaseService);
		}
		
		@Test
		void reorderProject_shouldThrow_whenUpdateFailed() {
				ProjectSortDto dto = new ProjectSortDto(List.of("slug-2", "slug-1"));
				List<Project> projectList = List.of(
								project(1, "slug-1"),
								project(2, "slug-2")
				);
				
				when(projectBaseService.selectList(userId, false)).thenReturn(projectList);
				when(projectBaseService.updateBatchById(anyList())).thenReturn(false);
				
				assertThatThrownBy(() -> projectService.reorderProject(userId, dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("更新排序失敗");
				
		}
		
		
		@Test
		void reorderProject_shouldSuccess_whenValidRequest() {
				ProjectSortDto dto = new ProjectSortDto(List.of("slug-2", "slug-1"));
				List<Project> projectList = List.of(
								project(1, "slug-1"),
								project(2, "slug-2")
				);
				
				when(projectBaseService.selectList(userId, false)).thenReturn(projectList);
				when(projectBaseService.updateBatchById(anyList())).thenReturn(true);
				
				BaseVo<Void> result = projectService.reorderProject(userId, dto);
				
				assertThat(result.getIsSuccess()).isTrue();
				
				
				ArgumentCaptor<List<Project>> captor = ArgumentCaptor.forClass(List.class);
				verify(projectBaseService).updateBatchById(captor.capture());
				
				List<Project> updatedProjects = captor.getValue();
				
				assertThat(updatedProjects).hasSize(2);
				Project first = updatedProjects.get(0);
				assertThat(first.getId()).isEqualTo(2);
				assertThat(first.getSortOrder()).isEqualTo(10);
				
				Project second = updatedProjects.get(1);
				assertThat(second.getId()).isEqualTo(1);
				assertThat(second.getSortOrder()).isEqualTo(20);
		}
		
}