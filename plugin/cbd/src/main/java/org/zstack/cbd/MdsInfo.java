package org.zstack.cbd;

/**
 * @author Xingwei Yu
 * @date 2024/4/10 23:18
 */
public class MdsInfo {
    public String sshUsername;
    public String sshPassword;
    public int sshPort = 22;
    public String mdsAddr;
    public int mdsPort = 6666;
    public MdsStatus mdsStatus;
    public String mdsVersion;

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

    public String getMdsAddr() {
        return mdsAddr;
    }

    public void setMdsAddr(String mdsAddr) {
        this.mdsAddr = mdsAddr;
    }

    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }

    public int getMdsPort() {
        return mdsPort;
    }

    public void setMdsPort(int mdsPort) {
        this.mdsPort = mdsPort;
    }

    public MdsStatus getMdsStatus() {
        return mdsStatus;
    }

    public void setMdsStatus(MdsStatus mdsStatus) {
        this.mdsStatus = mdsStatus;
    }

    public String getMdsVersion() {
        return mdsVersion;
    }

    public void setMdsVersion(String mdsVersion) {
        this.mdsVersion = mdsVersion;
    }
}
