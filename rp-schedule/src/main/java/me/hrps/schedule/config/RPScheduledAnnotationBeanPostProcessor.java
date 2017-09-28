package me.hrps.schedule.config;

import me.hrps.schedule.config.annotation.DynamicScheduled;
import me.hrps.schedule.taskmanager.ScheduledMethodRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description:
 * <pre>
 *     register the annotated @DynamicScheduled bean method as a task
 * </pre>
 * Author: javahuang
 * Create: 2017/9/28 上午10:01
 */
public class RPScheduledAnnotationBeanPostProcessor implements BeanPostProcessor, DestructionAwareBeanPostProcessor,
        Ordered, ApplicationContextAware,
        SmartInitializingSingleton, DisposableBean {
    private static Logger logger = LoggerFactory.getLogger(RPScheduledAnnotationBeanPostProcessor.class);

    private String beanName;
    private ApplicationContext applicationContext;

    private final Set<Class<?>> nonAnnotatedClasses = Collections.newSetFromMap(new ConcurrentHashMap<Class<?>, Boolean>());

    private final Set<ScheduledMethodRunnable> scheduledTasks = new LinkedHashSet<>(4);


    @Override
    public void destroy() throws Exception {
        synchronized (this.scheduledTasks) {
            this.scheduledTasks.clear();
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        this.nonAnnotatedClasses.clear();
        if (this.applicationContext == null) {
        }
    }

    @Override
    public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {

    }

    @Override
    public boolean requiresDestruction(Object bean) {
        return false;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        if (!this.nonAnnotatedClasses.contains(targetClass)) {
            // get method with @TBScheduled annotation in bean
            Map<Method, DynamicScheduled> annotatedMethods = MethodIntrospector.selectMethods(targetClass,
                    (MethodIntrospector.MetadataLookup<DynamicScheduled>) method ->
                            AnnotatedElementUtils.getMergedAnnotation(method, DynamicScheduled.class));
            if (annotatedMethods.isEmpty()) {
                this.nonAnnotatedClasses.add(targetClass);
                if (logger.isTraceEnabled()) {
                    logger.trace("No @Scheduled annotations found on bean class: " + bean.getClass());
                }
            } else {
                for (Map.Entry<Method, DynamicScheduled> entry : annotatedMethods.entrySet()) {
                    Method method = entry.getKey();
                    DynamicScheduled scheduled = entry.getValue();
                    processScheduled(scheduled, method, bean, beanName);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug(annotatedMethods.size() + " @Scheduled methods processed on bean '" + beanName +
                            "': " + annotatedMethods);
                }
            }
        }
        return bean;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }


    protected void processScheduled(DynamicScheduled scheduled, Method method, Object bean, String beanName) {
        try {
            Method invocableMethod = AopUtils.selectInvocableMethod(method, bean.getClass());
            ScheduledMethodRunnable task = new ScheduledMethodRunnable(bean, invocableMethod, scheduled.cron());
            task.setTaskName(beanName + "$" + method.getName());
            // init task parameter
            task.parseArgStrToArgs();
            synchronized (this.scheduledTasks) {
                scheduledTasks.add(task);
            }
        } catch (IllegalArgumentException e) {

        }
    }

    public Set<ScheduledMethodRunnable> getScheduledTasks() {
        return scheduledTasks;
    }
}
