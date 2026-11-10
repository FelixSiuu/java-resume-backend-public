package com.felixsiu.resume.task;

import com.felixsiu.resume.service.RefreshTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RefreshTokenCleanupTask {
		@Autowired
		RefreshTokenService refreshTokenService;
		
		/**
		 * 每週日凌晨 3 點執行清理
		 * cron = "秒 分 時 日 月 星期"
		 * 星期欄位：SUN (或數字 0/7) 代表星期日
		 */
		@Scheduled(cron = "0 0 3 * * SUN")
		public void cleanupRefreshTokens() {
				log.info("Cleaning up refresh tokens");
				int deleteCount = refreshTokenService.cleanUpExpiredTokens();
				log.info("Deleted {} refresh tokens", deleteCount);
		}
}
