package com.felixsiu.resume.service.project;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.felixsiu.resume.model.entity.ProjectStack;
import com.felixsiu.resume.respository.ProjectStacksMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectStacksService extends ServiceImpl<ProjectStacksMapper, ProjectStack> {
		
		/**
		 * query all stack
		 *
		 * @return stack list
		 */
		public List<ProjectStack> selectList() {
				return this.list();
		}
		
		public List<ProjectStack> selectListByProjectIds(List<Integer> projectIds) {
				if (projectIds == null || projectIds.isEmpty()) return List.of();
				LambdaQueryWrapper<ProjectStack> wrapper = new LambdaQueryWrapper<>();
				wrapper
								.in(ProjectStack::getProjectId, projectIds)
								.orderByAsc(ProjectStack::getProjectId)
								.orderByAsc(ProjectStack::getSortOrder);
				
				return this.list(wrapper);
		}
		
		/**
		 * query current project stack
		 *
		 * @param projectId project id (fk projects.id)
		 * @return stack list
		 */
		public List<ProjectStack> selectListByProjectId(Integer projectId) {
				LambdaQueryWrapper<ProjectStack> wrapper = new LambdaQueryWrapper<>();
				wrapper
								.eq(ProjectStack::getProjectId, projectId)
								.orderByAsc(ProjectStack::getSortOrder);
				
				return this.list(wrapper);
		}
		
		/**
		 * batch insert stack
		 *
		 * @param projectStackList stack list
		 * @return bool
		 */
		public boolean insertBatch(List<ProjectStack> projectStackList) {
				return this.saveBatch(projectStackList);
		}
		
		/**
		 * remove stack by  project id
		 *
		 * @param projectId project id (fk projects.id)
		 * @return bool
		 */
		public boolean deleteByProjectId(Integer projectId) {
				return this.remove(new LambdaQueryWrapper<ProjectStack>().eq(ProjectStack::getProjectId, projectId));
		}
}
