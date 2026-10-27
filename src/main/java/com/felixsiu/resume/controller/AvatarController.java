package com.felixsiu.resume.controller;

import com.felixsiu.resume.annotation.StandardApiResponses;
import com.felixsiu.resume.model.entity.Avatar;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.service.AvatarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

/**
 * PDF 頭像
 */
@RestController
@RequestMapping("/api/v1/myInfo/avatar")
@Tag(name = "Avatar", description = "頭像相關接口")
public class AvatarController {
		
		@Autowired
		private AvatarService avatarService;
		
		@GetMapping
		@Operation(
						summary = "獲取頭像",
						description = "獲取當前登入用戶的頭像"
		)
		@StandardApiResponses
		public ResponseEntity<byte[]> getAvatar(
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId
		) {
				Optional<Avatar> avatarOpt = avatarService.getAvatar(userId);
				
				if (avatarOpt.isPresent()) {
						return ResponseEntity.ok()
										.contentType(MediaType.parseMediaType(avatarOpt.get().getContentType()))
										.header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
										.body(avatarOpt.get().getImageData());
				}
				
				return ResponseEntity.notFound().build();
		}
		
		// 只接受 Content-Type: multipart/form-data 的 request
		@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
		@Operation(
						summary = "上傳頭像",
						description = "上傳當前登入用戶的頭像"
		)
		@StandardApiResponses
		public BaseVo<Void> upload(
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId,
						@RequestParam("avatar") MultipartFile file
		) throws IOException {
				return avatarService.upload(userId, file);
		}
		
		@DeleteMapping
		@Operation(
						summary = "刪除頭像",
						description = "刪除當前登入用戶的頭像"
		)
		@StandardApiResponses
		public BaseVo<Void> delete(
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId
		) {
				return avatarService.delete(userId);
		}
}
