package com.wmx.quartzjdbc.jobs;

import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

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
@Service
public class RequestJob implements Job {
    private static Logger logger = LoggerFactory.getLogger(RequestJob.class);
    private static final String HTTP = "http";
    @Resource
    private RestTemplate restTemplate;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDetail jobDetail = context.getJobDetail();
        Trigger trigger = context.getTrigger();
        JobDataMap mergedJobDataMap = context.getMergedJobDataMap();
        logger.info("jobGroup={},jobName={},jobDesc={},triggerGroup={},triggerName={},triggerDesc={}",
                jobDetail.getKey().getGroup(),
                jobDetail.getKey().getName(),
                jobDetail.getDescription(),
                trigger.getKey().getGroup(),
                trigger.getKey().getName(),
                trigger.getDescription());
        Object url = mergedJobDataMap.get("url");
        if (url != null && StringUtils.isNotBlank(url.toString()) && url.toString().toLowerCase().startsWith(HTTP)) {
            //发送 http 请求.
            ResponseEntity<String> forEntity = restTemplate.getForEntity(url.toString(), String.class);
            logger.info("url={},StatusCode={}", url, forEntity.getStatusCode());
        }
    }
}
