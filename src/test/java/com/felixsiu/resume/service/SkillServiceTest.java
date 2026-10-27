package com.felixsiu.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.felixsiu.resume.model.dto.SkillDto;
import com.felixsiu.resume.model.entity.CoreSkills;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.respository.CoreSkillsMapper;
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
class SkillServiceTest {
		
		@Mock
		private CoreSkillsMapper coreSkillsMapper;
		
		@Mock
		private MyRedisTemplate myRedisTemplate;
		
		@InjectMocks
		private SkillService skillService;
		
		private final String userId = "test-user-123";
		
		// ========== 測試數據工廠 ==========
		private CoreSkills createSkill(Integer id, String summary, String details) {
				CoreSkills skill = new CoreSkills();
				skill.setId(id);
				skill.setUserId(userId);
				skill.setSummary(summary);
				skill.setDetails(details);
				skill.setCreateTime(LocalDateTime.now());
				skill.setUpdateTime(LocalDateTime.now());
				return skill;
		}
		
		private SkillDto createSkillDto(String summary, String details) {
				return new SkillDto(summary, details);
		}
		
		// ============================================================
		// 1. getCoreSkills 測試
		// ============================================================
		
		@Test
		void getCoreSkills_shouldReturnFromCache_whenCacheExists() {
				// 準備：Redis 有快取
				List<CoreSkills> mockCachedList = List.of(
								createSkill(1, "Frontend", "React, TypeScript"),
								createSkill(2, "Backend", "Java, Spring Boot")
				);
				String cacheKey = CacheKeys.skills(userId);
				
				when(myRedisTemplate.get(eq(cacheKey), any(TypeReference.class))).thenReturn(mockCachedList);
				
				// 執行
				BaseVo<List<CoreSkills>> result = skillService.getCoreSkills(userId);
				
				// 驗證
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData()).hasSize(2);
				assertThat(result.getData().get(0).getSummary()).isEqualTo("Frontend");
				
				// 驗證：快取命中的情況下，不應該查資料庫
				verify(myRedisTemplate, times(1)).get(eq(cacheKey), any(TypeReference.class));
				verify(coreSkillsMapper, never()).selectList(any());
				verify(myRedisTemplate, never()).set(anyString(), any(), anyLong(), any());
		}
		
		@Test
		void getCoreSkills_shouldQueryDatabase_whenCacheMiss() {
				// 準備：Redis 回傳 null（快取未命中）
				List<CoreSkills> mockDbList = List.of(
								createSkill(1, "Frontend", "React, TypeScript"),
								createSkill(2, "Backend", "Java, Spring Boot")
				);
				String cacheKey = CacheKeys.skills(userId);
				
				when(myRedisTemplate.get(eq(cacheKey), any(TypeReference.class))).thenReturn(null);
				when(coreSkillsMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(mockDbList);
				
				// 執行
				BaseVo<List<CoreSkills>> result = skillService.getCoreSkills(userId);
				
				// 驗證
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData()).hasSize(2);
				
				// 驗證：查了資料庫，並寫入了快取
				verify(coreSkillsMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
				verify(myRedisTemplate, times(1)).set(eq(cacheKey), eq(mockDbList), anyLong(), any());
		}
		
		@Test
		void getCoreSkills_shouldReturnEmptyList_whenUserHasNoSkills() {
				// 準備：Redis 回傳 null，資料庫回傳空列表
				String cacheKey = CacheKeys.skills(userId);
				
				when(myRedisTemplate.get(eq(cacheKey), any(TypeReference.class))).thenReturn(null);
				when(coreSkillsMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
				
				// 執行
				BaseVo<List<CoreSkills>> result = skillService.getCoreSkills(userId);
				
				// 驗證
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData()).isEmpty();
				
				// 驗證：仍然寫入了空列表到快取（避免重複查詢資料庫）
				verify(myRedisTemplate, times(1)).set(eq(cacheKey), any(List.class), anyLong(), any());
		}
		
		// ============================================================
		// 2. addSkill 測試
		// ============================================================
		
		@Test
		void addSkill_shouldSuccess_whenValidInput() {
				// 準備
				SkillDto dto = createSkillDto("Frontend", "React, TypeScript, Next.js");
				
				when(coreSkillsMapper.insert(any(CoreSkills.class))).thenReturn(1);
				
				// 執行
				BaseVo<Void> result = skillService.addSkill(dto, userId);
				
				// 驗證
				assertThat(result.getIsSuccess()).isTrue();
				
				// 驗證參數
				ArgumentCaptor<CoreSkills> captor = ArgumentCaptor.forClass(CoreSkills.class);
				verify(coreSkillsMapper).insert(captor.capture());
				CoreSkills captured = captor.getValue();
				assertThat(captured.getUserId()).isEqualTo(userId);
				assertThat(captured.getSummary()).isEqualTo("Frontend");
				assertThat(captured.getDetails()).isEqualTo("React, TypeScript, Next.js");
				
				// 驗證快取被清除
				verify(myRedisTemplate, times(1)).delete(CacheKeys.skills(userId));
		}
		
		@Test
		void addSkill_shouldThrowException_whenSummaryIsEmpty() {
				// 準備：summary 為空
				SkillDto dto = createSkillDto("", "React, TypeScript");
				
				// 執行 & 驗證
				assertThatThrownBy(() -> skillService.addSkill(dto, userId))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("參數缺失");
				
				// 驗證：不應該呼叫 Mapper
				verify(coreSkillsMapper, never()).insert(any(CoreSkills.class));
				verify(myRedisTemplate, never()).delete(anyString());
		}
		
		@Test
		void addSkill_shouldThrowException_whenDetailsIsEmpty() {
				// 準備：details 為空
				SkillDto dto = createSkillDto("Frontend", "");
				
				// 執行 & 驗證
				assertThatThrownBy(() -> skillService.addSkill(dto, userId))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("參數缺失");
				
				verify(coreSkillsMapper, never()).insert(any(CoreSkills.class));
				verify(myRedisTemplate, never()).delete(anyString());
		}
		
		@Test
		void addSkill_shouldThrowException_whenDtoIsNull() {
				// 執行 & 驗證
				assertThatThrownBy(() -> skillService.addSkill(null, userId))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("參數缺失");
				
				verify(coreSkillsMapper, never()).insert(any(CoreSkills.class));
				verify(myRedisTemplate, never()).delete(anyString());
		}
		
		@Test
		void addSkill_shouldThrowException_whenInsertFails() {
				// 準備：Mapper 插入失敗（回傳 0）
				SkillDto dto = createSkillDto("Frontend", "React, TypeScript");
				
				when(coreSkillsMapper.insert(any(CoreSkills.class))).thenReturn(0);
				
				// 執行 & 驗證
				assertThatThrownBy(() -> skillService.addSkill(dto, userId))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("添加失敗");
				
				verify(coreSkillsMapper, times(1)).insert(any(CoreSkills.class));
				verify(myRedisTemplate, never()).delete(anyString());
		}
		
		// ============================================================
		// 3. editSkill 測試
		// ============================================================
		
		@Test
		void editSkill_shouldSuccess_whenValidInput() {
				// 準備
				Integer skillId = 1;
				SkillDto dto = createSkillDto("Updated Frontend", "React, TypeScript, Next.js, Tailwind");
				
				when(coreSkillsMapper.updateById(any(CoreSkills.class))).thenReturn(1);
				
				// 執行
				BaseVo<Void> result = skillService.editSkill(skillId, dto, userId);
				
				// 驗證
				assertThat(result.getIsSuccess()).isTrue();
				
				// 驗證參數
				ArgumentCaptor<CoreSkills> captor = ArgumentCaptor.forClass(CoreSkills.class);
				verify(coreSkillsMapper).updateById(captor.capture());
				CoreSkills captured = captor.getValue();
				assertThat(captured.getId()).isEqualTo(skillId);
				assertThat(captured.getUserId()).isEqualTo(userId);
				assertThat(captured.getSummary()).isEqualTo("Updated Frontend");
				assertThat(captured.getDetails()).isEqualTo("React, TypeScript, Next.js, Tailwind");
				
				// 驗證快取被清除
				verify(myRedisTemplate, times(1)).delete(CacheKeys.skills(userId));
		}
		
		@Test
		void editSkill_shouldThrowException_whenSummaryIsEmpty() {
				// 準備：summary 為空
				Integer skillId = 1;
				SkillDto dto = createSkillDto("", "React, TypeScript");
				
				// 執行 & 驗證
				assertThatThrownBy(() -> skillService.editSkill(skillId, dto, userId))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("參數缺失");
				
				verify(coreSkillsMapper, never()).updateById(any(CoreSkills.class));
				verify(myRedisTemplate, never()).delete(anyString());
		}
		
		@Test
		void editSkill_shouldThrowException_whenDetailsIsEmpty() {
				// 準備：details 為空
				Integer skillId = 1;
				SkillDto dto = createSkillDto("Frontend", "");
				
				assertThatThrownBy(() -> skillService.editSkill(skillId, dto, userId))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("參數缺失");
				
				verify(coreSkillsMapper, never()).updateById(any(CoreSkills.class));
				verify(myRedisTemplate, never()).delete(anyString());
		}
		
		@Test
		void editSkill_shouldThrowException_whenUpdateFails() {
				// 準備：更新失敗（回傳 0）
				Integer skillId = 1;
				SkillDto dto = createSkillDto("Updated Frontend", "React, TypeScript");
				
				when(coreSkillsMapper.updateById(any(CoreSkills.class))).thenReturn(0);
				
				// 執行 & 驗證
				assertThatThrownBy(() -> skillService.editSkill(skillId, dto, userId))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("修改失敗");
				
				verify(coreSkillsMapper, times(1)).updateById(any(CoreSkills.class));
				verify(myRedisTemplate, never()).delete(anyString());
		}
		
		// ============================================================
		// 4. deleteSkill 測試
		// ============================================================
		
		@Test
		void deleteSkill_shouldSuccess_whenSkillExists() {
				// 準備
				Integer skillId = 1;
				
				when(coreSkillsMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
				
				// 執行
				BaseVo<Void> result = skillService.deleteSkill(skillId, userId);
				
				// 驗證
				assertThat(result.getIsSuccess()).isTrue();
				
				// 驗證快取被清除
				verify(myRedisTemplate, times(1)).delete(CacheKeys.skills(userId));
				
				// 驗證 Wrapper 包含正確的條件
				ArgumentCaptor<LambdaQueryWrapper<CoreSkills>> captor =
								ArgumentCaptor.forClass(LambdaQueryWrapper.class);
				verify(coreSkillsMapper).delete(captor.capture());
				// 基本驗證 Wrapper 被建立
				assertThat(captor.getValue()).isNotNull();
		}
		
		@Test
		void deleteSkill_shouldThrowException_whenSkillNotFound() {
				// 準備：刪除失敗（回傳 0）
				Integer skillId = 999;
				
				when(coreSkillsMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);
				
				// 執行 & 驗證
				assertThatThrownBy(() -> skillService.deleteSkill(skillId, userId))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("刪除失敗");
				
				verify(myRedisTemplate, never()).delete(anyString());
		}
}