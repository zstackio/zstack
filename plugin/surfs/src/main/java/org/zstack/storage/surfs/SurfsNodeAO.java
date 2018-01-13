package org.zstack.storage.surfs;

import javax.persistence.*;

import java.sql.Timestamp;

/**
 * Created by zhouhaiping 2017-08-25
 */
@MappedSuperclass
public class SurfsNodeAO {
    @Id
    @Column
    private String uuid;

    @Column
    private String sshUsername;

    @Column
    private String sshPassword;

    @Column
    private String hostname;

    @Column
    private String nodeAddr;

    @Column
    private int nodePort = 6543;

    @Column
    private int sshPort = 22;

    @Column
    @Enumerated(EnumType.STRING)
    private NodeStatus status;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getNodeAddr() {
        return nodeAddr;
    }

    public void setNodeAddr(String monAddr) {
        this.nodeAddr = monAddr;
    }    
    
    public int getNodePort() {
        return nodePort;
    }

    public void setNodePort(int nodePort) {
        this.nodePort = nodePort;
    }    
    
    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

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

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public NodeStatus getStatus() {
        return status;
    }

    public void setStatus(NodeStatus status) {
        this.status = status;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
