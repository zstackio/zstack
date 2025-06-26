package org.zstack.cbd;

/**
 * @author Xingwei Yu
 * @date 2024/4/10 23:18
 */
public class MdsInfo {
    private String username;
    private String password;
    private int port = 22;
    private String addr;
    private String externalAddr;
    private MdsStatus status;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public String getExternalAddr() {
        return externalAddr;
    }

    public void setExternalAddr(String externalAddr) {
        this.externalAddr = externalAddr;
    }

    public MdsStatus getStatus() {
        return status;
    }

    public void setStatus(MdsStatus status) {
        this.status = status;
    }
}
