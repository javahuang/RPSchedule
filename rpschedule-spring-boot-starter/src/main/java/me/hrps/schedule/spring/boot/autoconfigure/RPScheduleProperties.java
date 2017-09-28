package me.hrps.schedule.spring.boot.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: javahuang
 * Create: 2017/9/28 上午10:45
 */
@ConfigurationProperties(prefix = "rp.tbschedule")
public class RPScheduleProperties {

    private String zkConnectString = "localhost:2181";
    private String zkRootPath = "/schedule-tasks";
    private Integer zkSessionTimeout = 60 * 1000;
    private String zkUserName;
    private String zkPassword;


    public String getZkConnectString() {
        return zkConnectString;
    }

    public void setZkConnectString(String zkConnectString) {
        this.zkConnectString = zkConnectString;
    }

    public String getZkRootPath() {
        return zkRootPath;
    }

    public void setZkRootPath(String zkRootPath) {
        this.zkRootPath = zkRootPath;
    }

    public Integer getZkSessionTimeout() {
        return zkSessionTimeout;
    }

    public void setZkSessionTimeout(Integer zkSessionTimeout) {
        this.zkSessionTimeout = zkSessionTimeout;
    }

    public String getZkUserName() {
        return zkUserName;
    }

    public void setZkUserName(String zkUserName) {
        this.zkUserName = zkUserName;
    }

    public String getZkPassword() {
        return zkPassword;
    }

    public void setZkPassword(String zkPassword) {
        this.zkPassword = zkPassword;
    }
}
