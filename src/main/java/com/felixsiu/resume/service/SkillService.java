package com.felixsiu.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.felixsiu.resume.model.dto.SkillDto;
import com.felixsiu.resume.model.entity.CoreSkills;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.respository.CoreSkillsMapper;
import com.felixsiu.resume.utils.CacheKeys;
import com.felixsiu.resume.utils.MyRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SkillService {
		@Autowired
		CoreSkillsMapper coreSkillsMapper;
		
		@Autowired
		private MyRedisTemplate myRedisTemplate;  // ✅ 使用自定义模板
		
		private static final long CACHE_TTL = 10;
		private static final TimeUnit CACHE_TTL_UNIT = TimeUnit.MINUTES;
		
		/**
		 * 獲取技能
		 */
		public BaseVo<List<CoreSkills>> getCoreSkills(String userId) {
				String cacheKey = CacheKeys.skills(userId);
				
				// 先從 Redis 查
				List<CoreSkills> cached = myRedisTemplate.get(cacheKey, new TypeReference<List<CoreSkills>>() {
				});
				if (cached != null) {
						System.out.println("✅ 命中缓存！");
						return BaseVo.success(cached);
				}
				
				// 2. 缓存未命中，查 MySQL
				System.out.println("❌ 缓存未命中，查询数据库...");
				
				LambdaQueryWrapper<CoreSkills> lambdaQueryWrapper = new LambdaQueryWrapper<>();
				lambdaQueryWrapper.eq(CoreSkills::getUserId, userId);
				List<CoreSkills> coreSkillsList = coreSkillsMapper.selectList(lambdaQueryWrapper);
				
				// 3. 寫入緩存
				myRedisTemplate.set(cacheKey, coreSkillsList, CACHE_TTL, CACHE_TTL_UNIT);
				
				return BaseVo.success(coreSkillsList);
		}
		
		/**
		 * 編輯某個技能
		 */
		public BaseVo<Void> editSkill(Integer id, SkillDto skillDto, String userId) throws RuntimeException {
				
				if (skillDto == null || !StringUtils.hasText(skillDto.summary()) || !StringUtils.hasText(skillDto.details())) {
						throw new RuntimeException("參數缺失");
				}
				
				String summary = skillDto.summary();
				String details = skillDto.details();
				
				CoreSkills coreSkills = new CoreSkills();
				coreSkills.setId(id);
				coreSkills.setUserId(userId);
				coreSkills.setSummary(summary);
				coreSkills.setDetails(details);
				
				int res = coreSkillsMapper.updateById(coreSkills);
				
				if (res == 0) {
						throw new RuntimeException("修改失敗，資料不存在或內容未變更");
				}
				
				myRedisTemplate.delete(CacheKeys.skills(userId));
				
				return BaseVo.success(null);
		}
		
		/**
		 * 刪除某個技能
		 */
		public BaseVo<Void> deleteSkill(Integer id, String userId) throws RuntimeException {
				
				LambdaQueryWrapper<CoreSkills> lambdaQueryWrapper = new LambdaQueryWrapper<>();
				lambdaQueryWrapper.eq(CoreSkills::getUserId, userId).eq(CoreSkills::getId, id);
				
				int res = coreSkillsMapper.delete(lambdaQueryWrapper);
				
				if (res == 0) {
						throw new RuntimeException("刪除失敗，資料不存在");
				}
				
				myRedisTemplate.delete(CacheKeys.skills(userId));
				
				return BaseVo.success(null);
		}
		
		/**
		 * 添加技能
		 */
		public BaseVo<Void> addSkill(SkillDto skillDto, String userId) throws RuntimeException {
				
				if (skillDto == null || !StringUtils.hasText(skillDto.summary()) || !StringUtils.hasText(skillDto.details())) {
						throw new RuntimeException("參數缺失");
				}
				
				String summary = skillDto.summary();
				String details = skillDto.details();
				CoreSkills coreSkills = new CoreSkills();
				coreSkills.setUserId(userId);
				coreSkills.setSummary(summary);
				coreSkills.setDetails(details);
				
				int res = coreSkillsMapper.insert(coreSkills);
				
				if (res == 0) {
						throw new RuntimeException("添加失敗");
				}
				
				myRedisTemplate.delete(CacheKeys.skills(userId));
				
				return BaseVo.success(null);
		}
}
