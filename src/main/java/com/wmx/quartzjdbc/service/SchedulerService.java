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
import java.util.*;

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
     * 注册并启动作业。如果 Job 或者 Trigger 已经存在，则替换它们.
     *
     * @param schedulerEntity
     * @throws IOException
     * @throws SchedulerException
     */
    public void scheduleJob(SchedulerEntity schedulerEntity) throws IOException, SchedulerException, ClassNotFoundException {
        JobDetail jobDetail = this.getJobDetail(schedulerEntity);
        Trigger trigger = this.getTrigger(schedulerEntity, null);

        //scheduleJob(JobDetail jobDetail, Trigger trigger):作业注册并启动。注意同一个组下面的任务详情或者触发器名称必须唯一，否则重复注册时会报错，已经存在.
        //scheduleJobs(Map<JobDetail, Set<? extends Trigger>> triggersAndJobs, boolean replace)
        // replace=true，表示如果存储相同的 Job 或者 Trigger ，则替换它们
        //因为全局配置文件中配置了 spring.quartz.uto-startup=true，所以不再需要手动启动：scheduler.start()
        Set<Trigger> triggerSet = new HashSet<>();
        triggerSet.add(trigger);
        scheduler.scheduleJob(jobDetail, triggerSet, true);
        logger.info("注册并启动作业:{}", schedulerEntity);
    }

    /**
     * 内部方法：处理 Trigger
     *
     * @param schedulerEntity
     * @return
     */
    private Trigger getTrigger(SchedulerEntity schedulerEntity, JobKey jobKey) {
        //触发器参数
        //schedulerEntity 中 job_data 属性值必须设置为 json 字符串格式，所以这里转为 JobDataMap 对象.
        JobDataMap triggerDataMap = new JobDataMap();
        Map<String, Object> triggerData = schedulerEntity.getTrigger_data();
        if (triggerData != null && triggerData.size() > 0) {
            triggerDataMap.putAll(triggerData);
        }
        //如果触发器名称为空，则使用 UUID 随机生成. group 为null时，会默认为 default.
        if (StringUtils.isBlank(schedulerEntity.getTrigger_name())) {
            schedulerEntity.setTrigger_name(UUID.randomUUID().toString());
        }
        //过期执行策略采用：MISFIRE_INSTRUCTION_DO_NOTHING
        //forJob：为触发器关联作业. 一个触发器只能关联一个作业.
        TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger();
        triggerBuilder.withIdentity(schedulerEntity.getTrigger_name(), schedulerEntity.getTrigger_group());
        triggerBuilder.withDescription(schedulerEntity.getTrigger_desc());
        triggerBuilder.usingJobData(triggerDataMap);
        if (jobKey != null && jobKey.getName() != null) {
            triggerBuilder.forJob(jobKey);
        }
        triggerBuilder.withSchedule(CronScheduleBuilder.cronSchedule(schedulerEntity.getCron_expression())
                .withMisfireHandlingInstructionDoNothing());
        return triggerBuilder.build();
    }

    /**
     * 内部方法：处理 JobDetail
     * storeDurably(boolean jobDurability)：指示 job 是否是持久性的。如果 job 是非持久的，当没有活跃的 trigger 与之关联时，就会被自动地从 scheduler 中删除。即非持久的 job 的生命期是由 trigger 的存在与否决定的.
     * requestRecovery(boolean jobShouldRecover) :指示  job 遇到故障重启后，是否是可恢复的。如果 job 是可恢复的，在其执行的时候，如果 scheduler 发生硬关闭（hard shutdown)（比如运行的进程崩溃了，或者关机了），则当 scheduler 重启时，该 job 会被重新执行。
     *
     * @param schedulerEntity
     * @return
     * @throws ClassNotFoundException
     */
    private JobDetail getJobDetail(SchedulerEntity schedulerEntity) throws ClassNotFoundException {
        //如果任务名称为空，则使用 UUID 随机生成.
        if (StringUtils.isBlank(schedulerEntity.getJob_name())) {
            schedulerEntity.setJob_name(UUID.randomUUID().toString());
        }
        Class<? extends Job> jobClass = (Class<? extends Job>) Class.forName(schedulerEntity.getJob_class_name());
        //作业参数
        JobDataMap jobDataMap = new JobDataMap();
        Map<String, Object> jobData = schedulerEntity.getJob_data();
        if (jobData != null && jobData.size() > 0) {
            jobDataMap.putAll(jobData);
        }
        //设置任务详情.
        return JobBuilder.newJob(jobClass)
                .withIdentity(schedulerEntity.getJob_name(), schedulerEntity.getJob_group())
                .withDescription(schedulerEntity.getJob_desc())
                .usingJobData(jobDataMap)
                .storeDurably(true)
                .requestRecovery(true)
                .build();
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
        TriggerKey triggerKey = TriggerKey.triggerKey(schedulerEntity.getTrigger_name(), schedulerEntity.getTrigger_group());
        // scheduler.getTrigger(TriggerKey triggerKey)：从调度器中获取指定的触发器
        //修改任务的触发器时，触发器必须存在再修改.
        CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
        if (trigger == null) {
            logger.warn("根据 {} -> {} 未查到对应触发器..", triggerKey.getGroup(), triggerKey.getName());
            return null;
        }
        Trigger triggerNew = this.getTrigger(schedulerEntity, null);
        /**
         * rescheduleJob(TriggerKey triggerKey, Trigger newTrigger)：重新注册作业
         *      先根据 triggerKey 删除指定的触发器，然后存储新触发器(newTrigger)，并关联相同的作业.
         * 一个触发器只能关联一个 Job，而 一个 Job 可以关联多个触发器.
         */
        Date nextDate = scheduler.rescheduleJob(triggerKey, triggerNew);
        logger.info("重新绑定作业触发器.{} -> {}." + triggerKey.getGroup(), triggerKey.getName());
        return nextDate;
    }

    /**
     * 注册 job 与 触发器。区别于上面的是这里会对 作业和触发器进行分开注册.
     * job_class_name 不能为空时，注册 JobDetail 作业详情，如果已经存在，则更新，不存在，则添加.
     * cron_expression 不为空时，注册触发器（注册触发器时，对应的作业必须先存在）：
     * <span>根据参数 job_name、job_group 获取 JobDetail，如果存在，则关联此触发器与 JobDetail，然后注册触发器，</span>
     *
     * @param schedulerEntity
     * @throws SchedulerException
     */
    public void scheduleJobOrTrigger(SchedulerEntity schedulerEntity) throws SchedulerException, ClassNotFoundException {
        //1)先处理 job
        String job_class_name = schedulerEntity.getJob_class_name();
        JobDetail jobDetail = null;
        if (StringUtils.isNotBlank(job_class_name)) {
            jobDetail = this.getJobDetail(schedulerEntity);
            //往调度器中添加作业.
            scheduler.addJob(jobDetail, true);
            logger.info("往调度器中添加作业 {}," + jobDetail.getKey());
        }
        //2）处理触发器
        String job_name = schedulerEntity.getJob_name();
        String job_group = schedulerEntity.getJob_group();
        if (jobDetail == null && StringUtils.isNotBlank(job_group) && StringUtils.isNotBlank(job_name)) {
            jobDetail = scheduler.getJobDetail(JobKey.jobKey(job_name, job_group));
        }
        String cron_expression = schedulerEntity.getCron_expression();
        Trigger trigger = null;
        if (jobDetail != null && StringUtils.isNotBlank(cron_expression)) {
            trigger = this.getTrigger(schedulerEntity, JobKey.jobKey(job_name, job_group));
        }

        if (trigger == null) {
            return;
        }
        //注册触发器。如果触发器不存在，则新增，否则修改
        boolean checkExists = scheduler.checkExists(trigger.getKey());
        if (checkExists) {
            //rescheduleJob(TriggerKey triggerKey, Trigger newTrigger)：更新指定的触发器.
            scheduler.rescheduleJob(trigger.getKey(), trigger);
        } else {
            //scheduleJob(Trigger trigger)：注册触发器，如果触发器已经存在，则报错.
            scheduler.scheduleJob(trigger);
        }
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
     * 删除多个作业
     *
     * @param jobKeyList
     * @throws SchedulerException
     */
    public void deleteJobList(List<JobKey> jobKeyList) throws SchedulerException {
        //如果 JobKey 指定的作业不存在，则 deleteJob(JobKey jobKey) 无实质性操作，不会抛异常.
        //删除作业的同时，关联的触发器也会一起被删除.
        scheduler.deleteJobs(jobKeyList);
        logger.info("删除作业 {}", jobKeyList);
    }

    /**
     * 清除/删除所有计划数据，包括所有的 Job，所有的 Trigger，所有的 日历。
     * jdbc 持久化时，clear 操作后，数据库相应表中的数据全部会被删除. {@link org.quartz.impl.jdbcjobstore.StdJDBCDelegate#clearData}
     *
     * @throws SchedulerException
     */
    public void clear() throws SchedulerException {
        scheduler.clear();
        logger.info("清除/删除所有计划数据，包括所有的 Job，所有的 Trigger，所有的 日历。");
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

    /**
     * 停止/关闭 quartz 调度程序，关闭了整个调度的线程池，意味者所有作业都不会继续执行。
     * 可以反复调用，即使当时已经被 shutdown
     *
     * @throws SchedulerException
     */
    public void shutdown() throws SchedulerException {
        scheduler.shutdown(true);
    }

}
