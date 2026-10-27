package com.felixsiu.resume.service.project;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.felixsiu.resume.model.entity.ProjectDetail;
import com.felixsiu.resume.respository.ProjectDetailMapper;
import org.springframework.stereotype.Service;

@Service
public class ProjectDetailService extends ServiceImpl<ProjectDetailMapper, ProjectDetail> {
		
		/**
		 * query one detail
		 * @param projectId project id (PK fk projects.id)
		 * @return detail entity
		 */
		public ProjectDetail selectOne(Integer projectId) {
				return this.getOne(new LambdaQueryWrapper<ProjectDetail>().eq(ProjectDetail::getProjectId, projectId));
		}
		
		/**
		 * insert detail
		 * @param projectDetail detail entity
		 * @return bool
		 */
		public boolean insert(ProjectDetail projectDetail) {
				return this.save(projectDetail);
		}
		
		/**
		 * update detail by id (PK)
		 * @param projectDetail detail entity
		 * @return bool
		 */
		public boolean updateByPK(ProjectDetail projectDetail) {
				return this.updateById(projectDetail);
		}
}
