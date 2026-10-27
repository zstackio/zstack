package org.zstack.kvm.memory;

import javax.persistence.*;

/** Public Host memory state for QueryHostMemoryState, refreshed from MemoryStateVO by the presentation rules. */
@Entity
@Table
public class HostMemoryStateVO {
    @Id @Column(length = 32)
    private String hostUuid;
    public String getHostUuid() { return hostUuid; }
    public void setHostUuid(String value) { hostUuid = value; }

    @Column(nullable = false, length = 32)
    private String status;
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }

    @Column(nullable = false, length = 32)
    private String featureState;
    public String getFeatureState() { return featureState; }
    public void setFeatureState(String value) { featureState = value; }

    @Column(length = 2048)
    private String reason;
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }

    @Column(columnDefinition = "TEXT")
    private String capabilities;
    public String getCapabilities() { return capabilities; }
    public void setCapabilities(String value) { capabilities = value; }

    @Column(columnDefinition = "TEXT")
    private String allowedActions;
    public String getAllowedActions() { return allowedActions; }
    public void setAllowedActions(String value) { allowedActions = value; }


    @Column(length = 32)
    private String activeJobUuid;
    public String getActiveJobUuid() { return activeJobUuid; }
    public void setActiveJobUuid(String value) { activeJobUuid = value; }

    @Column
    private Long lastObservedAt;
    public Long getLastObservedAt() { return lastObservedAt; }
    public void setLastObservedAt(Long value) { lastObservedAt = value; }

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
        if (createDate == null) { createDate = now; }
        lastOpDate = now;
    }

    @PreUpdate
    private void onUpdate() { lastOpDate = new java.sql.Timestamp(System.currentTimeMillis()); }
}
