package com.wmx.quartzjdbc.config;

import org.quartz.Scheduler;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

/**
 * @author wangmaoxiong
 * @version 1.0
 * @date 2020/4/9 16:49
 */
@Configuration
public class BeanConfig {
    /**
     * {@link QuartzAutoConfiguration } 类中自动将 {@link SchedulerFactoryBean }对象交由 Spring 容器管理.
     * 所以可以直接获取 SchedulerFactoryBean，然后根据它来获取 {@link Scheduler}。
     * 为了方便将 {@link Scheduler} 也交由容器管理.
     */
    @Resource
    private SchedulerFactoryBean schedulerFactoryBean;

    @Bean
    public Scheduler scheduler() {
        return schedulerFactoryBean.getScheduler();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
