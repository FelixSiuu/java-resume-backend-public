package com.felixsiu.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.felixsiu.resume.model.dto.ContactDto;
import com.felixsiu.resume.model.entity.Contact;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.model.vo.ContactVo;
import com.felixsiu.resume.respository.ContactMapper;
import com.felixsiu.resume.utils.CacheKeys;
import com.felixsiu.resume.utils.MyRedisTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {
		
		@Mock
		private ContactMapper contactMapper;
		
		@Mock
		private MyRedisTemplate myRedisTemplate;
		
		@InjectMocks
		private ContactService contactService;
		
		private final String userId = "test-user-123";
		
		// ========== 測試數據工廠 ==========
		
		private Contact createContact(String userId) {
				Contact contact = new Contact();
				contact.setId(1);
				contact.setUserId(userId);
				contact.setDisplayName("Felix Siu");
				contact.setTitle("Full-Stack Developer");
				contact.setEmail("felix@example.com");
				contact.setPhone("+852 6845 5092");
				contact.setAddress("Hong Kong, China");
				
				Map<String, String> socialLinks = new HashMap<>();
				socialLinks.put("github", "https://github.com/felixsiu");
				socialLinks.put("linkedin", "https://linkedin.com/in/felixsiu");
				contact.setSocialLinks(socialLinks);
				
				Map<String, Boolean> publicMap = new HashMap<>();
				publicMap.put("email", true);
				publicMap.put("phone", false);
				publicMap.put("github", true);
				publicMap.put("linkedin", true);
				contact.setPublicMap(publicMap);
				
				contact.setCreateTime(LocalDateTime.now());
				contact.setUpdateTime(LocalDateTime.now());
				return contact;
		}
		
		private ContactDto createContactDto() {
				ContactDto dto = new ContactDto();
				dto.setDisplayName("Felix Siu");
				dto.setTitle("Full-Stack Developer");
				dto.setEmail("felix@example.com");
				dto.setPhone("+852 6845 5092");
				dto.setAddress("Hong Kong, China");
				
				Map<String, String> socialLinks = new HashMap<>();
				socialLinks.put("github", "https://github.com/felixsiu");
				socialLinks.put("linkedin", "https://linkedin.com/in/felixsiu");
				dto.setSocialLinks(socialLinks);
				
				Map<String, Boolean> publicMap = new HashMap<>();
				publicMap.put("email", true);
				publicMap.put("phone", false);
				dto.setPublicMap(publicMap);
				
				return dto;
		}
		
		// ============================================================
		// 1. getContact 測試
		// ============================================================
		
		@Test
		void getContact_shouldReturnFromCache_whenCacheExists() {
				// 準備：Redis 有快取
				ContactVo mockCachedVo = new ContactVo();
				mockCachedVo.setDisplayName("Felix Siu");
				mockCachedVo.setEmail("felix@example.com");
				
				String cacheKey = CacheKeys.contact(userId);
				
				when(myRedisTemplate.get(eq(cacheKey), any(TypeReference.class))).thenReturn(mockCachedVo);
				
				// 執行
				BaseVo<ContactVo> result = contactService.getContact(userId);
				
				// 驗證
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData().getDisplayName()).isEqualTo("Felix Siu");
				assertThat(result.getData().getEmail()).isEqualTo("felix@example.com");
				
				verify(myRedisTemplate, times(1)).get(eq(cacheKey), any(TypeReference.class));
				verify(contactMapper, never()).selectOne(any(LambdaQueryWrapper.class));
				verify(myRedisTemplate, never()).set(anyString(), any(), anyLong(), any());
		}
		
		@Test
		void getContact_shouldQueryDatabaseAndCache_whenCacheMissAndContactExists() {
				// 準備：Redis 回傳 null，資料庫有資料
				Contact mockContact = createContact(userId);
				String cacheKey = CacheKeys.contact(userId);
				
				when(myRedisTemplate.get(eq(cacheKey), any(TypeReference.class))).thenReturn(null);
				when(contactMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(mockContact);
				
				// 執行
				BaseVo<ContactVo> result = contactService.getContact(userId);
				
				// 驗證
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData().getDisplayName()).isEqualTo("Felix Siu");
				assertThat(result.getData().getEmail()).isEqualTo("felix@example.com");
				
				verify(contactMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
				verify(myRedisTemplate, times(1)).set(eq(cacheKey), any(ContactVo.class), anyLong(), any());
		}
		
		@Test
		void getContact_shouldReturnEmptyContact_whenContactNotFound() {
				// 準備：Redis 回傳 null，資料庫也沒有資料
				String cacheKey = CacheKeys.contact(userId);
				
				when(myRedisTemplate.get(eq(cacheKey), any(TypeReference.class))).thenReturn(null);
				when(contactMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
				
				// 執行
				BaseVo<ContactVo> result = contactService.getContact(userId);
				
				// 驗證
				assertThat(result.getIsSuccess()).isTrue();
				assertThat(result.getData()).isNotNull();
				assertThat(result.getData().getEmail()).isNull();  // 空的 Contact，所有欄位為 null
				
				verify(contactMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
				verify(myRedisTemplate, times(1)).set(eq(cacheKey), any(ContactVo.class), anyLong(), any());
		}
		
		// ============================================================
		// 2. updateContact 測試（新增/更新）
		// ============================================================
		
		@Test
		void updateContact_shouldInsert_whenContactNotExists() {
				// 準備：用戶沒有聯絡資訊
				ContactDto dto = createContactDto();
				
				when(contactMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
				when(contactMapper.insert(any(Contact.class))).thenReturn(1);
				
				// 執行
				BaseVo<Void> result = contactService.updateContact(userId, dto);
				
				// 驗證
				assertThat(result.getIsSuccess()).isTrue();
				
				// 驗證 insert 被呼叫
				ArgumentCaptor<Contact> captor = ArgumentCaptor.forClass(Contact.class);
				verify(contactMapper).insert(captor.capture());
				Contact captured = captor.getValue();
				assertThat(captured.getUserId()).isEqualTo(userId);
				assertThat(captured.getDisplayName()).isEqualTo("Felix Siu");
				assertThat(captured.getEmail()).isEqualTo("felix@example.com");
				
				// 驗證 updateById 沒有被呼叫
				verify(contactMapper, never()).updateById(any(Contact.class));
				
				// 驗證快取被清除
				verify(myRedisTemplate, times(1)).delete(CacheKeys.contact(userId));
		}
		
		@Test
		void updateContact_shouldUpdate_whenContactExists() {
				// 準備：用戶已有聯絡資訊
				Contact existingContact = createContact(userId);
				ContactDto dto = createContactDto();
				dto.setDisplayName("Updated Name");
				dto.setEmail("updated@example.com");
				
				when(contactMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingContact);
				when(contactMapper.updateById(any(Contact.class))).thenReturn(1);
				
				// 執行
				BaseVo<Void> result = contactService.updateContact(userId, dto);
				
				// 驗證
				assertThat(result.getIsSuccess()).isTrue();
				
				// 驗證 updateById 被呼叫，且 ID 被保留
				ArgumentCaptor<Contact> captor = ArgumentCaptor.forClass(Contact.class);
				verify(contactMapper).updateById(captor.capture());
				Contact captured = captor.getValue();
				assertThat(captured.getId()).isEqualTo(1);  // 保留原有的 ID
				assertThat(captured.getUserId()).isEqualTo(userId);
				assertThat(captured.getDisplayName()).isEqualTo("Updated Name");
				assertThat(captured.getEmail()).isEqualTo("updated@example.com");
				
				// 驗證 insert 沒有被呼叫
				verify(contactMapper, never()).insert(any(Contact.class));
				
				// 驗證快取被清除
				verify(myRedisTemplate, times(1)).delete(CacheKeys.contact(userId));
		}
		
		@Test
		void updateContact_shouldThrowException_whenDtoIsNull() {
				// 執行 & 驗證
				assertThatThrownBy(() -> contactService.updateContact(userId, null))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("參數缺失");
				
				verify(contactMapper, never()).selectOne(any(LambdaQueryWrapper.class));
				verify(contactMapper, never()).insert(any(Contact.class));
				verify(contactMapper, never()).updateById(any(Contact.class));
				verify(myRedisTemplate, never()).delete(anyString());
		}
		
		@Test
		void updateContact_shouldThrowException_whenInsertFails() {
				// 準備：插入失敗
				ContactDto dto = createContactDto();
				
				when(contactMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
				when(contactMapper.insert(any(Contact.class))).thenReturn(0);
				
				// 執行 & 驗證
				assertThatThrownBy(() -> contactService.updateContact(userId, dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("修改失敗");
				
				verify(contactMapper, times(1)).insert(any(Contact.class));
				verify(contactMapper, never()).updateById(any(Contact.class));
				verify(myRedisTemplate, never()).delete(anyString());
		}
		
		@Test
		void updateContact_shouldThrowException_whenUpdateFails() {
				// 準備：更新失敗
				Contact existingContact = createContact(userId);
				ContactDto dto = createContactDto();
				
				when(contactMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingContact);
				when(contactMapper.updateById(any(Contact.class))).thenReturn(0);
				
				// 執行 & 驗證
				assertThatThrownBy(() -> contactService.updateContact(userId, dto))
								.isInstanceOf(RuntimeException.class)
								.hasMessageContaining("修改失敗");
				
				verify(contactMapper, times(1)).updateById(any(Contact.class));
				verify(contactMapper, never()).insert(any(Contact.class));
				verify(myRedisTemplate, never()).delete(anyString());
		}
		
		// ============================================================
		// 3. convertToVo / convertToEntity 轉換測試
		// ============================================================
		
		@Test
		void convertToVo_shouldConvertAllFieldsCorrectly() {
				// 準備
				Contact contact = createContact(userId);
				
				// 執行（透過 getContact 間接測試）
				when(myRedisTemplate.get(anyString(), any(TypeReference.class))).thenReturn(null);
				when(contactMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(contact);
				
				BaseVo<ContactVo> result = contactService.getContact(userId);
				
				// 驗證
				ContactVo vo = result.getData();
				assertThat(vo.getDisplayName()).isEqualTo(contact.getDisplayName());
				assertThat(vo.getTitle()).isEqualTo(contact.getTitle());
				assertThat(vo.getEmail()).isEqualTo(contact.getEmail());
				assertThat(vo.getPhone()).isEqualTo(contact.getPhone());
				assertThat(vo.getAddress()).isEqualTo(contact.getAddress());
				assertThat(vo.getSocialLinks()).isEqualTo(contact.getSocialLinks());
				assertThat(vo.getPublicMap()).isEqualTo(contact.getPublicMap());
				assertThat(vo.getCreateTime()).isEqualTo(contact.getCreateTime());
				assertThat(vo.getUpdateTime()).isEqualTo(contact.getUpdateTime());
		}
		
		@Test
		void convertToEntity_shouldConvertAllFieldsCorrectly() {
				// 準備
				ContactDto dto = createContactDto();
				
				// 執行（透過 updateContact 間接測試）
				when(contactMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
				when(contactMapper.insert(any(Contact.class))).thenReturn(1);
				
				contactService.updateContact(userId, dto);
				
				// 驗證
				ArgumentCaptor<Contact> captor = ArgumentCaptor.forClass(Contact.class);
				verify(contactMapper).insert(captor.capture());
				Contact entity = captor.getValue();
				
				assertThat(entity.getUserId()).isEqualTo(userId);
				assertThat(entity.getDisplayName()).isEqualTo(dto.getDisplayName());
				assertThat(entity.getTitle()).isEqualTo(dto.getTitle());
				assertThat(entity.getEmail()).isEqualTo(dto.getEmail());
				assertThat(entity.getPhone()).isEqualTo(dto.getPhone());
				assertThat(entity.getAddress()).isEqualTo(dto.getAddress());
				assertThat(entity.getSocialLinks()).isEqualTo(dto.getSocialLinks());
				assertThat(entity.getPublicMap()).isEqualTo(dto.getPublicMap());
		}
		
		// ============================================================
		// 4. 快取清除測試
		// ============================================================
		
		@Test
		void updateContact_shouldClearCache_whenInsertSuccess() {
				// 準備
				ContactDto dto = createContactDto();
				
				when(contactMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
				when(contactMapper.insert(any(Contact.class))).thenReturn(1);
				
				// 執行
				contactService.updateContact(userId, dto);
				
				// 驗證：快取被清除
				verify(myRedisTemplate, times(1)).delete(CacheKeys.contact(userId));
		}
		
		@Test
		void updateContact_shouldClearCache_whenUpdateSuccess() {
				// 準備
				Contact existingContact = createContact(userId);
				ContactDto dto = createContactDto();
				
				when(contactMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingContact);
				when(contactMapper.updateById(any(Contact.class))).thenReturn(1);
				
				// 執行
				contactService.updateContact(userId, dto);
				
				// 驗證：快取被清除
				verify(myRedisTemplate, times(1)).delete(CacheKeys.contact(userId));
		}
}