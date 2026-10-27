package org.zstack.kvm.memory;

import javax.persistence.*;

/** One Host's desired memory configuration, its delivery status and the last Agent observation. */
@Entity
@Table
public class MemoryStateVO {
    @Id @Column(length = 32)
    private String hostUuid;
    public String getHostUuid() { return hostUuid; }
    public void setHostUuid(String value) { hostUuid = value; }

    /** The generation of the desired configuration; every change increments it. */
    @Column(nullable = false)
    private long desiredRevision;
    public long getDesiredRevision() { return desiredRevision; }
    public void setDesiredRevision(long value) { desiredRevision = value; }

    @Column
    private Long appliedRevision;
    public Long getAppliedRevision() { return appliedRevision; }
    public void setAppliedRevision(Long value) { appliedRevision = value; }

    /** The operator's lifecycle choice: active, paused or drained. */
    @Column(nullable = false, length = 16)
    private String control = "active";
    public String getControl() { return control; }
    public void setControl(String value) { control = value; }

    /** The desired configuration last sent for desiredRevision. */
    @Column(columnDefinition = "TEXT")
    private String desired;
    public String getDesired() { return desired; }
    public void setDesired(String value) { desired = value; }

    @Column(nullable = false, length = 32)
    private String status;
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }

    @Column(length = 2048)
    private String reason;
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }

    @Column(nullable = false)
    private int failures;
    public int getFailures() { return failures; }
    public void setFailures(int value) { failures = value; }

    @Column
    private Long nextAttemptAt;
    public Long getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(Long value) { nextAttemptAt = value; }

    @Column(columnDefinition = "TEXT")
    private String capabilities;
    public String getCapabilities() { return capabilities; }
    public void setCapabilities(String value) { capabilities = value; }

    @Column(columnDefinition = "MEDIUMTEXT")
    private String state;
    public String getState() { return state; }
    public void setState(String value) { state = value; }

    @Column
    private Long lastSampleTime;
    public Long getLastSampleTime() { return lastSampleTime; }
    public void setLastSampleTime(Long value) { lastSampleTime = value; }

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
