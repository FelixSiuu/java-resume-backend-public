package com.felixsiu.resume.model;


import lombok.Getter;

public enum ErrorMap {
		SOMETHING_WRONG("出錯了", "SOMETHING WRONG", "0");
		
		@Getter
		private final String name;
		@Getter
		private final String code;
		@Getter
		private final String numCode;
		
		ErrorMap(String name, String code, String numCode) {
				this.name = name;
				this.code = code;
				this.numCode = numCode;
		}
}
