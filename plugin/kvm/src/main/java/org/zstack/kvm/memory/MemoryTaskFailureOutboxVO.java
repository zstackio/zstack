package org.zstack.kvm.memory;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.sql.Timestamp;

/** Durable notification intent created in the same transaction as a Host task's Failed transition. */
@Entity
@Table
public class MemoryTaskFailureOutboxVO {
    @Id
    @Column(length = 32)
    private String taskUuid;
    public String getTaskUuid() { return taskUuid; }
    public void setTaskUuid(String value) { taskUuid = value; }

    @Column(nullable = false, length = 32)
    private String hostUuid;
    public String getHostUuid() { return hostUuid; }
    public void setHostUuid(String value) { hostUuid = value; }

    @Column(nullable = false, length = 32)
    private String action;
    public String getAction() { return action; }
    public void setAction(String value) { action = value; }

    @Column(length = 2048)
    private String reason;
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }

    @Column(nullable = false)
    private Timestamp createDate;
    public Timestamp getCreateDate() { return createDate; }
    public void setCreateDate(Timestamp value) { createDate = value; }

    @Column
    private String leaseOwner;
    public String getLeaseOwner() { return leaseOwner; }
    public void setLeaseOwner(String value) { leaseOwner = value; }

    @Column
    private Long leaseUntil;
    public Long getLeaseUntil() { return leaseUntil; }
    public void setLeaseUntil(Long value) { leaseUntil = value; }

    @Column(nullable = false)
    private int attempts;
    public int getAttempts() { return attempts; }
    public void setAttempts(int value) { attempts = value; }

    @Column(nullable = false)
    private boolean delivered;
    public boolean isDelivered() { return delivered; }
    public void setDelivered(boolean value) { delivered = value; }

    @Column(length = 512)
    private String lastError;
    public String getLastError() { return lastError; }
    public void setLastError(String value) { lastError = value; }

    @Column(nullable = false)
    private Timestamp lastOpDate;
    public Timestamp getLastOpDate() { return lastOpDate; }
    public void setLastOpDate(Timestamp value) { lastOpDate = value; }

    @PrePersist
    private void onCreate() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (createDate == null) { createDate = now; }
        lastOpDate = now;
    }

    @PreUpdate
    private void onUpdate() { lastOpDate = new Timestamp(System.currentTimeMillis()); }

    public MemoryTaskFailureEvent toEvent() {
        MemoryTaskFailureEvent event = new MemoryTaskFailureEvent();
        event.setHostUuid(hostUuid);
        event.setTaskUuid(taskUuid);
        event.setAction(action);
        event.setReason(reason);
        event.setStatus("Failed");
        event.setCreateTimeMillis(createDate == null ? System.currentTimeMillis() : createDate.getTime());
        return event;
    }
}
