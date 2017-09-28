package me.hrps.schedule.config.annotation;

import java.lang.annotation.*;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: javahuang
 * Create: 2017/9/28 上午9:56
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DynamicScheduled {

    String cron() default "";
}
