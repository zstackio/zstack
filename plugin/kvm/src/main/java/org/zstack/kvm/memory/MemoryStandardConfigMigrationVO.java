package org.zstack.kvm.memory;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table
public class MemoryStandardConfigMigrationVO {
    public static final String ORDINARY_FIELDS_V1 = "ordinary-fields-v1";

    @Id @Column(length = 64)
    private String uuid;
    public String getUuid() { return uuid; }
    public void setUuid(String value) { uuid = value; }

    @Column(nullable = false, length = 16)
    private String status;
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }

    @Column(length = 2048)
    private String reason;
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }

    @Column(nullable = false)
    private Timestamp createDate;
    public Timestamp getCreateDate() { return createDate; }
    public void setCreateDate(Timestamp value) { createDate = value; }

    @Column(nullable = false)
    private Timestamp lastOpDate;
    public Timestamp getLastOpDate() { return lastOpDate; }
    public void setLastOpDate(Timestamp value) { lastOpDate = value; }

    @PrePersist private void onCreate() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        createDate = now; lastOpDate = now;
    }
    @PreUpdate private void onUpdate() { lastOpDate = new Timestamp(System.currentTimeMillis()); }
}
