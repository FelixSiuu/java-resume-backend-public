package com.felixsiu.resume.task;

import com.felixsiu.resume.service.account.AccountBaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AccountCleanUpTask {
		@Autowired
		private AccountBaseService accountBaseService;
		
		/**
		 * 每天凌晨 3 點：硬清軟刪已滿 30 天的帳號
		 */
		@Scheduled(cron = "0 0 3 * * *")
		public void cleanupAccount() {
				log.info("Cleaning up soft-deleted accounts older than {} days",
								AccountBaseService.ACCOUNT_PURGE_GRACE_DAYS);
				int deleteCount = accountBaseService.cleanUpDeletedAccount();
				log.info("Hard-deleted {} accounts", deleteCount);
		}
}
