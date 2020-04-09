package com.wmx.quartzjdbc.controller;

import com.wmx.quartzjdbc.jobs.HelloJob;
import com.wmx.quartzjdbc.pojo.ResultData;
import com.wmx.quartzjdbc.pojo.TaskDefine;
import com.wmx.quartzjdbc.service.HelloService;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

/**
 * @author wangmaoxiong
 */
@RestController
public class HelloController {

    @Resource
    private HelloService quartzJobService;

    @Autowired
    SchedulerFactoryBean schedulerFactoryBean;

    //假如 这个定时任务的 名字叫做HelloWorld, 组名GroupOne
    private final JobKey jobKey = JobKey.jobKey("HelloWorld", "GroupOne");

    /**
     * 启动 hello world
     */
    @GetMapping("/startHelloWorldJob")
    public String startHelloWorldJob() throws SchedulerException {

        //创建一个定时任务
        TaskDefine task = new TaskDefine();
        task.setJobKey(jobKey);
        task.setDescription("这是一个测试的 任务");
        task.setCronExpression("0/2 * * * * ?");
        task.setJobClass(HelloJob.class);

        quartzJobService.scheduleJob(task);
        return "startHelloWorldJob success";
    }

    /**
     * 暂停 hello world
     */
    @GetMapping("/pauseHelloWorldJob")
    public String pauseHelloWorldJob() throws SchedulerException {
        quartzJobService.pauseJob(jobKey);
        return "pauseHelloWorldJob success";
    }

    /**
     * 恢复 hello world
     */
    @GetMapping("/resumeHelloWorldJob")
    public String resumeHelloWorldJob() throws SchedulerException {
        quartzJobService.resumeJob(jobKey);
        return "resumeHelloWorldJob success";
    }

    /**
     * 删除 hello world
     */
    @GetMapping("/deleteHelloWorldJob")
    public String deleteHelloWorldJob() throws SchedulerException {
        quartzJobService.deleteJob(jobKey);
        return "deleteHelloWorldJob success";
    }

    /**
     * 修改 hello world 的cron表达式
     */
    @GetMapping("/modifyHelloWorldJobCron")
    public String modifyHelloWorldJobCron() {

        TaskDefine modifyCronTask = new TaskDefine();
        modifyCronTask.setJobKey(jobKey);
        modifyCronTask.setDescription("增强版调度任务");
        modifyCronTask.setCronExpression("0/10 * * * * ?");
        modifyCronTask.setJobClass(HelloJob.class);

        if (quartzJobService.modifyJobCron(modifyCronTask)) {
            return "modifyHelloWorldJobCron success";
        } else {
            return "modifyHelloWorldJobCron fail";
        }
    }

    /**
     * http://localhost:8080/getJobs
     *
     * @return
     */
    @GetMapping("getJobs")
    public ResultData getJobs() {
        ResultData resultData = null;
        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            List<String> calendarNames = scheduler.getCalendarNames();
            List<JobExecutionContext> executingJobs = scheduler.getCurrentlyExecutingJobs();
            List<String> jobGroupNames = scheduler.getJobGroupNames();
            Set<String> pausedTriggerGroups = scheduler.getPausedTriggerGroups();
            List<String> triggerGroupNames = scheduler.getTriggerGroupNames();

            System.out.println("calendarNames=" + calendarNames);
            System.out.println("executingJobs=" + executingJobs);
            System.out.println("jobGroupNames=" + jobGroupNames);
            System.out.println("pausedTriggerGroups=" + pausedTriggerGroups);
            System.out.println("triggerGroupNames=" + triggerGroupNames);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }


        return resultData;
    }
}

