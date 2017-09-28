package me.hrps.schedule.taskmanager;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.hrps.schedule.CronExpression;
import me.hrps.schedule.zk.ScheduleDataManager4ZK;
import me.hrps.schedule.zk.ZKConfig;
import me.hrps.schedule.zk.ZKManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.NamedBeanHolder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: javahuang
 * Create: 2017/9/28 上午10:16
 */
public class ScheduledTaskProcessor implements Runnable, ApplicationContextAware, BeanNameAware, InitializingBean, DisposableBean {

    Logger logger = LoggerFactory.getLogger(ScheduledTaskProcessor.class);

    BeanFactory beanFactory;
    String beanName;
    TaskScheduler taskScheduler;
    ZKManager zkManager;
    ZKConfig zkConfig;
    ScheduleDataManager4ZK scheduleDataManager4ZK;
    private ApplicationContext applicationContext;
    private Gson gson;

    public static final String DEFAULT_TASK_SCHEDULER_BEAN_NAME = "taskScheduler";
    // 默认调度线程池线程数量
    public static int core_pool_size = 2;

    static Map<String, ScheduledFuture> scheduledResult = Maps.newConcurrentMap();
    static Map<String, Boolean> watchedTasks = Maps.newConcurrentMap();
    public static List<NodeCache> nodeCacheList = Lists.newArrayList();


    public ScheduledTaskProcessor(ZKConfig config) {
        this.zkConfig = config;
        this.gson = new GsonBuilder().create();
    }

    @Override
    public void run() {
        try {
            this.scheduleDataManager4ZK = new ScheduleDataManager4ZK(this.zkManager);
            // register tasks to zookeeper
            Set<ScheduledMethodRunnable> tasks = scheduleDataManager4ZK.registerScheduledTasks(this);
            // add watcher to tasks
            watchTasks(tasks);
            // cancel task if running
            cancelTasks();
            // begin schedule
            for (ScheduledMethodRunnable task : tasks) {
                scheduleTask(task);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            this.zkManager = new ZKManager(this.zkConfig, this);
            getTaskScheduler();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void destroy() throws Exception {
        nodeCacheList.forEach(nodeCache -> {
            try {
                nodeCache.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        this.zkManager.close();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static void cancelTasks() {
        scheduledResult.forEach((taskName, future) -> {
            if (!future.isDone()) {
                future.cancel(false);
            }
        });
    }

    private void watchTasks(Set<ScheduledMethodRunnable> tasks) throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        tasks.forEach(task -> {
            // watch node data change
            try {
                String zkPath = "/" + task.getTaskName();
                if (watchedTasks.getOrDefault(zkPath, false)) {
                    return;
                }
                NodeCache nodeCache = new NodeCache(this.zkManager.getClient(), zkPath, false);
                nodeCache.start(true);
                nodeCacheList.add(nodeCache);
                watchedTasks.put(zkPath, true);
                // determine whether the node data has changed
                nodeCache.getListenable().addListener(() -> {
                    if (nodeCache.getCurrentData() == null) {
                        return;
                    }
                    ScheduledMethodRunnable currTask = this.gson.fromJson(new String(nodeCache.getCurrentData().getData()), ScheduledMethodRunnable.class);
                    boolean needReloadTask = false;
                    if (currTask == null || currTask.getCron() == null) {
                        task.setMsg("config error");
                        needReloadTask = true;
                    } else {
                        if (!currTask.getCron().equalsIgnoreCase(task.getCron())) {
                            task.setCron(currTask.getCron());
                            needReloadTask = true;
                        }
                        if (!StringUtils.equalsIgnoreCase(task.getArgStr(), currTask.getArgStr())) {
                            task.setArgStr(currTask.getArgStr());
                            task.parseArgStrToArgs();
                            needReloadTask = true;
                        }
                        if (currTask.isStartRun()) { // 任务非运行状态
                            needReloadTask = true;
                        }
                    }
                    if (!needReloadTask) {
                        return;
                    }
                    task.setMsg(null);
                    // waiting the currTask finished
                    while (currTask.getCurrThread() != null) {
                        Thread.sleep(20);
                    }
                    // cancel the tasks which is ready to run ,then reload
                    ScheduledFuture<?> futureTask = getScheduledResult().get(task.getTaskName());
                    if (futureTask != null && !futureTask.isDone()) {
                        futureTask.cancel(false);
                    }
                    // startup immediate
                    if (currTask.isStartRun()) {
                        task.setRunning(true);
                    }
                    scheduleTask(task);
                }, pool);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * start schedule
     *
     * @param task
     */
    public void scheduleTask(ScheduledMethodRunnable task) {
        synchronized (task) {
            // show error msg
            if (task.getMsg() != null) {
                refreshTaskRunningInfo(task);
                return;
            }
            try {
                String taskName = task.getTaskName();
                if (task.isRunning()) {
                    taskScheduler.schedule(task, new Date());
                    return;
                }
                CronExpression cronExp = new CronExpression(task.getCron());
                Date startTime = cronExp.getNextValidTimeAfter(new Date());
                ScheduledFuture<?> future = taskScheduler.schedule(task, startTime);
                scheduledResult.put(taskName, future);
            } catch (ParseException p) {    // cron 表达式解析失败
                task.setMsg("cron exp incorrect");
                refreshTaskRunningInfo(task);
                logger.error("task cron exp {} incorrect", task.getTaskName(), p);
            } catch (Exception e) {
                logger.error("task {} schedule failure", task.getTaskName(), e);
            }
        }
    }

    private void getTaskScheduler() {
        try {
            this.taskScheduler = resolveSchedulerBean(TaskScheduler.class, false);
        } catch (Exception e) {
            // 调度任务线程数
            this.taskScheduler = new ConcurrentTaskScheduler(Executors.newScheduledThreadPool(core_pool_size));
        }
    }

    private <T> T resolveSchedulerBean(Class<T> schedulerType, boolean byName) {
        if (byName) {
            T scheduler = this.beanFactory.getBean(DEFAULT_TASK_SCHEDULER_BEAN_NAME, schedulerType);
            if (this.beanFactory instanceof ConfigurableBeanFactory) {
                ((ConfigurableBeanFactory) this.beanFactory).registerDependentBean(
                        DEFAULT_TASK_SCHEDULER_BEAN_NAME, this.beanName);
            }
            return scheduler;
        } else if (this.beanFactory instanceof AutowireCapableBeanFactory) {
            NamedBeanHolder<T> holder = ((AutowireCapableBeanFactory) this.beanFactory).resolveNamedBean(schedulerType);
            if (this.beanFactory instanceof ConfigurableBeanFactory) {
                ((ConfigurableBeanFactory) this.beanFactory).registerDependentBean(
                        holder.getBeanName(), this.beanName);
            }
            return holder.getBeanInstance();
        } else {
            return this.beanFactory.getBean(schedulerType);
        }
    }

    public void refreshTaskRunningInfo(ScheduledMethodRunnable task) {
        try {
            this.scheduleDataManager4ZK.refreshScheduledTask(task);
        } catch (Exception e) {
            logger.error("刷新运行时任务({})信息失败", task.getTaskName(), e);
        }
    }

    public Map<String, ScheduledFuture> getScheduledResult() {
        return scheduledResult;
    }
}
