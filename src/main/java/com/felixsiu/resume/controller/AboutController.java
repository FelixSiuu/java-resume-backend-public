package com.felixsiu.resume.controller;

import com.felixsiu.resume.annotation.StandardApiResponses;
import com.felixsiu.resume.model.dto.AboutDto;
import com.felixsiu.resume.model.entity.About;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.service.AboutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 關於我
 */
@RestController
@RequestMapping("/api/v1/myInfo/about")
@Tag(name = "About", description = "關於我相關接口")
public class AboutController {
		
		@Autowired
		AboutService aboutService;
		
		@GetMapping
		@Operation(
						summary = "獲取關於我",
						description = "獲取當前登入用戶的關於我資訊列表"
		)
		@StandardApiResponses
		public BaseVo<List<About>> about(
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId
		) {
				return aboutService.getAboutMe(userId);
		}
		
		@PutMapping("/{id}")
		@Operation(
						summary = "編輯關於我段落",
						description = "修改指定 ID 的關於我段落內容"
		)
		@StandardApiResponses
		public BaseVo<Void> about(
						@Parameter(description = "段落 ID")
						@PathVariable Integer id,
						@Valid @RequestBody AboutDto aboutDto,
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId
		) {
				return aboutService.editAbout(id, aboutDto, userId);
		}
		
		@DeleteMapping("/{id}")
		@Operation(summary = "刪除關於我段落")
		@StandardApiResponses
		public BaseVo<Void> about(
						@Parameter(description = "段落 ID")
						@PathVariable Integer id,
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId
		) {
				return aboutService.deleteAbout(id, userId);
		}
		
		@PostMapping
		@Operation(summary = "新增關於我段落")
		@StandardApiResponses
		public BaseVo<Void> about(
						@Valid @RequestBody AboutDto aboutDto,
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId
		) {
				return aboutService.addAbout(aboutDto, userId);
		}
}
