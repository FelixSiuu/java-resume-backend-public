package com.felixsiu.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.felixsiu.resume.model.dto.AboutDto;
import com.felixsiu.resume.model.entity.About;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.respository.AboutMapper;
import com.felixsiu.resume.utils.CacheKeys;
import com.felixsiu.resume.utils.MyRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class AboutService {
		@Autowired
		AboutMapper aboutMapper;
		
		@Autowired
		private MyRedisTemplate myRedisTemplate;  // ✅ 使用自定义模板
		
		private static final long CACHE_TTL = 10;
		private static final TimeUnit CACHE_TTL_UNIT = TimeUnit.MINUTES;
		
		/**
		 * 獲取關於我
		 */
		public BaseVo<List<About>> getAboutMe(String userId) {
				String cacheKey = CacheKeys.about(userId);
				
				// 先從 Redis 查
				List<About> cached = myRedisTemplate.get(cacheKey, new TypeReference<List<About>>() {
				});
				if (cached != null) {
						System.out.println("✅ 命中缓存！");
						return BaseVo.success(cached);
				}
				
				// 2. 缓存未命中，查 MySQL
				System.out.println("❌ 缓存未命中，查询数据库...");
				
				LambdaQueryWrapper<About> lambdaQueryWrapper = new LambdaQueryWrapper<>();
				lambdaQueryWrapper.eq(About::getUserId, userId);
				List<About> aboutMeList = aboutMapper.selectList(lambdaQueryWrapper);
				
				// 3. 寫入緩存
				myRedisTemplate.set(cacheKey, aboutMeList, CACHE_TTL, CACHE_TTL_UNIT);
				
				return BaseVo.success(aboutMeList);
		}
		
		/**
		 * 編輯某一段落的關於我
		 */
		public BaseVo<Void> editAbout(Integer id, AboutDto aboutDto, String userId) throws RuntimeException {
				
				if (aboutDto == null || !StringUtils.hasText(aboutDto.paragraph())) {
						throw new RuntimeException("參數缺失");
				}
				
				String paragraph = aboutDto.paragraph();
				
				About about = new About();
				about.setId(id);
				about.setUserId(userId);
				about.setParagraph(paragraph);
				
				int res = aboutMapper.updateById(about);
				
				if (res == 0) {
						throw new RuntimeException("修改失敗，資料不存在或內容未變更");
				}
				
				// ✅ 清除缓存
				myRedisTemplate.delete(CacheKeys.about(userId));
				
				return BaseVo.success(null);
		}
		
		/**
		 * 刪除關於我
		 */
		public BaseVo<Void> deleteAbout(Integer id, String userId) throws RuntimeException {
				
				LambdaQueryWrapper<About> lambdaQueryWrapper = new LambdaQueryWrapper<>();
				lambdaQueryWrapper.eq(About::getUserId, userId).eq(About::getId, id);
				
				int res = aboutMapper.delete(lambdaQueryWrapper);
				
				if (res == 0) {
						throw new RuntimeException("刪除失敗，資料不存在");
				}
				
				// ✅ 清除缓存
				myRedisTemplate.delete(CacheKeys.about(userId));
				
				return BaseVo.success(null);
		}
		
		/**
		 * 添加關於我
		 */
		public BaseVo<Void> addAbout(AboutDto aboutDto, String userId) throws RuntimeException {
				
				if (aboutDto == null || !StringUtils.hasText(aboutDto.paragraph())) {
						throw new RuntimeException("參數缺失");
				}
				
				About about = new About();
				about.setParagraph(aboutDto.paragraph());
				about.setUserId(userId);
				
				int res = aboutMapper.insert(about);
				
				if (res == 0) {
						throw new RuntimeException("添加失敗");
				}
				
				// ✅ 清除缓存
				myRedisTemplate.delete(CacheKeys.about(userId));
				
				return BaseVo.success(null);
		}
}
