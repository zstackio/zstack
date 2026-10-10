package org.zstack.kvm.memory;

import javax.persistence.*;

/** Durable, installer-created intent; schema migration intentionally creates no row. */
@Entity @Table
public class MemoryCloudBootstrapVO {
    public static final String GLOBAL = "Global:global";
    public static final String PENDING = "Pending";
    public static final String APPLIED = "Applied";
    public static final String CANCELLED = "Cancelled";
    public static final String NEEDS_REVIEW = "NeedsReview";

    @Id @Column(length = 32)
    private String uuid;
    public String getUuid() { return uuid; }
    public void setUuid(String value) { uuid = value; }

    @Column(nullable = false, length = 32)
    private String status;
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }

    @Column(nullable = false, unique = true, length = 32)
    private String requestUuid;
    public String getRequestUuid() { return requestUuid; }
    public void setRequestUuid(String value) { requestUuid = value; }

    @Column(length = 32)
    private String taskUuid;
    public String getTaskUuid() { return taskUuid; }
    public void setTaskUuid(String value) { taskUuid = value; }

    @Column(length = 64)
    private String targetPolicyHash;
    public String getTargetPolicyHash() { return targetPolicyHash; }
    public void setTargetPolicyHash(String value) { targetPolicyHash = value; }

    @Column(length = 2048)
    private String reason;
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }

    @Column(nullable = false)
    private java.sql.Timestamp createDate;
    public java.sql.Timestamp getCreateDate() { return createDate; }
    public void setCreateDate(java.sql.Timestamp value) { createDate = value; }

    @Column(nullable = false)
    private java.sql.Timestamp lastOpDate;
    public java.sql.Timestamp getLastOpDate() { return lastOpDate; }
    public void setLastOpDate(java.sql.Timestamp value) { lastOpDate = value; }

    @PrePersist private void onCreate() {
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
        createDate = now; lastOpDate = now;
    }
    @PreUpdate private void onUpdate() { lastOpDate = new java.sql.Timestamp(System.currentTimeMillis()); }
}
