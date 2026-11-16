package org.zstack.storage.zbs;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private String physicalServerSerialNumber;
    private MdsStatus status;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        MdsInfo other = (MdsInfo) obj;
        return Objects.equals(addr, other.addr) &&
                Objects.equals(port, other.port) &&
                Objects.equals(username, other.username) &&
                Objects.equals(password, other.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(addr, port, username, password);
    }

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

    public String getPhysicalServerSerialNumber() {
        return physicalServerSerialNumber;
    }

    public void setPhysicalServerSerialNumber(String physicalServerSerialNumber) {
        this.physicalServerSerialNumber = physicalServerSerialNumber;
    }

    public MdsStatus getStatus() {
        return status;
    }

    public void setStatus(MdsStatus status) {
        this.status = status;
    }

    public static MdsInfo valueOf(String mdsUrl) {
        MdsUri uri = new MdsUri(mdsUrl);
        MdsInfo mdsInfo = new MdsInfo();
        mdsInfo.setUsername(uri.getUsername());
        mdsInfo.setPassword(uri.getPassword());
        mdsInfo.setPort(uri.getSshPort());
        mdsInfo.setAddr(uri.getHostname());
        return mdsInfo;
    }

    public static List<MdsInfo> valueOf(Collection<String> mdsUrls) {
        return mdsUrls.stream().map(MdsInfo::valueOf).collect(Collectors.toList());
    }
}
