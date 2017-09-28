package me.hrps.schedule.zk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.hrps.schedule.config.RPScheduledAnnotationBeanPostProcessor;
import me.hrps.schedule.taskmanager.ScheduledMethodRunnable;
import me.hrps.schedule.taskmanager.ScheduledTaskProcessor;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;

import java.util.Set;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: javahuang
 * Create: 2017/9/28 上午10:25
 */
public class ScheduleDataManager4ZK {

    private ZKManager zkManager;
    private CuratorFramework zkClient;
    private Gson gson;


    public ScheduleDataManager4ZK(ZKManager zkManager) throws Exception {
        this.zkManager = zkManager;
        this.zkClient = zkManager.getClient();
        this.gson = new GsonBuilder().create();
    }


    public Set<ScheduledMethodRunnable> registerScheduledTasks(ScheduledTaskProcessor taskProcessor) throws Exception {
        RPScheduledAnnotationBeanPostProcessor annotationProcessor = taskProcessor.getApplicationContext().getBean(RPScheduledAnnotationBeanPostProcessor.class);
        Set<ScheduledMethodRunnable> tasks = annotationProcessor.getScheduledTasks();
        tasks.forEach(task -> {
            String zkPath = "/" + task.getTaskName();
            try {
                // 设置 processor
                task.setProcessor(taskProcessor);
                if (this.zkClient.checkExists().forPath(zkPath) == null) {
                    this.zkClient.create().withMode(CreateMode.EPHEMERAL).withACL(this.zkManager.getAclList()).forPath(zkPath);
                }
                // 设置数据
                this.zkClient.setData().forPath(zkPath, this.gson.toJson(task).getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return tasks;
    }


    public void refreshScheduledTask(ScheduledMethodRunnable task) throws Exception {
        String zkPath = "/" + task.getTaskName();
        if (this.zkClient.checkExists().forPath(zkPath) == null) {
            this.zkClient.create().withMode(CreateMode.EPHEMERAL).forPath(zkPath);
        }
        this.zkClient.setData().forPath(zkPath, this.gson.toJson(task).getBytes());
    }
}
