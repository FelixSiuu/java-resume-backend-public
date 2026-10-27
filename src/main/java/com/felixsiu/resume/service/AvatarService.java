package com.felixsiu.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.felixsiu.resume.model.entity.Avatar;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.respository.AvatarMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@Service
public class AvatarService {
		
		@Autowired
		private AvatarMapper avatarMapper;
		
		private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png");
		private static final long MAX_SIZE = 2 * 1024 * 1024; // 2MB
		
		/**
		 * 獲取頭像
		 */
		public Optional<Avatar> getAvatar(String userId) {
				LambdaQueryWrapper<Avatar> lambdaQueryWrapper = new LambdaQueryWrapper<>();
				lambdaQueryWrapper.eq(Avatar::getUserId, userId);
				
				Avatar avatar = avatarMapper.selectOne(lambdaQueryWrapper);
				
				return Optional.ofNullable(avatar);
		}
		
		/**
		 * 上傳頭像
		 */
		public BaseVo<Void> upload(String userId, MultipartFile file) throws IOException {
				if (file.getSize() > MAX_SIZE) {
						throw new RuntimeException("圖片不能超過 2MB");
				}
				
				String contentType = file.getContentType();
				if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
						throw new RuntimeException("僅支持 JPG/PNG 格式");
				}
				
				Avatar avatar = new Avatar();
				avatar.setUserId(userId);
				avatar.setContentType(contentType);
				avatar.setFileSize((int) file.getSize());
				avatar.setImageData(file.getBytes());
				
				Optional<Avatar> existing = getAvatar(userId);
				if (existing.isPresent()) {
						// 重複上傳(覆蓋)
						avatar.setId(existing.get().getId());
						int res = avatarMapper.updateById(avatar);
						if (res == 0) {
								throw new RuntimeException("修改失敗");
						}
				} else {
						// 新上傳
						int res = avatarMapper.insert(avatar);
						if (res == 0) {
								throw new RuntimeException("添加失敗");
						}
				}
				
				return BaseVo.success(null);
		}
		
		/**
		 * 刪除頭像
		 */
		public BaseVo<Void> delete(String userId) throws RuntimeException {
				
				LambdaQueryWrapper<Avatar> lambdaQueryWrapper = new LambdaQueryWrapper<>();
				lambdaQueryWrapper.eq(Avatar::getUserId, userId);
				
				int res = avatarMapper.delete(lambdaQueryWrapper);
				
				if (res == 0) {
						throw new RuntimeException("刪除失敗，資料不存在");
				}
				
				return BaseVo.success(null);
		}
}
