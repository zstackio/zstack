package org.zstack.kvm.memory;

import javax.persistence.*;

/** One operator operation on a Host, finished when the Host reaches its generation. */
@Entity
@Table
public class MemoryTaskVO {
    @Id @Column(length = 32)
    private String uuid;
    public String getUuid() { return uuid; }
    public void setUuid(String value) { uuid = value; }

    @Column(length = 32)
    private String hostUuid;
    public String getHostUuid() { return hostUuid; }
    public void setHostUuid(String value) { hostUuid = value; }

    @Column(nullable = false, length = 80)
    private String actorUuid;
    public String getActorUuid() { return actorUuid; }
    public void setActorUuid(String value) { actorUuid = value; }

    @Column(nullable = false, length = 32)
    private String action;
    public String getAction() { return action; }
    public void setAction(String value) { action = value; }

    @Column(nullable = false, length = 32)
    private String status;
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }

    @Column(length = 2048)
    private String reason;
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }

    /** The Host generation that carries this operation. */
    @Column(nullable = false)
    private long desiredRevision;
    public long getDesiredRevision() { return desiredRevision; }
    public void setDesiredRevision(long value) { desiredRevision = value; }

    @Column(nullable = false)
    private java.sql.Timestamp createDate;
    public java.sql.Timestamp getCreateDate() { return createDate; }
    public void setCreateDate(java.sql.Timestamp value) { createDate = value; }

    @Column(nullable = false)
    private java.sql.Timestamp lastOpDate;
    public java.sql.Timestamp getLastOpDate() { return lastOpDate; }
    public void setLastOpDate(java.sql.Timestamp value) { lastOpDate = value; }

    @PrePersist
    private void onCreate() {
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
        createDate = now;
        lastOpDate = now;
    }

    @PreUpdate
    private void onUpdate() { lastOpDate = new java.sql.Timestamp(System.currentTimeMillis()); }
}
