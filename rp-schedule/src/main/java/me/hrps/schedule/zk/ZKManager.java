package me.hrps.schedule.zk;

import com.google.common.collect.Lists;
import me.hrps.schedule.taskmanager.ScheduledTaskProcessor;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: javahuang
 * Create: 2017/9/28 上午9:54
 */
public class ZKManager {

    private static Logger logger = LoggerFactory.getLogger(ZKManager.class);

    private ZKConfig config;
    private CuratorFramework client;
    private ScheduledTaskProcessor scheduledTaskProcessor;
    private List<ACL> aclList = Lists.newArrayList();

    public ZKManager(ZKConfig config, ScheduledTaskProcessor taskProcessor) throws Exception{
        this.config = config;
        this.scheduledTaskProcessor = taskProcessor;
        this.connect();
    }

    private void connect() throws Exception {
        CountDownLatch connectionLatch = new CountDownLatch(1);
        createZookeeper(connectionLatch);
        connectionLatch.await(10, TimeUnit.SECONDS);
    }

    public void reConnect() throws Exception {
        if (this.client != null) {
            this.client.close();
            this.client = null;
            this.connect();
        }
    }

    private void createZookeeper(CountDownLatch connectedLatch) throws Exception {
        String authString = config.getZkUserName() + ":"
                + config.getZkPassword();
        if (config.getZkUserName() == null) {
            authString = null;
        }
        String namespace = config.getZkRootPath().substring(1);
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .connectString((String) config.getZkConnectString())
                .namespace(namespace)
                .sessionTimeoutMs(config.getZkSessionTimeout())
                .retryPolicy(new ExponentialBackoffRetry(1000, 2))
                .defaultData(null);  // forPath() 的时候不设置默认值
        if (authString != null) {
            builder.authorization("digest", authString.getBytes());
        }
        client = builder.build();
        client.getConnectionStateListenable().addListener((client, newState) -> {
            if (newState.isConnected()) {
                logger.info("zookeeper has connected...");
                try {
                   new Thread(scheduledTaskProcessor).start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                connectedLatch.countDown();
            } else {
                logger.error("zookeeper can't connected");
                connectedLatch.countDown();
            }
        });
        client.start();
        aclList.clear();
        if (authString != null) {
            aclList.add(new ACL(ZooDefs.Perms.ALL, new Id("digest",
                    DigestAuthenticationProvider.generateDigest(authString))));
            aclList.add(new ACL(ZooDefs.Perms.READ, ZooDefs.Ids.ANYONE_ID_UNSAFE));
        } else {
            aclList.add(new ACL(ZooDefs.Perms.ALL, ZooDefs.Ids.ANYONE_ID_UNSAFE));
        }
    }

    /**
     * 关闭连接
     */
    public void close() {
        logger.info("close zookeeper...");
        if (this.client.getState() == CuratorFrameworkState.STARTED) {
            this.client.close();
        }
    }

    /**
     * @return 检查 zookeeper 是否已连接
     */
    public boolean isZkConnected() {
        return this.client.getZookeeperClient().isConnected();
    }


    public String getRootPath() {
        return this.config.getZkRootPath();
    }

    public CuratorFramework getClient() throws Exception {
        if (!isZkConnected()) {
            reConnect();
        }
        return this.client;
    }

    public List<ACL> getAclList() {
        return aclList;
    }
}
