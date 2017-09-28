package me.hrps.schedule.zk;

/**
 * Description:
 * <pre>
 * </pre>
 * Author: javahuang
 * Create: 2017/9/28 上午9:55
 */
public class ZKConfig {

    private String zkConnectString;
    private String zkRootPath;
    private Integer zkSessionTimeout;
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
