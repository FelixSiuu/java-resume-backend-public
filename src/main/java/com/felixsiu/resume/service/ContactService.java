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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class ContactService {
		
		@Autowired
		ContactMapper contactMapper;
		
		@Autowired
		private MyRedisTemplate myRedisTemplate;  // ✅ 使用自定义模板
		
		private static final long CACHE_TTL = 10;
		private static final TimeUnit CACHE_TTL_UNIT = TimeUnit.MINUTES;
		
		/**
		 * 查詢聯繫方式
		 */
		public BaseVo<ContactVo> getContact(String userId) {
				
				// 1. 先從 Redis 查
				String cacheKey = CacheKeys.contact(userId);
				ContactVo cached = myRedisTemplate.get(cacheKey, new TypeReference<ContactVo>() {
				});
				if (cached != null) {
						System.out.println("✅ 命中缓存！");
						return BaseVo.success(cached);
				}
				
				// 2. 缓存未命中，查 MySQL
				System.out.println("❌ 缓存未命中，查询数据库...");
				
				LambdaQueryWrapper<Contact> lambdaQueryWrapper = new LambdaQueryWrapper<>();
				lambdaQueryWrapper.eq(Contact::getUserId, userId);
				Contact contact = contactMapper.selectOne(lambdaQueryWrapper);
				
				if (contact == null) {
						contact = new Contact();
				}
				
				ContactVo contactVo = convertToVo(contact);
				
				// 3. 寫入緩存
				myRedisTemplate.set(cacheKey, contactVo, CACHE_TTL, CACHE_TTL_UNIT);
				
				return BaseVo.success(convertToVo(contact));
		}
		
		/**
		 * 更新聯繫方式
		 */
		public BaseVo<Void> updateContact(String userId, ContactDto contactDto) throws RuntimeException {
				
				if (contactDto == null) {
						throw new RuntimeException("參數缺失");
				}
				
				LambdaQueryWrapper<Contact> lambdaQueryWrapper = new LambdaQueryWrapper<>();
				lambdaQueryWrapper.eq(Contact::getUserId, userId);
				Contact existing = contactMapper.selectOne(lambdaQueryWrapper);
				
				Contact contact = convertToEntity(contactDto);
				contact.setUserId(userId);
				
				if (existing == null) {
						int res = contactMapper.insert(contact);
						
						if (res == 0) {
								throw new RuntimeException("修改失敗，資料不存在或內容未變更");
						}
				} else {
						contact.setId(existing.getId());
						int res = contactMapper.updateById(contact);
						
						if (res == 0) {
								throw new RuntimeException("修改失敗，資料不存在或內容未變更");
						}
				}
				
				// ✅ 清除缓存
				myRedisTemplate.delete(CacheKeys.contact(userId));
				
				return BaseVo.success(null);
		}
		
		private ContactVo convertToVo(Contact entity) {
				ContactVo vo = new ContactVo();
				vo.setEmail(entity.getEmail());
				vo.setPhone(entity.getPhone());
				vo.setDisplayName(entity.getDisplayName());
				vo.setAddress(entity.getAddress());
				vo.setTitle(entity.getTitle());
				vo.setPublicMap(entity.getPublicMap());
				vo.setSocialLinks(entity.getSocialLinks());
				vo.setCreateTime(entity.getCreateTime());
				vo.setUpdateTime(entity.getUpdateTime());
				return vo;
		}
		
		private Contact convertToEntity(ContactDto dto) {
				Contact contact = new Contact();
				contact.setDisplayName(dto.getDisplayName());
				contact.setTitle(dto.getTitle());
				contact.setEmail(dto.getEmail());
				contact.setPhone(dto.getPhone());
				contact.setSocialLinks(dto.getSocialLinks());
				contact.setPublicMap(dto.getPublicMap());
				contact.setAddress(dto.getAddress());
				return contact;
		}
		
}
