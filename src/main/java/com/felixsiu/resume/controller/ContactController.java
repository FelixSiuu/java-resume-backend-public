package com.felixsiu.resume.controller;

import com.felixsiu.resume.annotation.StandardApiResponses;
import com.felixsiu.resume.model.dto.ContactDto;
import com.felixsiu.resume.model.vo.BaseVo;
import com.felixsiu.resume.model.vo.ContactVo;
import com.felixsiu.resume.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 聯繫方式
 */
@RestController
@RequestMapping("/api/v1/myInfo/contact")
@Tag(name = "Contact", description = "聯繫方式相關接口")
public class ContactController {
		
		@Autowired
		ContactService contactService;
		
		@GetMapping
		@Operation(
						summary = "獲取聯繫方式",
						description = "獲取當前登入用戶的聯繫方式"
		)
		@StandardApiResponses
		public BaseVo<ContactVo> contact(
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId
		) {
				return contactService.getContact(userId);
		}
		
		@PostMapping
		@Operation(summary = "修改聯繫方式")
		@StandardApiResponses
		public BaseVo<Void> updateContact(
						@Parameter(hidden = true)
						@RequestAttribute("userId") String userId, @Valid @RequestBody ContactDto contactDto
		) {
				return contactService.updateContact(userId, contactDto);
		}
}
