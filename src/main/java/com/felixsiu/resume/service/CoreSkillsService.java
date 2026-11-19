package com.felixsiu.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.felixsiu.resume.exception.BusinessException;
import com.felixsiu.resume.model.ErrorMap;
import com.felixsiu.resume.model.dto.SkillDto;
import com.felixsiu.resume.model.entity.CoreSkills;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.model.vo.CoreSkillVo;
import com.felixsiu.resume.repository.CoreSkillsMapper;
import com.felixsiu.resume.utils.CacheKeys;
import com.felixsiu.resume.utils.MyRedisTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CoreSkillsService {
		@Autowired
		CoreSkillsMapper coreSkillsMapper;
		
		@Autowired
		private MyRedisTemplate myRedisTemplate;  // ✅ 使用自定义模板
		
		private static final long CACHE_TTL = 10;
		private static final TimeUnit CACHE_TTL_UNIT = TimeUnit.MINUTES;
		
		/**
		 * 獲取技能
		 */
		public BaseVo<List<CoreSkillVo>> getCoreSkills(String userId) {
				String cacheKey = CacheKeys.skills(userId);
				
				// 先從 Redis 查
				List<CoreSkillVo> cached = myRedisTemplate.get(cacheKey, new TypeReference<List<CoreSkillVo>>() {
				});
				if (cached != null) {
						log.debug("cache hit");
						return BaseVo.success(cached);
				}
				
				// 2. 缓存未命中，查 MySQL
				log.debug("cache miss, querying DB");
				
				LambdaQueryWrapper<CoreSkills> lambdaQueryWrapper = new LambdaQueryWrapper<>();
				lambdaQueryWrapper.eq(CoreSkills::getUserId, userId);
				List<CoreSkills> coreSkillsList = coreSkillsMapper.selectList(lambdaQueryWrapper);
				
				List<CoreSkillVo> coreSkillVoList = coreSkillsList.stream().map(e -> {
						CoreSkillVo vo = new CoreSkillVo();
						vo.setId(e.getId());
						vo.setSummary(e.getSummary());
						vo.setDetails(e.getDetails());
						vo.setCreateTime(e.getCreateTime());
						vo.setUpdateTime(e.getUpdateTime());
						return vo;
				}).toList();
				
				// 3. 寫入緩存
				myRedisTemplate.set(cacheKey, coreSkillVoList, CACHE_TTL, CACHE_TTL_UNIT);
				
				return BaseVo.success(coreSkillVoList);
		}
		
		/**
		 * 編輯某個技能
		 */
		public BaseVo<Void> editSkill(Integer id, SkillDto skillDto, String userId) throws RuntimeException {
				
				if (skillDto == null || !StringUtils.hasText(skillDto.summary()) || !StringUtils.hasText(skillDto.details())) {
						throw new BusinessException(ErrorMap.MISSING_PARAM);
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
						throw new BusinessException(ErrorMap.CONTENT_UNCHANGED);
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
						throw new BusinessException(ErrorMap.RESOURCE_NOT_FOUND);
				}
				
				myRedisTemplate.delete(CacheKeys.skills(userId));
				
				return BaseVo.success(null);
		}
		
		/**
		 * 添加技能
		 */
		public BaseVo<Void> addSkill(SkillDto skillDto, String userId) throws RuntimeException {
				
				if (skillDto == null || !StringUtils.hasText(skillDto.summary()) || !StringUtils.hasText(skillDto.details())) {
						throw new BusinessException(ErrorMap.MISSING_PARAM);
				}
				
				String summary = skillDto.summary();
				String details = skillDto.details();
				CoreSkills coreSkills = new CoreSkills();
				coreSkills.setUserId(userId);
				coreSkills.setSummary(summary);
				coreSkills.setDetails(details);
				
				int res = coreSkillsMapper.insert(coreSkills);
				
				if (res == 0) {
						throw new BusinessException(ErrorMap.CREATE_FAILED);
				}
				
				myRedisTemplate.delete(CacheKeys.skills(userId));
				
				return BaseVo.success(null);
		}
}
