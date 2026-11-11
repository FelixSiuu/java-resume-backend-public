package com.felixsiu.resume.model.vo;

import lombok.Data;

@Data
public class EducationVo {
		private Integer id;
		private String startDate;
		private String endDate;
		private String institution;
		private String degree;
		private String major;
		private String location;
}
