package com.wmx.quartzjdbc.jobs;

import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * http get 请求作业
 * <p>
 * 当任务的执行时间过长，而触发的时间间隔小于执行时间，则会导致同一个 JobDetail 实例被并发执行，如果不想让它并发执行，
 * 则加上 @DisallowConcurrentExecution、@PersistJobDataAfterExecution
 *
 * @author wangmaoxiong
 * @version 1.0
 * @date 2020/4/9 16:28
 */
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
public class RequestJob implements Job {
    private static Logger logger = LoggerFactory.getLogger(RequestJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDetail jobDetail = context.getJobDetail();
        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        logger.info("group={},name={},description={},url={}",
                jobDetail.getKey().getGroup(),
                jobDetail.getKey().getName(),
                jobDetail.getDescription(),
                jobDataMap.getString("url"));
    }
}
