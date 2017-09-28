package me.hrps.schedule.spring.boot.autoconfigure;

import me.hrps.schedule.config.RPScheduledAnnotationBeanPostProcessor;
import me.hrps.schedule.config.ScheduleScannerRegistrarConfiguration;
import me.hrps.schedule.zk.ZKConfig;
import me.hrps.schedule.taskmanager.ScheduledTaskProcessor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: javahuang
 * Create: 2017/9/28 上午10:45
 */
@Configuration
@EnableConfigurationProperties(RPScheduleProperties.class)
@EnableScheduling
@Import({ ScheduleScannerRegistrarConfiguration.class })
public class RPScheduleAutoConfigure {

    @Autowired
    private RPScheduleProperties rpScheduleProperties;

    @Bean
    @ConditionalOnMissingBean
    public ScheduledTaskProcessor scheduledTaskProcessor() {
        ZKConfig config = new ZKConfig();
        BeanUtils.copyProperties(rpScheduleProperties, config);
        return new ScheduledTaskProcessor(config);
    }


    @Bean
    @ConditionalOnMissingBean
    public RPScheduledAnnotationBeanPostProcessor rpScheduledAnnotationBeanPostProcessor() {
        return new RPScheduledAnnotationBeanPostProcessor();
    }
}