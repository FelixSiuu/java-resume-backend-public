package com.felixsiu.resume.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.felixsiu.resume.model.entity.ProjectDetail;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectDetailMapper extends BaseMapper<ProjectDetail> {
}
