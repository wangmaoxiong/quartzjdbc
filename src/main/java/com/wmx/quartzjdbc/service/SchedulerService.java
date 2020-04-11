package com.wmx.quartzjdbc.service;

import com.wmx.quartzjdbc.config.BeanConfig;
import com.wmx.quartzjdbc.pojo.SchedulerEntity;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Scheduler 调度业务层，用于启动、暂停、删除作业(Job)
 *
 * @author wangmaoxiong
 * @version 1.0
 * @date 2020/4/9 16:33
 * @see Scheduler
 */
@Service
public class SchedulerService {

    private static Logger logger = LoggerFactory.getLogger(SchedulerService.class);
    /**
     * @see BeanConfig
     */
    @Resource
    private Scheduler scheduler;

    /**
     * 注册并启动作业
     *
     * @param schedulerEntity
     * @throws IOException
     * @throws SchedulerException
     */
    public void scheduleJob(SchedulerEntity schedulerEntity) throws IOException, SchedulerException, ClassNotFoundException {
        //schedulerEntity 中 job_data 属性值必须设置为 json 字符串格式，所以这里转为 JobDataMap 对象.
        JobDataMap jobDataMap = new JobDataMap();
        JobDataMap triggerDataMap = new JobDataMap();
        Map<String, Object> jobData = schedulerEntity.getJob_data();
        Map<String, Object> triggerData = schedulerEntity.getTrigger_data();
        //作业参数
        if (jobData != null && jobData.size() > 0) {
            jobDataMap.putAll(jobData);
        }
        //触发器参数
        if (triggerData != null && triggerData.size() > 0) {
            triggerDataMap.putAll(triggerData);
        }

        /**
         *  1）设置任务详情。使用 taskId 与 groupName 作为作业的名称与组名
         *  storeDurably(boolean jobDurability)：指示 job 是否是持久性的。如果 job 是非持久的，当没有活跃的 trigger 与之关联时，就会被自动地从 scheduler 中删除。即非持久的 job 的生命期是由 trigger 的存在与否决定的.
         *  requestRecovery(boolean jobShouldRecover) :指示  job 遇到故障重启后，是否是可恢复的。如果 job 是可恢复的，在其执行的时候，如果 scheduler 发生硬关闭（hard shutdown)（比如运行的进程崩溃了，或者关机了），则当 scheduler 重启时，该 job 会被重新执行。
         */
        if (StringUtils.isBlank(schedulerEntity.getJob_name())) {
            schedulerEntity.setJob_name(UUID.randomUUID().toString());
        }
        Class<? extends Job> jobClass = (Class<? extends Job>) Class.forName(schedulerEntity.getJob_class_name());
        JobDetail jobDetail = JobBuilder.newJob(jobClass)
                .withIdentity(schedulerEntity.getJob_name(), schedulerEntity.getJob_group())
                .withDescription(schedulerEntity.getJob_desc())
                .usingJobData(jobDataMap)
                .storeDurably(true)
                .requestRecovery(true)
                .build();

        //2）设置触发器，使用 taskId 与 groupName 作为触发器的名称与组名
        if (StringUtils.isBlank(schedulerEntity.getTrigger_name())) {
            schedulerEntity.setTrigger_name(UUID.randomUUID().toString());
        }
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(schedulerEntity.getTrigger_name(), schedulerEntity.getTrigger_group())
                .withDescription(schedulerEntity.getTrigger_desc())
                .usingJobData(triggerDataMap)
                .withSchedule(CronScheduleBuilder.cronSchedule(schedulerEntity.getCron_expression()))
                .build();

        //3）scheduleJob(JobDetail jobDetail, Trigger trigger)
        // 作业注册并启动。注意同一个组下面的任务详情或者触发器名称必须唯一，否则重复注册时会报错，已经存在.
        scheduler.scheduleJob(jobDetail, trigger);
        scheduler.start();
        logger.info("注册并启动作业:{}", schedulerEntity);
    }

    /**
     * 修改作业的 cron 触发器规则，并重新注册。
     * 比如任务开始定义的是每1小时执行一次，现在想让它每2小时执行一次，就可以修改触发器规则，然后重新注册.
     *
     * @param schedulerEntity
     * @return
     */
    public Date rescheduleJob(SchedulerEntity schedulerEntity) throws SchedulerException, IOException {
        //触发器参数
        JobDataMap triggerDataMap = new JobDataMap();
        Map<String, Object> triggerData = schedulerEntity.getTrigger_data();
        if (triggerData != null && triggerData.size() > 0) {
            triggerDataMap.putAll(triggerData);
        }
        /**
         * cronExpression：新设置的 cron 表达式字符串.
         * scheduler.getTrigger(TriggerKey triggerKey)：从调度器中获取指定的触发器
         * rescheduleJob(TriggerKey triggerKey, Trigger newTrigger)：重新注册作业
         *      先根据 triggerKey 删除指定的触发器，然后存储新触发器(newTrigger)，并关联相同的作业.
         * 一个触发器只能关联一个 Job，而 一个 Job 可以关联多个触发器.
         */
        String cronExpression = schedulerEntity.getCron_expression();
        if (StringUtils.isBlank(schedulerEntity.getTrigger_name())) {
            schedulerEntity.setTrigger_name(UUID.randomUUID().toString());
        }
        TriggerKey triggerKey = TriggerKey.triggerKey(schedulerEntity.getTrigger_name(), schedulerEntity.getTrigger_group());
        CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
        if (trigger != null) {
            Trigger triggerNew = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .withDescription(schedulerEntity.getTrigger_desc())
                    .usingJobData(triggerDataMap)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                    .build();
            Date nextDate = scheduler.rescheduleJob(triggerKey, triggerNew);
            logger.info("重新绑定作业触发器.{} -> {}." + triggerKey.getGroup(), triggerKey.getName());
            return nextDate;
        } else {
            logger.warn("根据 {} -> {} 未查到对应触发器..", triggerKey.getGroup(), triggerKey.getName());
        }
        return null;
    }

    /**
     * 删除作业。作业一旦删除，数据库中就不存在了。删除作业不需要先暂停，
     * deleteJobs(List<JobKey> jobKeys)：可以同时删除多个作业
     *
     * @param jobKey
     * @throws SchedulerException
     */
    public void deleteJob(JobKey jobKey) throws SchedulerException {
        //如果 JobKey 指定的作业不存在，则 deleteJob(JobKey jobKey) 无实质性操作，不会抛异常.
        //删除作业的同时，关联的触发器也会一起被删除.
        scheduler.deleteJob(jobKey);
        logger.info("删除作业 {} -> {}" + jobKey.getGroup(), jobKey.getName());
    }

    /**
     * 暂停作业。可以重复调用，即使任务已经被暂停了.
     * pauseJob(JobKey jobKey)：会暂停指定 Job 的所有触发器，即完全暂停作业
     * pauseTrigger(TriggerKey triggerKey)：暂停指定的触发器
     * pauseJobs(GroupMatcher<JobKey> matcher)：暂停匹配的组下面的所有的作业.
     *
     * @param jobKey
     * @throws SchedulerException
     */
    public void pauseJob(JobKey jobKey) throws SchedulerException {
        scheduler.pauseJob(jobKey);
        logger.info("暂停作业 {} -> {}" + jobKey.getGroup(), jobKey.getName());
    }

    /**
     * 暂停所有作业。
     * scheduler.pauseAll()：可以重复调用，即使任务已经被暂停了.
     *
     * @throws SchedulerException
     */
    public void pauseAll() throws SchedulerException {
        scheduler.pauseAll();
        logger.info("暂停所有作业.");
    }

    /**
     * 恢复指定作业继续运行。可以重复调用，即使作业已经被恢复，在运行.
     * 如果在暂停期间作业未触发，则恢复后执行过期策略，即选择是否将遗漏的补上，还是放弃等.:
     * https://wangmaoxiong.blog.csdn.net/article/details/105309522#%E9%94%99%E8%BF%87%E8%A7%A6%E5%8F%91(misfire%C2%A0Instructions)
     *
     * @param jobKey
     * @throws SchedulerException
     */
    public void resumeJob(JobKey jobKey) throws SchedulerException {
        //resumeTrigger(TriggerKey triggerKey)：恢复指定触发器
        //resumeJobs(GroupMatcher<JobKey> matcher)：恢复匹配的整个组下的所有作业.
        scheduler.resumeJob(jobKey);
        logger.info("恢复指定作业 {}", jobKey);
    }

    /**
     * 恢复所有作业。即使作业之前是单独暂停的，恢复所有时，都可以恢复.
     * scheduler.resumeAll()：可以重复调用，即使作业已经被恢复，在运行.
     *
     * @throws SchedulerException
     */
    public void resumeAll() throws SchedulerException {
        scheduler.resumeAll();
        logger.info("恢复所有作业.");
    }

}
