package com.wmx.quartzjdbc.pojo;

import java.util.Map;

/**
 * 调度实体
 * sched_name：调度器名称
 * job_name：作业名称
 * job_group：作业所属组名称
 * job_desc：作业描述
 * job_class_name：作业详情关联的 Job 类名，如 {@link com.wmx.quartzjdbc.jobs.RequestJob}
 * job_data：作业的参数
 * trigger_name：触发器名称
 * trigger_group：触发器所属组名称
 * trigger_state：触发器的状态，如：PAUSED（暂停）、ACQUIRED（活动）、WAITING（等待）
 * trigger_desc:触发器描述
 * trigger_type：触发器类型，如 cron 表示 cron 触发器
 * trigger_data：触发器参数
 * cron_expression：cron 触发器表达式.
 *
 * @author wangmaoxiong
 * @version 1.0
 * @date 2020/4/10 14:41
 */
public class SchedulerEntity {
    private String sched_name;
    private String job_name;
    private String job_group;
    private String job_desc;
    private String job_class_name;
    private Map<String,Object> job_data;
    private String trigger_name;
    private String trigger_group;
    private String trigger_state;
    private String trigger_desc;
    private String trigger_type;
    private Map<String,Object> trigger_data;
    private String cron_expression;

    public String getSched_name() {
        return sched_name;
    }

    public void setSched_name(String sched_name) {
        this.sched_name = sched_name;
    }

    public String getJob_name() {
        return job_name;
    }

    public void setJob_name(String job_name) {
        this.job_name = job_name;
    }

    public String getJob_group() {
        return job_group;
    }

    public void setJob_group(String job_group) {
        this.job_group = job_group;
    }

    public String getJob_desc() {
        return job_desc;
    }

    public void setJob_desc(String job_desc) {
        this.job_desc = job_desc;
    }

    public String getJob_class_name() {
        return job_class_name;
    }

    public void setJob_class_name(String job_class_name) {
        this.job_class_name = job_class_name;
    }

    public String getTrigger_name() {
        return trigger_name;
    }

    public void setTrigger_name(String trigger_name) {
        this.trigger_name = trigger_name;
    }

    public String getTrigger_group() {
        return trigger_group;
    }

    public void setTrigger_group(String trigger_group) {
        this.trigger_group = trigger_group;
    }

    public String getTrigger_state() {
        return trigger_state;
    }

    public void setTrigger_state(String trigger_state) {
        this.trigger_state = trigger_state;
    }

    public String getTrigger_desc() {
        return trigger_desc;
    }

    public void setTrigger_desc(String trigger_desc) {
        this.trigger_desc = trigger_desc;
    }

    public String getTrigger_type() {
        return trigger_type;
    }

    public void setTrigger_type(String trigger_type) {
        this.trigger_type = trigger_type;
    }

    public String getCron_expression() {
        return cron_expression;
    }

    public void setCron_expression(String cron_expression) {
        this.cron_expression = cron_expression;
    }

    public Map<String, Object> getJob_data() {
        return job_data;
    }

    public void setJob_data(Map<String, Object> job_data) {
        this.job_data = job_data;
    }

    public Map<String, Object> getTrigger_data() {
        return trigger_data;
    }

    public void setTrigger_data(Map<String, Object> trigger_data) {
        this.trigger_data = trigger_data;
    }
}
