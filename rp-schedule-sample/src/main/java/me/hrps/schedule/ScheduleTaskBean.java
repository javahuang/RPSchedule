package me.hrps.schedule;

import me.hrps.schedule.config.annotation.DynamicScheduled;
import me.hrps.schedule.config.annotation.DynamicScheduledComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: javahuang
 * Create: 2017/9/28 下午2:39
 */
@DynamicScheduledComponent
public class ScheduleTaskBean {

    Logger logger = LoggerFactory.getLogger(ScheduleTaskBean.class);

    @DynamicScheduled(cron = "0/15 * * * * ?")
    public void task() {
        logger.info("hello");
    }


    @DynamicScheduled(cron = "0/15 * * * * ?")
    public void taskWithParameter(String name, Integer age) {
        if (name == null) {
            name = "Lucy";
        }
        if (age == null) {
            age = 20;
        }
        logger.info(name + "'s age is " + age);
    }
}
