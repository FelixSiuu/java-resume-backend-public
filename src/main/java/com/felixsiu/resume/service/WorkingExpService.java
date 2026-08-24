package com.felixsiu.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.felixsiu.resume.model.dto.KeyRespDto;
import com.felixsiu.resume.model.dto.WorkingExpDto;
import com.felixsiu.resume.model.entity.KeyResponsibilities;
import com.felixsiu.resume.model.entity.WorkingExperience;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.model.vo.KeyResponsibilitiesVo;
import com.felixsiu.resume.model.vo.WorkingExperienceVo;
import com.felixsiu.resume.respository.KeyResponsibilitiesMapper;
import com.felixsiu.resume.respository.WorkingExperienceMapper;
import com.felixsiu.resume.utils.CacheKeys;
import com.felixsiu.resume.utils.MyRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class WorkingExpService {
		@Autowired
		private WorkingExperienceMapper workingExperienceMapper;
		@Autowired
		private KeyResponsibilitiesMapper keyResponsibilitiesMapper;
		
		@Autowired
		private MyRedisTemplate myRedisTemplate;  // ✅ 使用自定义模板
		
		private static final long CACHE_TTL = 10;
		private static final TimeUnit CACHE_TTL_UNIT = TimeUnit.MINUTES;
		
		/**
		 * 批量插入 keyResponsibilities
		 */
		private void batchInsertResponsibilities(List<KeyResponsibilities> list) {
				if (list == null || list.isEmpty()) {
						return;
				}
				for (KeyResponsibilities item : list) {
						keyResponsibilitiesMapper.insert(item);
				}
		}
		
		/**
		 * 獲取工作經歷
		 * 主表：WorkingExperience
		 * 副表：KeyResponsibilities
		 */
		public BaseVo<List<WorkingExperienceVo>> getWorkingExperiences(String userId) {
				String cacheKey = CacheKeys.experience(userId);
				
				// 先從 Redis 查
				List<WorkingExperienceVo> cached = myRedisTemplate.get(cacheKey, new TypeReference<List<WorkingExperienceVo>>() {
				});
				if (cached != null) {
						System.out.println("✅ 命中缓存！");
						return BaseVo.success(cached);
				}
				
				// 2. 缓存未命中，查 MySQL
				System.out.println("❌ 缓存未命中，查询数据库...");
				
				// 最新的工作經驗排在前頭，根據id進行倒序獲取
				LambdaQueryWrapper<WorkingExperience> weLambdaQueryWrapper = new LambdaQueryWrapper<>();
				LambdaQueryWrapper<KeyResponsibilities> krLambdaQueryWrapper = new LambdaQueryWrapper<>();
				weLambdaQueryWrapper
								.eq(WorkingExperience::getUserId, userId)
								.orderByDesc(WorkingExperience::getId);
				krLambdaQueryWrapper.eq(KeyResponsibilities::getUserId, userId);
				
				// 各自從表格獲取到基礎list數據
				List<WorkingExperience> workingExperienceList = workingExperienceMapper.selectList(weLambdaQueryWrapper);
				List<KeyResponsibilities> keyResponsibilitiesList = keyResponsibilitiesMapper.selectList(krLambdaQueryWrapper);
				
				/*
				 * 根據working_id進行分類，返回
				 * {working_id: List<KeyResponsibilities>}
				 * 類型的數據
				 */
				Map<Integer, List<KeyResponsibilities>> krMap = keyResponsibilitiesList.stream().collect(Collectors.groupingBy(KeyResponsibilities::getWorkingId));
				
				// 遍歷working_experience表的數據，然後進行vo拼接
				List<WorkingExperienceVo> workingExperienceVoList = workingExperienceList.stream().map(owe -> {
						WorkingExperienceVo cweVo = new WorkingExperienceVo();
						
						/*
						 * 欄位不多，可以使用BeanUtils
						 * 🏆 優化點一：消除 6 行手動 set
						 * 自動將 owe 裡的 company, location, startDate, endDate, jobTitle, createTime
						 * 複製到 cweVo 中。因為名稱和型態完全一致，效率極高。
						 */
						org.springframework.beans.BeanUtils.copyProperties(owe, cweVo);
						
						// map.getOrDefault()，類似js的 || 賦予默認值，避免null
						List<KeyResponsibilities> keyResponsibilities = krMap.getOrDefault(owe.getId(), new ArrayList<>());
						
						cweVo.setKeyResponsibilities(keyResponsibilities.stream().map(k -> {
								KeyResponsibilitiesVo krv = new KeyResponsibilitiesVo();
								org.springframework.beans.BeanUtils.copyProperties(k, krv);
								return krv;
						}).toList());
						
						return cweVo;
						
				}).toList();
				
				// 3. 寫入緩存
				myRedisTemplate.set(cacheKey, workingExperienceVoList, CACHE_TTL, CACHE_TTL_UNIT);
				
				return BaseVo.success(workingExperienceVoList);
		}
		
		/**
		 * 刪除工作經歷
		 */
		@Transactional(rollbackFor = Exception.class)
		public BaseVo<Void> deleteWorkingExperiences(Integer id, String userId) throws RuntimeException {
				LambdaQueryWrapper<WorkingExperience> wElambdaQueryWrapper = new LambdaQueryWrapper<>();
				wElambdaQueryWrapper
								.eq(WorkingExperience::getId, id)
								.eq(WorkingExperience::getUserId, userId);
				WorkingExperience wE = workingExperienceMapper.selectOne(wElambdaQueryWrapper);
				
				if (wE == null) {
						throw new RuntimeException("刪除失敗，資料不存在");
				}
				
				Integer workingId = wE.getId();
				
				// 刪除副表的數據
				LambdaQueryWrapper<KeyResponsibilities> kRLambdaQueryWrapper = new LambdaQueryWrapper<>();
				kRLambdaQueryWrapper
								.eq(KeyResponsibilities::getWorkingId, workingId)
								.eq(KeyResponsibilities::getUserId, userId);
				keyResponsibilitiesMapper.delete(kRLambdaQueryWrapper);
				
				// 刪除主表的數據
				int deleteWERes = workingExperienceMapper.delete(wElambdaQueryWrapper);
				if (deleteWERes == 0) {
						throw new RuntimeException("刪除失敗，資料不存在");
				}
				
				myRedisTemplate.delete(CacheKeys.experience(userId));
				
				return BaseVo.success(null);
		}
		
		/**
		 * 編輯工作經歷
		 */
		@Transactional(rollbackFor = Exception.class)
		public BaseVo<Void> editWorkingExperiences(Integer id, WorkingExpDto workingExpDto, String userId) throws RuntimeException {
				
				if (isPostBodyNotOk(workingExpDto)) {
						throw new RuntimeException("參數缺失");
				}
				
				List<KeyRespDto> kRListDto = workingExpDto.keyResponsibilities();
				
				// 先刪除副表的數據
				LambdaQueryWrapper<KeyResponsibilities> kRLambdaQueryWrapper = new LambdaQueryWrapper<>();
				kRLambdaQueryWrapper
								.eq(KeyResponsibilities::getWorkingId, id)
								.eq(KeyResponsibilities::getUserId, userId);
				keyResponsibilitiesMapper.delete(kRLambdaQueryWrapper);
				
				/*
				 * 再根據新的副表數據完整插入
				 * 如果副表有數據，再插入到副表
				 */
				if (!kRListDto.isEmpty() && kRListDto != null) {
						List<KeyResponsibilities> kRList = kRListDto.stream().map(kRDto -> {
								KeyResponsibilities kR = new KeyResponsibilities();
								kR.setWorkingId(id);
								kR.setTitle(kRDto.title());
								kR.setResponsibility(kRDto.responsibility());
								kR.setUserId(userId);
								return kR;
						}).toList();
						
						batchInsertResponsibilities(kRList);
				}
				
				// 修改主表的數據
				WorkingExperience wE = new WorkingExperience();
				wE.setId(id);
				wE.setStartDate(workingExpDto.startDate());
				wE.setEndDate(workingExpDto.endDate());
				wE.setJobTitle(workingExpDto.jobTitle());
				wE.setCompany(workingExpDto.company());
				wE.setLocation(workingExpDto.location());
				wE.setUserId(userId);
				int updateWERes = workingExperienceMapper.updateById(wE);
				if (updateWERes == 0) {
						throw new RuntimeException("修改失敗");
				}
				
				myRedisTemplate.delete(CacheKeys.experience(userId));
				
				return BaseVo.success(null);
		}
		
		/**
		 * 添加工作經歷
		 */
		@Transactional(rollbackFor = Exception.class)
		public BaseVo<Void> addWorkingExperiences(WorkingExpDto workingExpDto, String userId) throws RuntimeException {
				
				if (isPostBodyNotOk(workingExpDto)) {
						throw new RuntimeException("參數缺失");
				}
				
				// 添加主表的數據
				WorkingExperience wE = new WorkingExperience();
				wE.setStartDate(workingExpDto.startDate());
				wE.setEndDate(workingExpDto.endDate());
				wE.setJobTitle(workingExpDto.jobTitle());
				wE.setCompany(workingExpDto.company());
				wE.setLocation(workingExpDto.location());
				wE.setUserId(userId);
				int updateWERes = workingExperienceMapper.insert(wE);
				if (updateWERes == 0) {
						throw new RuntimeException("添加失敗");
				}
				
				List<KeyRespDto> kRListDto = workingExpDto.keyResponsibilities();
				if (kRListDto != null && !kRListDto.isEmpty()) {
						Integer newWorkingId = wE.getId();
						List<KeyResponsibilities> kRList = kRListDto.stream().map(kRDto -> {
								KeyResponsibilities kR = new KeyResponsibilities();
								kR.setWorkingId(newWorkingId);
								kR.setTitle(kRDto.title());
								kR.setResponsibility(kRDto.responsibility());
								kR.setUserId(userId);
								return kR;
						}).toList();
						
						batchInsertResponsibilities(kRList);
				}
				
				myRedisTemplate.delete(CacheKeys.experience(userId));
				
				return BaseVo.success(null);
		}
		
		boolean isPostBodyNotOk(WorkingExpDto dto) {
				if (dto == null ||
								!StringUtils.hasText(dto.startDate()) ||
								!StringUtils.hasText(dto.jobTitle()) ||
								!StringUtils.hasText(dto.company()) ||
								!StringUtils.hasText(dto.location())) {
						return true;
				}
				
				List<KeyRespDto> respDtos = dto.keyResponsibilities();
				if (respDtos != null) {
						for (KeyRespDto keyRespDto : respDtos) {
								if (!StringUtils.hasText(keyRespDto.responsibility())) {
										return true;
								}
								
						}
				}
				
				return false;
		}
		
}
