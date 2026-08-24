package com.felixsiu.resume.service;

import com.felixsiu.resume.model.dto.AboutDto;
import com.felixsiu.resume.model.entity.About;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.respository.AboutMapper;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AboutServiceTest {
		
		@Mock
		private AboutMapper aboutMapper;
		
		@Mock
		private MyRedisTemplate myRedisTemplate;
		
		@InjectMocks
		private AboutService aboutService;
		
		private String userId = "0a6b605e358a13e36cc3138213d64690";
		
		@Test
		void getAboutMe_shouldReturnAboutList_whenUserExists() {
				List<About> mockAboutList = new ArrayList<>();
				
				About about1 = new About();
				about1.setId(1);
				about1.setUserId(userId);
				about1.setParagraph("Hello World");
				about1.setCreateTime(LocalDateTime.now());
				
				About about2 = new About();
				about2.setId(2);
				about2.setUserId(userId);
				about2.setParagraph("Java Developer");
				about2.setCreateTime(LocalDateTime.now());
				
				mockAboutList.add(about1);
				mockAboutList.add(about2);
				
				when(myRedisTemplate.get(anyString(), any())).thenReturn(null);
				when(aboutMapper.selectList(any())).thenReturn(mockAboutList);
				
				BaseVo<List<About>> result = aboutService.getAboutMe(userId);
				
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData().size() == 2);
				assertThat(result.getData().get(0).getParagraph().contains("Hello"));
				
				//  驗證 Redis 有被呼叫（但沒有真的連線）
				verify(myRedisTemplate, times(1)).get(anyString(), any());
				verify(myRedisTemplate, times(1)).set(anyString(), any(), anyLong(), any());
				
				// 驗證 Mapper 被呼叫（因為快取未命中）
				verify(aboutMapper, times(1)).selectList(any());
		}
		
		@Test
		void getAboutMe_shouldReturnFromCache_whenCacheExists() {
				List<About> mockCachedList = new ArrayList<>();
				About about = new About();
				about.setId(1);
				about.setUserId(userId);
				about.setParagraph("Cached Data");
				mockCachedList.add(about);
				
				when(myRedisTemplate.get(anyString(), any())).thenReturn(mockCachedList);
				
				BaseVo<List<About>> result = aboutService.getAboutMe(userId);
				
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData()).hasSize(1);
				assertThat(result.getData().get(0).getParagraph()).isEqualTo("Cached Data");
				
				// 驗證：Mapper 應該沒有被呼叫
				verify(aboutMapper, never()).selectList(any());
		}
		
		@Test
		void getAboutMe_shouldReturnEmptyList_whenUserNotExists() {
				final String fakeUserId = "fake_id";
				
				when(myRedisTemplate.get(anyString(), any())).thenReturn(null);
				
				when(aboutMapper.selectList(any())).thenReturn(new ArrayList<>());
				
				BaseVo<List<About>> result = aboutService.getAboutMe(fakeUserId);
				
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData().isEmpty());
		}
		
		@Test
		void editAbout_shouldUpdateSuccessfully_whenValidInput() {
				Integer aboutId = 1;
				
				AboutDto aboutDto = new AboutDto("Updated Paragraph");
				
				when(aboutMapper.updateById(any(About.class))).thenReturn(1);
				
				BaseVo<Void> result = aboutService.editAbout(aboutId, aboutDto, userId);
				
				assertThat(result.getIsSuccess()).isTrue();
				
				// 驗證 Redis 快取被清除
				verify(myRedisTemplate, times(1)).delete(anyString());
		}
		
		@Test
		void editAbout_shouldThrowException_whenInputEmpty() {
				AboutDto aboutDto = new AboutDto("");
				
				// 模擬拋出異常
				assertThrows(
								RuntimeException.class,
								() -> aboutService.editAbout(1, aboutDto, userId)
				);
				
				// 驗證：不應該呼叫 updateById
				verify(aboutMapper, never()).updateById(any(About.class));
		}
		
		@Test
		void editAbout_shouldDeleteCorrectCacheKey() {
				Integer aboutId = 1;
				AboutDto aboutDto = new AboutDto("Updated Paragraph");
				when(aboutMapper.updateById(any(About.class))).thenReturn(1);
				
				aboutService.editAbout(aboutId, aboutDto, userId);
				
				// 驗證刪除正確的 Key
				ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
				verify(myRedisTemplate).delete(keyCaptor.capture());
				
				String expectedKey = "user:about:" + userId;
				assertThat(keyCaptor.getValue()).isEqualTo(expectedKey);
		}
		
		@Test
		void deleteAbout_shouldDeleteSuccessfully_whenRecordExists() {
				when(aboutMapper.delete(any())).thenReturn(1);
				
				BaseVo<Void> result = aboutService.deleteAbout(1, userId);
				
				assertThat(result.getIsSuccess()).isTrue();
				verify(myRedisTemplate, times(1)).delete(anyString());
		}
		
		@Test
		void deleteAbout_shouldThrowException_whenRecordNotExists() {
				when(aboutMapper.delete(any())).thenReturn(0);
				
				assertThrows(RuntimeException.class, () -> aboutService.deleteAbout(999, userId));
		}
		
		@Test
		void addAbout_shouldAddSuccessfully_whenValidInput() {
				AboutDto aboutDto = new AboutDto("Add Paragraph");
				
				when(aboutMapper.insert(any(About.class))).thenReturn(1);
				
				BaseVo<Void> result = aboutService.addAbout(aboutDto, userId);
				
				assertThat(result.getIsSuccess()).isTrue();
				
				verify(myRedisTemplate, times(1)).delete(anyString());
		}
		
		@Test
		void addAbout_shouldThrowException_whenValidEmpty() {
				AboutDto aboutDto = new AboutDto("");
				
				assertThrows(
								RuntimeException.class,
								() -> aboutService.addAbout(aboutDto, userId)
				);
				
				verify(aboutMapper, never()).insert(any(About.class));
		}
}