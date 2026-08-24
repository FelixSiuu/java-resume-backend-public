package com.felixsiu.resume.service;

import com.felixsiu.resume.model.dto.EduDto;
import com.felixsiu.resume.model.entity.Education;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.respository.EducationMapper;
import com.felixsiu.resume.utils.MyRedisTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EducationServiceTest {
		
		@Mock
		private EducationMapper educationMapper;
		
		@Mock
		private MyRedisTemplate myRedisTemplate;
		
		@InjectMocks
		private EducationService educationService;
		
		private String userId = "0a6b605e358a13e36cc3138213d64690";
		
		@Test
		void getEducation_shouldReturnEducationList_whenUserExists() {
				List<Education> mockList = new ArrayList<>();
				
				Education education = new Education();
				education.setMajor("marketing");
				education.setEndDate("2024-09");
				
				mockList.add(education);
				
				when(myRedisTemplate.get(anyString(), any())).thenReturn(null);
				when(educationMapper.selectList(any())).thenReturn(mockList);
				
				BaseVo<List<Education>> result = educationService.getEducation(userId);
				
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData().size() == 1);
				assertThat(result.getData().get(0).getMajor().contains("marketing"));
				
				verify(myRedisTemplate, times(1)).get(anyString(), any());
		}
		
		@Test
		void getEducation_shouldReturnEducationList_whenCacheExists() {
				List<Education> mockList = new ArrayList<>();
				
				Education education = new Education();
				education.setMajor("Cached Data");
				
				mockList.add(education);
				
				when(myRedisTemplate.get(anyString(), any())).thenReturn(mockList);
				
				BaseVo<List<Education>> result = educationService.getEducation(userId);
				
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData().size() == 1);
				assertThat(result.getData().get(0).getMajor().equals("Cached Data"));
				
				verify(educationMapper, never()).selectList(any());
		}
		
		@Test
		void getEducation_shouldReturnEmptyList_whenUserNotExists() {
				final String fakeUserId = "fake_id";
				
				when(myRedisTemplate.get(anyString(), any())).thenReturn(null);
				
				when(educationMapper.selectList(any())).thenReturn(new ArrayList<>());
				
				BaseVo<List<Education>> result = educationService.getEducation(fakeUserId);
				
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData().isEmpty());
		}
		
		@Test
		void editEducation_shouldSuccess_whenValid() {
				Integer eduId = 1;
				EduDto dto = new EduDto(
								"2024-09-01",
								"2026-06-30",
								"Updated University",
								"Bachelor of Computing",
								"Updated Major",
								"Updated Location"
				);
				
				when(educationMapper.updateById(any(Education.class))).thenReturn(1);
				
				BaseVo<Void> result = educationService.editEducation(eduId, dto, userId);
				
				assertThat(result.getIsSuccess()).isTrue();
				verify(educationMapper, times(1)).updateById(any(Education.class));
				verify(myRedisTemplate, times(1)).delete(anyString());
		}
		
		@Test
		void addEducation_shouldSuccess_whenAllFieldsValid() {
				EduDto dto = new EduDto(
								"2024-09-01",
								"2026-06-30",
								"National University of Singapore",
								"Bachelor of Computing",
								"Computer Science",
								"Singapore"
				);
				
				when(educationMapper.insert(any(Education.class))).thenReturn(1);
				
				BaseVo<Void> result = educationService.addEducation(dto, userId);
				
				assertThat(result.getIsSuccess()).isTrue();
				verify(educationMapper, times(1)).insert(any(Education.class));
				verify(myRedisTemplate, times(1)).delete(anyString());
		}
		
		@Test
		void addEducation_shouldThrowException_whenStartDateIsEmpty() {
				EduDto dto = new EduDto(
								"",   // startDate 為空
								"2026-06-30",
								"National University of Singapore",
								"Bachelor of Computing",
								"Computer Science",
								"Singapore"
				);
				
				assertThrows(RuntimeException.class, () -> educationService.addEducation(dto, userId));
				verify(educationMapper, never()).insert(any(Education.class));
		}
		
		@Test
		void addEducation_shouldSuccess_whenDegreeIsEmptyString() {
				EduDto dto = new EduDto(
								"2024-09-01",
								"2026-06-30",
								"National University of Singapore",
								"",   // degree 為空字串（選填）
								"Computer Science",
								"Singapore"
				);
				
				when(educationMapper.insert(any(Education.class))).thenReturn(1);
				
				BaseVo<Void> result = educationService.addEducation(dto, userId);
				
				assertThat(result.getIsSuccess()).isTrue();
		}
		
		@Test
		void addEducation_shouldSuccess_whenEndDateBeforeStartDate() {
				EduDto dto = new EduDto(
								"2026-09-01",   // 開始日期在結束日期之後
								"2024-06-30",   // 結束日期在開始日期之前
								"National University of Singapore",
								"Bachelor of Computing",
								"Computer Science",
								"Singapore"
				);
				
				when(educationMapper.insert(any(Education.class))).thenReturn(1);
				
				BaseVo<Void> result = educationService.addEducation(dto, userId);
				
				assertThat(result.getIsSuccess()).isTrue();
		}
		
		@Test
		void deleteEducation_shouldSuccess_whenExists() {
				when(educationMapper.delete(any())).thenReturn(1);
				
				BaseVo<Void> result = educationService.deleteEducation(1, userId);
				
				assertThat(result.getIsSuccess()).isTrue();
				verify(myRedisTemplate, times(1)).delete(anyString());
		}
		
		@Test
		void deleteEducation_shouldThrowException_whenNotExists() {
				when(educationMapper.delete(any())).thenReturn(0);
				
				assertThrows(RuntimeException.class,
								() -> educationService.deleteEducation(999, userId));
		}
}