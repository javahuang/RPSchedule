package me.hrps;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: javahuang
 * Create: 2017/9/28 下午3:36
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DynamicScheduledTests {

    /**
     * 1. start the application
     * 2. run this test
     * 3. watch console change
     * @throws Exception
     */
    @Test
    public void test() throws Exception{
        String namespace = "schedule-tasks";
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString("localhost:2181")
                .namespace(namespace)
                .sessionTimeoutMs(6000)
                .retryPolicy(new RetryOneTime(1))
                .defaultData(null)
                .build();
        client.start();
        String taskNodePath = "/scheduleTaskBean$taskWithParameter";
        // run the task immediate
        String zkNodeData = "{\"cron\":\"0/15 * * * * ?\",\"taskName\":\"scheduleTaskBean$taskWithParameter\",\"startRun\":true}";
        client.setData().forPath(taskNodePath , zkNodeData.getBytes());
        // change task parameter
        String zkNodeData1 = "{\"cron\":\"0/15 * * * * ?\",\"taskName\":\"scheduleTaskBean$taskWithParameter\",\"argStr\":\"lily 22\"}";
        client.setData().forPath(taskNodePath, zkNodeData1.getBytes());
        // dynamic set task run time
        String zkNodeData2 = "{\"cron\":\"0/3 * * * * ?\",\"taskName\":\"scheduleTaskBean$taskWithParameter\",\"argStr\":\"lily 22\"}";
        client.setData().forPath(taskNodePath, zkNodeData2.getBytes());
    }
}
