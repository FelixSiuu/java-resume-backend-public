package com.felixsiu.resume.model;

import lombok.Getter;

public enum EmailTokenMap {
		VERIFY("VERIFY", "verify email"),
		RESET("RESET", "reset password"),
		DELETE("DELETE", "delete account"),
		;
		
		@Getter
		private final String name;
		@Getter
		private final String description;
		
		EmailTokenMap(String name, String description) {
				this.name = name;
				this.description = description;
		}
}
