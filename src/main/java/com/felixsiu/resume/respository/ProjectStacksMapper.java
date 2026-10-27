package com.felixsiu.resume.respository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.felixsiu.resume.model.entity.ProjectStack;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectStacksMapper extends BaseMapper<ProjectStack> {
}
