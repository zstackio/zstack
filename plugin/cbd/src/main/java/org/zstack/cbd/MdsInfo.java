package org.zstack.cbd;

/**
 * @author Xingwei Yu
 * @date 2024/4/10 23:18
 */
public class MdsInfo {
    private String sshUsername;
    private String sshPassword;
    private int sshPort = 22;
    private String mdsAddr;
    private MdsStatus mdsStatus;

    public String getSshUsername() {
        return sshUsername;
    }

    public void setSshUsername(String sshUsername) {
        this.sshUsername = sshUsername;
    }

    public String getSshPassword() {
        return sshPassword;
    }

    public void setSshPassword(String sshPassword) {
        this.sshPassword = sshPassword;
    }

    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }

    public String getMdsAddr() {
        return mdsAddr;
    }

    public void setMdsAddr(String mdsAddr) {
        this.mdsAddr = mdsAddr;
    }

    public MdsStatus getMdsStatus() {
        return mdsStatus;
    }

    public void setMdsStatus(MdsStatus mdsStatus) {
        this.mdsStatus = mdsStatus;
    }
}
