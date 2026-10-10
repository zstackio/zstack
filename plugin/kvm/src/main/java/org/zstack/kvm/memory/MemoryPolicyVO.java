package org.zstack.kvm.memory;

import javax.persistence.*;

@Entity
@Table
public class MemoryPolicyVO {
    @Id @Column(length = 80)
    private String uuid;
    public String getUuid() { return uuid; }
    public void setUuid(String value) { uuid = value; }

    @Column(nullable = false, length = 16)
    private String scope;
    public String getScope() { return scope; }
    public void setScope(String value) { scope = value; }

    @Column(nullable = false, length = 64)
    private String resourceUuid;
    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String value) { resourceUuid = value; }

    @Column(nullable = false)
    private long revision;
    public long getRevision() { return revision; }
    public void setRevision(long value) { revision = value; }

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String policy;
    public String getPolicy() { return policy; }
    public void setPolicy(String value) { policy = value; }

    @Column(columnDefinition = "LONGTEXT")
    private String legacyPolicy;
    public String getLegacyPolicy() { return legacyPolicy; }
    public void setLegacyPolicy(String value) { legacyPolicy = value; }

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

    public MemoryPolicyInventory toInventory() {
        MemoryPolicyInventory result = new MemoryPolicyInventory();
        result.setScope(scope);
        result.setResourceUuid(resourceUuid);
        result.setRevision(revision);
        result.setPolicy(policy);
        return result;
    }
}
