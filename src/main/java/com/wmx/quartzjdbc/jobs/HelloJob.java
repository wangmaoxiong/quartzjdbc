package com.wmx.quartzjdbc.jobs;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wangmaoxiong
 * Job 是 定时任务的具体执行逻辑
 * JobDetail 是 定时任务的定义
 */
@DisallowConcurrentExecution
public class HelloJob implements Job {
    private static Logger logger = LoggerFactory.getLogger(HelloJob.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        logger.info("hello world  ! ");
    }
}

