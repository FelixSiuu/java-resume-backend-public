package com.felixsiu.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.felixsiu.resume.model.dto.EduDto;
import com.felixsiu.resume.model.entity.Education;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.respository.EducationMapper;
import com.felixsiu.resume.utils.CacheKeys;
import com.felixsiu.resume.utils.MyRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class EducationService {
		@Autowired
		EducationMapper educationMapper;
		
		@Autowired
		private MyRedisTemplate myRedisTemplate;  // ✅ 使用自定义模板
		
		private static final long CACHE_TTL = 10;
		private static final TimeUnit CACHE_TTL_UNIT = TimeUnit.MINUTES;
		
		/**
		 * 獲取我的教育
		 */
		public BaseVo<List<Education>> getEducation(String userId) {
				String cacheKey = CacheKeys.education(userId);
				
				// 先從 Redis 查
				List<Education> cached = myRedisTemplate.get(cacheKey, new TypeReference<List<Education>>() {
				});
				if (cached != null) {
						System.out.println("✅ 命中缓存！");
						return BaseVo.success(cached);
				}
				
				// 2. 缓存未命中，查 MySQL
				System.out.println("❌ 缓存未命中，查询数据库...");
				
				LambdaQueryWrapper<Education> eLambdaQueryWrapper = new LambdaQueryWrapper<>();
				eLambdaQueryWrapper.eq(Education::getUserId, userId).orderByDesc(Education::getId);
				
				List<Education> educationList = educationMapper.selectList(eLambdaQueryWrapper);
				
				// 3. 寫入緩存
				myRedisTemplate.set(cacheKey, educationList, CACHE_TTL, CACHE_TTL_UNIT);
				
				return BaseVo.success(educationList);
		}
		
		/**
		 * 修改我的教育
		 */
		public BaseVo<Void> editEducation(Integer id, EduDto eduDto, String userId) throws RuntimeException {
				
				if (isPostBodyNotOk(eduDto)) {
						throw new RuntimeException("參數缺失");
				}
				
				Education education = new Education();
				education.setId(id);
				education.setStartDate(eduDto.startDate());
				education.setEndDate(eduDto.endDate());
				education.setInstitution(eduDto.institution());
				education.setMajor(eduDto.major());
				education.setLocation(eduDto.location());
				education.setDegree(eduDto.degree());
				education.setUserId(userId);
				
				int res = educationMapper.updateById(education);
				
				if (res == 0) {
						throw new RuntimeException("修改失敗，資料不存在或內容未變更");
				}
				
				myRedisTemplate.delete(CacheKeys.education(userId));
				
				return BaseVo.success(null);
		}
		
		/**
		 * 添加我的教育
		 */
		public BaseVo<Void> addEducation(EduDto eduDto, String userId) throws RuntimeException {
				
				if (isPostBodyNotOk(eduDto)) {
						throw new RuntimeException("參數缺失");
				}
				
				Education education = new Education();
				education.setStartDate(eduDto.startDate());
				education.setEndDate(eduDto.endDate());
				education.setInstitution(eduDto.institution());
				education.setMajor(eduDto.major());
				education.setLocation(eduDto.location());
				education.setDegree(eduDto.degree());
				education.setUserId(userId);
				
				int res = educationMapper.insert(education);
				
				if (res == 0) {
						throw new RuntimeException("添加失敗");
				}
				
				myRedisTemplate.delete(CacheKeys.education(userId));
				
				return BaseVo.success(null);
		}
		
		/**
		 * 刪除我的教育
		 */
		public BaseVo<Void> deleteEducation(Integer id, String userId) throws RuntimeException {
				LambdaQueryWrapper<Education> lambdaQueryWrapper = new LambdaQueryWrapper<>();
				lambdaQueryWrapper.eq(Education::getId, id).eq(Education::getUserId, userId);
				
				int res = educationMapper.delete(lambdaQueryWrapper);
				
				if (res == 0) {
						throw new RuntimeException("刪除失敗，資料不存在");
				}
				
				myRedisTemplate.delete(CacheKeys.education(userId));
				
				return BaseVo.success(null);
		}
		
		boolean isPostBodyNotOk(EduDto eduDto) {
				return eduDto == null ||
								!StringUtils.hasText(eduDto.startDate()) ||
								!StringUtils.hasText(eduDto.endDate()) ||
								!StringUtils.hasText(eduDto.institution()) ||
								!StringUtils.hasText(eduDto.major()) ||
								!StringUtils.hasText(eduDto.location());
		}
}
