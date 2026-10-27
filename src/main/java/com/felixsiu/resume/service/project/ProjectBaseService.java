package com.felixsiu.resume.service.project;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.felixsiu.resume.model.entity.Project;
import com.felixsiu.resume.respository.ProjectsMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectBaseService extends ServiceImpl<ProjectsMapper, Project> {
		
		/**
		 * query project list
		 *
		 * @param userId   user id
		 * @param featured query featured only
		 * @return project entity list
		 */
		public List<Project> selectList(String userId, Boolean featured) {
				LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
				wrapper.eq(Project::getUserId, userId);
				
				if (featured) {
						wrapper.eq(Project::getFeatured, true);
				}
				
				return this.list(wrapper);
		}
		
		/**
		 * query one by slug
		 *
		 * @param userId    user id
		 * @param querySlug query slug
		 * @return optional project entity
		 */
		public Project selectOneBySlug(String userId, String querySlug) {
				LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
				wrapper
								.eq(Project::getUserId, userId)
								.eq(Project::getSlug, querySlug);
				
				return this.getOne(wrapper);
		}
		
		/**
		 * query one by id
		 *
		 * @param userId user id
		 * @param id     PK id
		 * @return optional project entity
		 */
		public Project selectOneById(String userId, Integer id) {
				LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
				wrapper
								.eq(Project::getId, id)
								.eq(Project::getUserId, userId);
				
				return this.getOne(wrapper);
		}
		
		/**
		 * get project maximum sort order by user
		 *
		 * @param userId user id
		 * @return maximum sort order
		 */
		public Integer getLastSortOrder(String userId) {
				LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
				wrapper
								.eq(Project::getUserId, userId)
								.orderByDesc(Project::getSortOrder)
								.last("LIMIT 1");
				
				Project lastProject = this.getOne(wrapper);
				return lastProject == null ? 0 : lastProject.getSortOrder();
		}
		
		/**
		 * insert project
		 *
		 * @param projectEntity project entity
		 * @return bool
		 */
		public boolean insert(Project projectEntity) {
				return this.save(projectEntity);
		}
		
		/**
		 * update project by id
		 *
		 * @param projectEntity project entity
		 * @return bool
		 */
		public boolean updateByPK(Project projectEntity) {
				return this.updateById(projectEntity);
		}
		
		/**
		 * delete project by id
		 *
		 * @param userId user id
		 * @param id     project id
		 * @return bool
		 */
		public boolean deleteByPKAndUser(String userId, Integer id) {
				LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
				wrapper
								.eq(Project::getUserId, userId)
								.eq(Project::getId, id);
				
				return this.remove(wrapper);
		}
}
