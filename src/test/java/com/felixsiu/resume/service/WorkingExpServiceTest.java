package com.felixsiu.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.felixsiu.resume.model.dto.KeyRespDto;
import com.felixsiu.resume.model.dto.WorkingExpDto;
import com.felixsiu.resume.model.entity.KeyResponsibilities;
import com.felixsiu.resume.model.entity.WorkingExperience;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.model.vo.WorkingExperienceVo;
import com.felixsiu.resume.respository.KeyResponsibilitiesMapper;
import com.felixsiu.resume.respository.WorkingExperienceMapper;
import com.felixsiu.resume.utils.CacheKeys;
import com.felixsiu.resume.utils.MyRedisTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkingExpServiceTest {
		
		@Mock
		private WorkingExperienceMapper workingExperienceMapper;
		
		@Mock
		private KeyResponsibilitiesMapper keyResponsibilitiesMapper;
		
		@Mock
		private MyRedisTemplate myRedisTemplate;
		
		@InjectMocks
		private WorkingExpService workingExpService;
		
		private final String userId = "test-user-123";
		
		// ========== 測試數據工廠 ==========
		
		private WorkingExperience createWorkingExperience(Integer id, String startDate, String endDate,
		                                                  String jobTitle, String company, String location) {
				WorkingExperience we = new WorkingExperience();
				we.setId(id);
				we.setUserId(userId);
				we.setStartDate(startDate);
				we.setEndDate(endDate);
				we.setJobTitle(jobTitle);
				we.setCompany(company);
				we.setLocation(location);
				we.setCreateTime(LocalDateTime.now());
				we.setUpdateTime(LocalDateTime.now());
				return we;
		}
		
		private KeyResponsibilities createKeyResponsibility(Integer id, Integer workingId, String title, String responsibility) {
				KeyResponsibilities kr = new KeyResponsibilities();
				kr.setId(id);
				kr.setUserId(userId);
				kr.setWorkingId(workingId);
				kr.setTitle(title);
				kr.setResponsibility(responsibility);
				return kr;
		}
		
		private KeyRespDto createKeyRespDto(Integer workingId, String title, String responsibility) {
				return new KeyRespDto(workingId, title, responsibility);
		}
		
		private WorkingExpDto createWorkingExpDto(String startDate, String endDate, String jobTitle,
		                                          String company, String location, List<KeyRespDto> responsibilities) {
				return new WorkingExpDto(startDate, endDate, jobTitle, company, location, responsibilities);
		}
		
		// ============================================================
		// 1. getWorkingExperiences 測試
		// ============================================================
		
		@Test
		void getWorkingExperiences_shouldReturnFromCache_whenCacheExists() {
				// 準備：Redis 有快取
				List<WorkingExperienceVo> mockCachedList = new ArrayList<>();
				WorkingExperienceVo vo = new WorkingExperienceVo();
				vo.setId(1);
				vo.setJobTitle("Frontend Developer");
				vo.setCompany("Tech Corp");
				vo.setLocation("Hong Kong");
				mockCachedList.add(vo);
				
				String cacheKey = CacheKeys.experience(userId);
				
				when(myRedisTemplate.get(eq(cacheKey), any(TypeReference.class))).thenReturn(mockCachedList);
				
				// 執行
				BaseVo<List<WorkingExperienceVo>> result = workingExpService.getWorkingExperiences(userId);
				
				// 驗證
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData()).hasSize(1);
				assertThat(result.getData().get(0).getJobTitle()).isEqualTo("Frontend Developer");
				
				verify(myRedisTemplate, times(1)).get(eq(cacheKey), any(TypeReference.class));
				verify(workingExperienceMapper, never()).selectList(any());
				verify(keyResponsibilitiesMapper, never()).selectList(any());
				verify(myRedisTemplate, never()).set(anyString(), any(), anyLong(), any());
		}
		
		@Test
		void getWorkingExperiences_shouldQueryDatabase_whenCacheMiss() {
				// 準備：Redis 回傳 null
				List<WorkingExperience> mockWeList = List.of(
								createWorkingExperience(1, "2023-05", null, "Frontend Developer", "Tech Corp", "Hong Kong"),
								createWorkingExperience(2, "2022-05", "2023-05", "Web Developer", "SunnyVision", "Hong Kong")
				);
				
				List<KeyResponsibilities> mockKrList = List.of(
								createKeyResponsibility(1, 1, "Core Dev", "React development"),
								createKeyResponsibility(2, 1, "UI/UX", "Figma to code"),
								createKeyResponsibility(3, 2, "Gov Projects", "WCAG compliance")
				);
				
				String cacheKey = CacheKeys.experience(userId);
				
				when(myRedisTemplate.get(eq(cacheKey), any(TypeReference.class))).thenReturn(null);
				when(workingExperienceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(mockWeList);
				when(keyResponsibilitiesMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(mockKrList);
				
				// 執行
				BaseVo<List<WorkingExperienceVo>> result = workingExpService.getWorkingExperiences(userId);
				
				// 驗證
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData()).hasSize(2);
				
				// 驗證：第一次查詢工作經歷，第二次查詢職責，然後寫入快取
				verify(workingExperienceMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
				verify(keyResponsibilitiesMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
				verify(myRedisTemplate, times(1)).set(eq(cacheKey), anyList(), anyLong(), any());
		}
		
		@Test
		void getWorkingExperiences_shouldReturnEmptyList_whenUserHasNoExperience() {
				// 準備：用戶沒有工作經歷
				String cacheKey = CacheKeys.experience(userId);
				
				when(myRedisTemplate.get(eq(cacheKey), any(TypeReference.class))).thenReturn(null);
				when(workingExperienceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
				when(keyResponsibilitiesMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
				
				// 執行
				BaseVo<List<WorkingExperienceVo>> result = workingExpService.getWorkingExperiences(userId);
				
				// 驗證
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData()).isEmpty();
				
				verify(myRedisTemplate, times(1)).set(eq(cacheKey), anyList(), anyLong(), any());
		}
		
		// ============================================================
		// 2. addWorkingExperiences 測試
		// ============================================================
		
		@Test
		void addWorkingExperiences_shouldSuccess_whenValidInput() {
				// 準備
				List<KeyRespDto> responsibilities = List.of(
								createKeyRespDto(1, "Core Dev", "React development"),
								createKeyRespDto(1, "UI/UX", "Figma to code")
				);
				
				WorkingExpDto dto = createWorkingExpDto(
								"2023-05", null, "Frontend Developer",
								"Tech Corp", "Hong Kong", responsibilities
				);
				
				// 模擬插入主表成功
				when(workingExperienceMapper.insert(any(WorkingExperience.class))).thenAnswer(invocation -> {
						WorkingExperience we = invocation.getArgument(0);
						we.setId(1);   // 模拟数据库回填 ID
						return 1;
				});
				
				// 模擬批量插入副表成功
				when(keyResponsibilitiesMapper.insert(any(KeyResponsibilities.class))).thenReturn(2);
				
				// 執行
				BaseVo<Void> result = workingExpService.addWorkingExperiences(dto, userId);
				
				// 驗證
				assertThat(result.getIsSuccess()).isTrue();
				
				verify(workingExperienceMapper, times(1)).insert(any(WorkingExperience.class));
				
				ArgumentCaptor<KeyResponsibilities> captor =
								ArgumentCaptor.forClass(KeyResponsibilities.class);
				verify(keyResponsibilitiesMapper, times(2)).insert(captor.capture());
				for (KeyResponsibilities captured : captor.getAllValues()) {
						System.out.println("workingId = " + captured.getWorkingId());
						assertThat(captured.getWorkingId()).isEqualTo(1);
				}
				
				verify(myRedisTemplate, times(1)).delete(CacheKeys.experience(userId));
		}
		
		@Test
		void addWorkingExperiences_shouldSuccess_whenNoResponsibilities() {
				// 準備：沒有職責列表
				WorkingExpDto dto = createWorkingExpDto(
								"2023-05", null, "Frontend Developer",
								"Tech Corp", "Hong Kong", new ArrayList<>()
				);
				
				when(workingExperienceMapper.insert(any(WorkingExperience.class))).thenReturn(1);
				
				// 執行
				BaseVo<Void> result = workingExpService.addWorkingExperiences(dto, userId);
				
				// 驗證
				assertThat(result.getIsSuccess()).isTrue();
				
				verify(workingExperienceMapper, times(1)).insert(any(WorkingExperience.class));
				verify(keyResponsibilitiesMapper, never()).insert(any(KeyResponsibilities.class));
				verify(myRedisTemplate, times(1)).delete(CacheKeys.experience(userId));
		}
		
		@Test
		void addWorkingExperiences_shouldThrowException_whenStartDateIsEmpty() {
				// 準備：startDate 為空
				WorkingExpDto dto = createWorkingExpDto(
								"", null, "Frontend Developer",
								"Tech Corp", "Hong Kong", new ArrayList<>()
				);
				
				// 執行 & 驗證
				assertThatThrownBy(() -> workingExpService.addWorkingExperiences(dto, userId))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("參數缺失");
				
				verify(workingExperienceMapper, never()).insert(any(WorkingExperience.class));
				verify(keyResponsibilitiesMapper, never()).insert(any(KeyResponsibilities.class));
				verify(myRedisTemplate, never()).delete(anyString());
		}
		
		@Test
		void addWorkingExperiences_shouldThrowException_whenJobTitleIsEmpty() {
				// 準備：jobTitle 為空
				WorkingExpDto dto = createWorkingExpDto(
								"2023-05", null, "",
								"Tech Corp", "Hong Kong", new ArrayList<>()
				);
				
				assertThatThrownBy(() -> workingExpService.addWorkingExperiences(dto, userId))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("參數缺失");
				
				verify(workingExperienceMapper, never()).insert(any(WorkingExperience.class));
		}
		
		@Test
		void addWorkingExperiences_shouldThrowException_whenCompanyIsEmpty() {
				// 準備：company 為空
				WorkingExpDto dto = createWorkingExpDto(
								"2023-05", null, "Frontend Developer",
								"", "Hong Kong", new ArrayList<>()
				);
				
				assertThatThrownBy(() -> workingExpService.addWorkingExperiences(dto, userId))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("參數缺失");
		}
		
		@Test
		void addWorkingExperiences_shouldThrowException_whenLocationIsEmpty() {
				// 準備：location 為空
				WorkingExpDto dto = createWorkingExpDto(
								"2023-05", null, "Frontend Developer",
								"Tech Corp", "", new ArrayList<>()
				);
				
				assertThatThrownBy(() -> workingExpService.addWorkingExperiences(dto, userId))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("參數缺失");
		}
		
		@Test
		void addWorkingExperiences_shouldThrowException_whenDtoIsNull() {
				assertThatThrownBy(() -> workingExpService.addWorkingExperiences(null, userId))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("參數缺失");
		}
		
		@Test
		void addWorkingExperiences_shouldThrowException_whenInsertFails() {
				// 準備：插入主表失敗
				WorkingExpDto dto = createWorkingExpDto(
								"2023-05", null, "Frontend Developer",
								"Tech Corp", "Hong Kong", new ArrayList<>()
				);
				
				when(workingExperienceMapper.insert(any(WorkingExperience.class))).thenReturn(0);
				
				assertThatThrownBy(() -> workingExpService.addWorkingExperiences(dto, userId))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("添加失敗");
				
				verify(myRedisTemplate, never()).delete(anyString());
		}
		
		// ============================================================
		// 3. editWorkingExperiences 測試
		// ============================================================
		
		@Test
		void editWorkingExperiences_shouldSuccess_whenValidInput() {
				// 準備
				Integer expId = 1;
				List<KeyRespDto> responsibilities = List.of(
								createKeyRespDto(1, "Core Dev", "React development"),
								createKeyRespDto(2, "API", "REST API integration")
				);
				
				WorkingExpDto dto = createWorkingExpDto(
								"2023-05", "2024-12", "Senior Frontend Developer",
								"Tech Corp", "Hong Kong", responsibilities
				);
				
				// 模擬刪除舊的職責成功
				when(keyResponsibilitiesMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(2);
				// 模擬更新主表成功
				when(workingExperienceMapper.updateById(any(WorkingExperience.class))).thenReturn(1);
				
				// 執行
				BaseVo<Void> result = workingExpService.editWorkingExperiences(expId, dto, userId);
				
				// 驗證
				assertThat(result.getIsSuccess()).isTrue();
				
				verify(keyResponsibilitiesMapper, times(1)).delete(any(LambdaQueryWrapper.class));
				verify(keyResponsibilitiesMapper, atLeastOnce()).insert(any(KeyResponsibilities.class));
				verify(workingExperienceMapper, times(1)).updateById(any(WorkingExperience.class));
				verify(myRedisTemplate, times(1)).delete(CacheKeys.experience(userId));
		}
		
		@Test
		void editWorkingExperiences_shouldSuccess_whenNoResponsibilities() {
				// 準備：沒有職責
				Integer expId = 1;
				WorkingExpDto dto = createWorkingExpDto(
								"2023-05", "2024-12", "Senior Frontend Developer",
								"Tech Corp", "Hong Kong", new ArrayList<>()
				);
				
				when(keyResponsibilitiesMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(2);
				when(workingExperienceMapper.updateById(any(WorkingExperience.class))).thenReturn(1);
				
				// 執行
				BaseVo<Void> result = workingExpService.editWorkingExperiences(expId, dto, userId);
				
				// 驗證
				assertThat(result.getIsSuccess()).isTrue();
				
				verify(keyResponsibilitiesMapper, times(1)).delete(any(LambdaQueryWrapper.class));
				// 驗證：沒有插入新的職責
				verify(keyResponsibilitiesMapper, never()).insert(any(KeyResponsibilities.class));
				verify(workingExperienceMapper, times(1)).updateById(any(WorkingExperience.class));
				verify(myRedisTemplate, times(1)).delete(CacheKeys.experience(userId));
		}
		
		@Test
		void editWorkingExperiences_shouldThrowException_whenUpdateFails() {
				// 準備：更新主表失敗
				Integer expId = 1;
				WorkingExpDto dto = createWorkingExpDto(
								"2023-05", "2024-12", "Senior Frontend Developer",
								"Tech Corp", "Hong Kong", new ArrayList<>()
				);
				
				when(keyResponsibilitiesMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(2);
				when(workingExperienceMapper.updateById(any(WorkingExperience.class))).thenReturn(0);
				
				assertThatThrownBy(() -> workingExpService.editWorkingExperiences(expId, dto, userId))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("修改失敗");
				
				verify(myRedisTemplate, never()).delete(anyString());
		}
		
		@Test
		void editWorkingExperiences_shouldThrowException_whenDtoInvalid() {
				Integer expId = 1;
				WorkingExpDto dto = createWorkingExpDto(
								"", null, "",
								"Tech Corp", "", new ArrayList<>()
				);
				
				assertThatThrownBy(() -> workingExpService.editWorkingExperiences(expId, dto, userId))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("參數缺失");
				
				verify(keyResponsibilitiesMapper, never()).delete(any(LambdaQueryWrapper.class));
				verify(workingExperienceMapper, never()).updateById(any(WorkingExperience.class));
		}
		
		// ============================================================
		// 4. deleteWorkingExperiences 測試
		// ============================================================
		
		@Test
		void deleteWorkingExperiences_shouldSuccess_whenRecordExists() {
				// 準備
				Integer expId = 1;
				WorkingExperience mockWe = createWorkingExperience(1, "2023-05", null, "Frontend Developer", "Tech Corp", "Hong Kong");
				
				when(workingExperienceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(mockWe);
				when(keyResponsibilitiesMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(2);
				when(workingExperienceMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
				
				// 執行
				BaseVo<Void> result = workingExpService.deleteWorkingExperiences(expId, userId);
				
				// 驗證
				assertThat(result.getIsSuccess()).isTrue();
				
				verify(workingExperienceMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
				verify(keyResponsibilitiesMapper, times(1)).delete(any(LambdaQueryWrapper.class));
				verify(workingExperienceMapper, times(1)).delete(any(LambdaQueryWrapper.class));
				verify(myRedisTemplate, times(1)).delete(CacheKeys.experience(userId));
		}
		
		@Test
		void deleteWorkingExperiences_shouldThrowException_whenWorkingExperienceNotFound() {
				// 準備：主表找不到
				Integer expId = 999;
				
				when(workingExperienceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
				
				assertThatThrownBy(() -> workingExpService.deleteWorkingExperiences(expId, userId))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("刪除失敗");
				
				verify(keyResponsibilitiesMapper, never()).delete(any(LambdaQueryWrapper.class));
				verify(workingExperienceMapper, never()).delete(any(LambdaQueryWrapper.class));
				verify(myRedisTemplate, never()).delete(anyString());
		}
}