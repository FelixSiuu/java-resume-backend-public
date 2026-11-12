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
		 * 每月 1 號 00:00:00 執行清理
		 */
		@Scheduled(cron = "0 0 0 1 * *")
		public void cleanupAccount(){
				log.info("Cleaning up accounts");
				int deleteCount = accountBaseService.cleanUpDeletedAccount();
				log.info("Deleted {} accounts", deleteCount);
		}
}
