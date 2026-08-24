package com.felixsiu.resume.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WorkingExperienceVo {
		private Integer id;
		private String startDate;
		private String endDate;
		private String jobTitle;
		private String company;
		private String location;
		private LocalDateTime createTime;
		private LocalDateTime updateTime;
		private List<KeyResponsibilitiesVo> keyResponsibilities;
}
